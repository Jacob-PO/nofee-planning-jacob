import os
import pandas as pd
import pymysql
import subprocess
import re
from datetime import datetime
from pathlib import Path

class CampaignPriceTossStyle:
    def __init__(self):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)
        self.env_vars = self.load_env_vars()

        # DB 연결 정보 (env 우선)
        self.db_config = {
            'host': self.env_vars.get('DB_HOST', '43.203.125.223'),
            'port': int(self.env_vars.get('DB_PORT', 3306)),
            'user': self.env_vars.get('DB_USER', 'nofee'),
            'password': self.env_vars.get('DB_PASSWORD', 'HBDyNLZBXZ41TkeZ'),
            'database': self.env_vars.get('DB_NAME', 'db_nofee'),
            'charset': self.env_vars.get('DB_CHARSET', 'utf8mb4')
        }

    def load_env_vars(self):
        """프로젝트 루트의 .env 파일을 읽어 dict로 반환"""
        env_data = {}
        try:
            project_root = self.base_path.parents[4]
            env_path = project_root / '.env'
            if env_path.exists():
                with env_path.open('r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if not line or line.startswith('#'):
                            continue
                        if '=' not in line:
                            continue
                        key, value = line.split('=', 1)
                        env_data[key.strip()] = value.strip()
        except Exception:
            pass
        # 환경 변수 우선
        for key in ['DB_HOST', 'DB_PORT', 'DB_USER', 'DB_PASSWORD', 'DB_NAME', 'DB_CHARSET']:
            if key in os.environ:
                env_data[key] = os.environ[key]
        return env_data

    def get_campaign_data(self):
        """DB에서 진행중인 캠페인 데이터 가져오기"""
        connection = pymysql.connect(**self.db_config)

        try:
            with connection.cursor() as cursor:
                query = f"""
                SELECT
                    pg.product_group_nm as device_name,
                    CONCAT(IFNULL(sido.sido_nm, ''), ' ', IFNULL(sigungu.sigungu_nm, '')) as region,
                    CASE
                        WHEN priced.lowest_price_10k >= 999999999 THEN NULL
                        ELSE priced.lowest_price_10k * 10000
                    END AS price,
                    priced.carrier_code,
                    priced.join_type_code,
                    NULL as campaign_title,
                    priced.store_no,
                    COALESCE(s.nickname, CONVERT(s.store_nm USING utf8mb4)) as store_name_raw,
                    NULL as campaign_no,
                    priced.pricetable_dt as start_at,
                    pg.product_group_code
                FROM (
                    SELECT
                        r.pricetable_dt,
                        r.product_group_code,
                        r.product_code,
                        r.rate_plan_code,
                        r.store_no,
                        r.carrier_code,
                        r.join_type_code,
                        LEAST(
                            COALESCE(col.skt_common_mnp, 999999999),
                            COALESCE(col.skt_common_chg, 999999999),
                            COALESCE(col.skt_common_new, 999999999),
                            COALESCE(col.skt_select_mnp, 999999999),
                            COALESCE(col.skt_select_chg, 999999999),
                            COALESCE(col.skt_select_new, 999999999),
                            COALESCE(col.kt_common_mnp, 999999999),
                            COALESCE(col.kt_common_chg, 999999999),
                            COALESCE(col.kt_common_new, 999999999),
                            COALESCE(col.kt_select_mnp, 999999999),
                            COALESCE(col.kt_select_chg, 999999999),
                            COALESCE(col.kt_select_new, 999999999),
                            COALESCE(col.lg_common_mnp, 999999999),
                            COALESCE(col.lg_common_chg, 999999999),
                            COALESCE(col.lg_common_new, 999999999),
                            COALESCE(col.lg_select_mnp, 999999999),
                            COALESCE(col.lg_select_chg, 999999999),
                            COALESCE(col.lg_select_new, 999999999)
                        ) AS lowest_price_10k
                    FROM tb_pricetable_store_phone_row r
                    LEFT JOIN tb_pricetable_store_phone_col col
                        ON col.pricetable_dt = r.pricetable_dt
                        AND col.store_no = r.store_no
                        AND col.product_group_code = r.product_group_code
                        AND col.product_code = r.product_code
                        AND col.rate_plan_code = r.rate_plan_code
                    WHERE r.product_code IS NOT NULL
                ) priced
                LEFT JOIN tb_product_phone p ON priced.product_code = p.product_code
                LEFT JOIN tb_product_group_phone pg ON priced.product_group_code = pg.product_group_code
                LEFT JOIN tb_store s ON priced.store_no = s.store_no
                LEFT JOIN tb_area_sido sido ON s.sido_no = sido.sido_no
                LEFT JOIN tb_area_sigungu sigungu ON s.sigungu_no = sigungu.sigungu_no
                WHERE s.deleted_yn = 'N'
                    AND s.step_code = '0202003'
                ORDER BY pg.product_group_nm, priced.lowest_price_10k ASC, priced.pricetable_dt DESC
                """

                cursor.execute(query)
                results = cursor.fetchall()

                df = pd.DataFrame(results, columns=[
                    'device_name', 'region', 'price', 'carrier_code',
                    'join_type_code', 'campaign_title', 'store_no',
                    'store_name_raw', 'campaign_no', 'start_at', 'product_group_code'
                ])

                store_map = self.fetch_store_names(df['store_no'].dropna().unique())
                df['store_name'] = df['store_no'].map(store_map)
                df['store_name'] = df['store_name'].fillna(df['store_name_raw'])
                df.drop(columns=['store_name_raw'], inplace=True)

                # 통신사 코드 변환
                carrier_map = {
                    '0301001001': 'SKT',
                    '0301001002': 'KT',
                    '0301001003': 'LG'
                }
                df['carrier'] = df['carrier_code'].map(carrier_map)

                # 가입유형 코드 변환
                join_type_map = {
                    '0301007001': '신규',
                    '0301007002': '번호이동',
                    '0301007003': '기기변경'
                }
                df['join_type'] = df['join_type_code'].map(join_type_map)

                print(f"총 {len(df)}개의 캠페인 데이터를 가져왔습니다.")
                return df

        finally:
            connection.close()

    def mask_store_name(self, store_nm):
        """매장 닉네임 출력 - 없으면 공백"""
        if not store_nm:
            return ""
        return str(store_nm).strip()

    def fetch_store_names(self, store_nos):
        """store_no 목록을 받아 매장 닉네임 매핑"""
        if store_nos is None or len(store_nos) == 0:
            return {}

        valid_store_nos = sorted({int(s) for s in store_nos if pd.notna(s)})
        if not valid_store_nos:
            return {}

        placeholders = ','.join(['%s'] * len(valid_store_nos))
        query = f"""
            SELECT store_no, COALESCE(nickname, CONVERT(store_nm USING utf8mb4)) as store_name
            FROM tb_store
            WHERE store_no IN ({placeholders})
        """

        connection = pymysql.connect(**self.db_config)
        try:
            with connection.cursor() as cursor:
                cursor.execute(query, valid_store_nos)
                return {row[0]: row[1] for row in cursor.fetchall()}
        finally:
            connection.close()

    def sort_devices_by_priority(self, devices):
        """기기명을 최신 모델 우선순위로 정렬 - 애플 > 삼성 순서"""
        def get_device_priority(device_name):
            # 아이폰 우선순위 (애플 제품이 가장 먼저, 음수로 최신 모델이 앞에)
            if '아이폰' in device_name:
                # 아이폰 17 시리즈
                if '아이폰 17' in device_name:
                    if '프로 맥스' in device_name:
                        return (0, -17, -4)
                    elif '프로' in device_name:
                        return (0, -17, -3)
                    elif '플러스' in device_name:
                        return (0, -17, -2)
                    else:
                        return (0, -17, -1)
                # 아이폰 16 시리즈
                elif '아이폰 16' in device_name:
                    if '프로 맥스' in device_name:
                        return (0, -16, -4)
                    elif '프로' in device_name:
                        return (0, -16, -3)
                    elif '플러스' in device_name:
                        return (0, -16, -2)
                    else:
                        return (0, -16, -1)
                # 아이폰 15 시리즈
                elif '아이폰 15' in device_name:
                    if '프로 맥스' in device_name:
                        return (0, -15, -4)
                    elif '프로' in device_name:
                        return (0, -15, -3)
                    elif '플러스' in device_name:
                        return (0, -15, -2)
                    else:
                        return (0, -15, -1)
                # 기타 아이폰 (숫자 추출)
                else:
                    import re
                    match = re.search(r'아이폰\s*(\d+)', device_name)
                    if match:
                        return (0, -int(match.group(1)), 0)
                    return (0, 0, 0)

            # 갤럭시 우선순위 (애플 다음으로 표시)
            elif '갤럭시' in device_name:
                # Z 시리즈 (폴더블)
                if 'Z 플립' in device_name or 'Z플립' in device_name:
                    import re
                    match = re.search(r'(\d+)', device_name)
                    num = int(match.group(1)) if match else 0
                    return (1, -num, -2)
                elif 'Z 폴드' in device_name or 'Z폴드' in device_name:
                    import re
                    match = re.search(r'(\d+)', device_name)
                    num = int(match.group(1)) if match else 0
                    return (1, -num, -3)
                # S 시리즈
                elif 'S' in device_name:
                    import re
                    match = re.search(r'S\s*(\d+)', device_name)
                    if match:
                        num = int(match.group(1))
                        if '울트라' in device_name:
                            return (1, -num, -1)
                        else:
                            return (1, -num, 0)
                    return (1, 0, 0)
                # 기타 갤럭시
                else:
                    return (1, 0, 0)

            # 기타 기기
            else:
                return (2, 0, 0)

        return sorted(devices, key=get_device_priority)

    def generate_toss_style_html(self, df):
        """토스 스타일 HTML 생성 - 컴팩트 버전"""
        now = datetime.now()
        weekdays = ['월', '화', '수', '목', '금', '토', '일']
        weekday = weekdays[now.weekday()]
        date = now.strftime(f'%m월 %d일') + f' ({weekday})'

        # 모든 고유 기기명 가져오기
        all_devices = df['device_name'].unique()
        all_devices = [d for d in all_devices if d]
        all_devices = self.sort_devices_by_priority(all_devices)  # 최신 모델 우선순위로 정렬

        # 디버깅: 정렬된 기기 순서 출력
        print(f"\n📱 전체 기기 수: {len(all_devices)}개")
        print(f"\n📱 정렬된 기기 순서:")
        for idx, device in enumerate(all_devices, 1):
            device_count = len(df[df['device_name'] == device])
            print(f"  {idx}. {device} ({device_count}개 캠페인)")

        blog_sections = []

        html = f"""
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>휴대폰 최저가</title>
    <link href="https://cdn.jsdelivr.net/gh/sun-typeface/SUIT@2/fonts/variable/woff2/SUIT-Variable.css" rel="stylesheet">
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: 'SUIT Variable', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            width: 1000px;
            height: 1000px;
            background: #131FA0;
            display: flex;
            justify-content: center;
            align-items: center;
        }}

        .container {{
            width: 1000px;
            height: 1000px;
            background: #131FA0;
            padding: 15px;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }}

        .header {{
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            color: white;
            margin-bottom: 10px;
        }}

        .logo {{
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 15px;
            margin-bottom: 10px;
        }}

        .logo-text {{
            font-size: 32px;
            font-weight: 900;
            color: white;
            margin-bottom: 3px;
        }}

        .date {{
            font-size: 14px;
            background: rgba(255, 255, 255, 0.2);
            padding: 4px 12px;
            border-radius: 20px;
            font-weight: 600;
        }}

        .content {{
            flex: 1;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 8px;
            overflow: hidden;
            grid-auto-rows: minmax(0, 1fr);
            align-content: start;
            padding-bottom: 8px;
        }}

        .device-card {{
            background: rgba(255, 255, 255, 0.95);
            border-radius: 10px;
            padding: 10px;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            min-height: 0;
        }}

        .device-card.single-price-group {{
            grid-column: span 2;
        }}

        .device-card.full-width {{
            grid-column: span 3;
        }}

        .device-name {{
            font-size: 18px;
            font-weight: 800;
            color: #191F28;
            margin-bottom: 6px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.2;
        }}


        .device-tag {{
            font-size: 12px;
            padding: 3px 7px;
            border-radius: 10px;
            font-weight: 600;
            display: inline-block;
        }}

        .device-tag.skt {{
            background: #FFE5E8;
            color: #EA002C;
        }}

        .device-tag.kt {{
            background: #E5F5FF;
            color: #0089D0;
        }}

        .device-tag.lg {{
            background: #FFE5F5;
            color: #E6007E;
        }}

        .device-tag.join-type {{
            background: #F0F2F5;
            color: #191F28;
        }}

        .price-list {{
            display: flex;
            flex-direction: column;
            gap: 5px;
            flex: 1;
            min-height: 0;
            overflow: hidden;
        }}

        .price-list.two-columns {{
            display: grid;
            grid-template-columns: 1fr 1px 1fr;
            gap: 8px;
        }}

        .divider {{
            width: 1px;
            background: #E0E0E0;
            align-self: stretch;
        }}

        .column {{
            display: flex;
            flex-direction: column;
            gap: 5px;
        }}

        .price-item {{
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 7px 9px;
            background: #F7F9FB;
            border-radius: 8px;
            font-size: 14px;
            min-height: 32px;
            gap: 8px;
        }}

        .price-item.compact {{
            padding: 4px 6px;
            min-height: 28px;
            font-size: 12px;
        }}

        .device-name-small {{
            font-size: 13px;
            font-weight: 700;
            color: #191F28;
            margin-right: 6px;
            min-width: 80px;
        }}

        .device-name-inline {{
            font-size: 14px;
            font-weight: 800;
            color: #191F28;
            white-space: nowrap;
            flex-shrink: 0;
        }}

        .price-item-left {{
            display: flex;
            align-items: center;
            gap: 6px;
            flex-shrink: 1;
            min-width: 0;
            overflow: visible;
        }}

        .carrier-price {{
            display: flex;
            align-items: center;
            gap: 4px;
            flex-wrap: nowrap;
        }}

        .price-item-right {{
            display: flex;
            align-items: center;
            gap: 4px;
            flex-shrink: 0;
        }}

        .carrier-dot {{
            width: 10px;
            height: 10px;
            border-radius: 50%;
        }}

        .carrier-dot.skt {{
            background: #EA002C;
        }}

        .carrier-dot.kt {{
            background: #0089D0;
        }}

        .carrier-dot.lg {{
            background: #E6007E;
        }}

        .price {{
            font-weight: 800;
            color: #191F28;
            font-size: 17px;
        }}

        .location-tag {{
            font-size: 13px;
            color: #131FA0;
            font-weight: 700;
            background: #E8EBFF;
            padding: 3px 6px;
            border-radius: 5px;
            white-space: nowrap;
        }}

        .footer {{
            margin-top: 10px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 20px;
            color: white;
            font-size: 17px;
            font-weight: 600;
            height: 45px;
            flex-shrink: 0;
        }}

        .footer-left {{
            flex: 1;
            text-align: left;
        }}

        .footer-center {{
            flex: 1;
            display: flex;
            justify-content: center;
            align-items: center;
        }}

        .footer-right {{
            flex: 1;
            text-align: right;
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 6px;
        }}

        .search-icon {{
            width: 18px;
            height: 18px;
            fill: white;
        }}

        @media print {{
            body {{
                margin: 0;
            }}
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo-text">{now.strftime('%Y년 %m월')} 전국 휴대폰 최저가 시세표</div>
            <div class="date">{date}</div>
        </div>

        <div class="content">
"""

        # 각 기기별로 데이터 정리하고 단가 개수 계산
        device_price_counts = []

        for device in all_devices:
            device_data = df[df['device_name'] == device]
            if device_data.empty:
                continue

            # 상품 코드 가져오기
            product_group_code = device_data['product_group_code'].iloc[0] if 'product_group_code' in device_data.columns else ''

            # 가격별로 그룹화하여 같은 가격의 모든 지역과 가입유형 표시
            # 같은 조건일 때 start_at이 가장 이른 지역만 선택
            price_groups = {}
            seen_combinations = {}  # (carrier, price, join_type): earliest_start_at
            seen_regions_per_key = {}  # 각 가격 조합별로 이미 노출된 (지역, 매장) 추적

            for _, row in device_data.iterrows():
                carrier = row['carrier']
                price = row['price']
                region = row['region'] if pd.notna(row['region']) else ''
                join_type = row['join_type'] if pd.notna(row['join_type']) else ''
                start_at = row['start_at']
                store_nm = row['store_name'] if pd.notna(row['store_name']) else ''

                key = (carrier, price, join_type)

                # 처음 보는 조합이거나 더 이른 start_at인 경우만 추가
                if key not in seen_combinations:
                    seen_combinations[key] = start_at
                    price_groups[key] = []
                    seen_regions_per_key[key] = set()

                    price_groups[key].append({
                        'region': region.strip() if region else '',
                        'store_nm': store_nm
                    })
                    seen_regions_per_key[key].add((region.strip() if region else '', store_nm))
                elif start_at == seen_combinations[key]:
                    # 같은 start_at인 경우만 지역/매장 조합 추가
                    combo = (region.strip() if region else '', store_nm)
                    if combo not in seen_regions_per_key[key]:
                        price_groups[key].append({
                            'region': region.strip() if region else '',
                            'store_nm': store_nm
                        })
                        seen_regions_per_key[key].add(combo)

            # 가격순으로 정렬, 최대 2개까지 표시
            sorted_groups = sorted(
                price_groups.items(),
                key=lambda x: x[0][1]
            )[:2]

            # 모든 가격 그룹 포함
            valid_price_groups = price_groups

            # 데이터가 없으면 건너뛰기
            if len(valid_price_groups) == 0:
                continue

            # 모든 상품을 device_price_counts에 추가 (단일/여러 가격 관계없이)
            device_price_counts.append({
                'device': device,
                'price_count': len(valid_price_groups),
                'sorted_groups': sorted_groups,
                'product_group_code': product_group_code
            })

        # 기기 우선순위 순서 유지 (all_devices 순서대로 이미 정렬되어 있음)
        # device_price_counts.sort(key=lambda x: x['price_count'], reverse=True)  # 이 정렬 제거

        # 상품 카드 21개까지만 표시
        device_price_counts = device_price_counts[:21]

        # 디버깅: 상품 수 출력
        print(f"\n📊 상품 분류 결과:")
        print(f"  - 표시된 상품: {len(device_price_counts)}개")
        total_prices = sum(len(d['sorted_groups']) for d in device_price_counts)
        print(f"  - 총 가격 항목: {total_prices}개\n")

        # HTML 생성
        for device_info in device_price_counts:
            device = device_info['device']
            sorted_groups = device_info['sorted_groups']
            product_group_code = device_info.get('product_group_code', '')

            # 가격 항목이 1개인지 여러개인지 확인
            card_class = "single-price" if len(sorted_groups) == 1 else "multi-price"

            html += f"""
            <div class="device-card {card_class}">
                <div class="device-name">{device}</div>
                <div class="price-list">
"""

            device_entries = []

            for (carrier, price, join_type), regions in sorted_groups:
                # 통신사 클래스
                carrier_class = carrier.lower().replace(' ', '').replace('+', '')

                # 가격 표시 (모두 검정색)
                if price < 0:
                    price_text = f"{int(price/10000)}만"
                    price_class = "price"
                elif price == 0:
                    price_text = "0원"
                    price_class = "price"
                else:
                    price_text = f"{int(price/10000)}만"
                    price_class = "price"

                # 지역 추출 - 실제 DB에서 가져온 지역 사용 (첫 번째 지역 표시)
                region_display = regions[0]['region'] if regions else "전국"
                store_display = regions[0].get('store_nm') if regions else ""

                # 모든 가격 항목 HTML에 추가 (지역 필터 제거)
                html += f"""
                    <div class="price-item">
                        <div class="carrier-price">
                            <div class="carrier-dot {carrier_class}"></div>
                            <span class="{price_class}">{price_text}</span>
                            <span class="device-tag {carrier_class}" style="margin-left: 5px;">{carrier}</span>
                            <span class="device-tag join-type">{join_type}</span>
                        </div>
                        <span class="location-tag">{region_display}</span>
                    </div>
"""
                device_entries.append({
                    'carrier': carrier,
                    'join_type': join_type,
                    'price': price,
                    'region': region_display,
                    'store': self.mask_store_name(store_display)
                })

            html += """
                </div>
            </div>
"""
            if device_entries:
                blog_sections.append({
                    'device': device,
                    'entries': device_entries,
                    'product_group_code': product_group_code
                })

        # 오늘의 특가 섹션 제거 (하드코딩)

        html += """
        </div>

        <div class="footer">
            <div class="footer-left">nofee.team</div>
            <div class="footer-center">
                <svg width="95" height="20" viewBox="0 0 95 20" fill="none" xmlns="http://www.w3.org/2000/svg" style="height: 22px; width: auto;">
                    <path d="M18 5.38456L18 18.7514C18 19.1651 17.664 19.5 17.2489 19.5L8.63149 19.5L4.63676 14.1508L4.63676 19.5L0.751099 19.5C0.337731 19.5 1.26474e-09 19.1651 1.2367e-09 18.7514L0 1.24864C0 0.834871 0.337731 0.5 0.751099 0.5L13.0994 0.5C13.2982 0.5 13.4899 0.578896 13.6306 0.719157L17.7801 4.85508C17.9208 4.99534 18 5.18644 18 5.38456Z" fill="white"/>
                    <path d="M26 1.34198L29.6922 1.34198C32.4316 5.23816 35.171 9.0426 37.9336 13.1667L37.9816 13.1667L37.9816 1.34198L41.5067 1.34198L41.5067 19.1138L37.8393 19.1138C35.0999 15.2635 32.362 11.5715 29.6226 7.53918L29.5515 7.53918L29.5515 19.1138L26.0031 19.1138L26.0031 1.34198L26 1.34198Z" fill="white"/>
                    <path d="M43.8409 12.779C43.8409 8.83692 46.6514 6.03427 50.701 6.03427C54.7505 6.03427 57.5611 8.83692 57.5611 12.779C57.5611 16.721 54.7505 19.5 50.701 19.5C46.6514 19.5 43.8409 16.7432 43.8409 12.779ZM50.6994 16.4473C52.6994 16.4473 54.0807 14.9202 54.0807 12.779C54.0807 10.6378 52.6994 9.08847 50.6994 9.08847C48.6994 9.08847 47.2934 10.6156 47.2934 12.779C47.2934 14.9424 48.6979 16.4473 50.6994 16.4473Z" fill="white"/>
                    <path d="M60.4892 9.29416L58.5124 9.29416L58.5124 6.40125L60.4892 6.40125L60.4892 5.1479C60.4892 1.77554 62.6083 0.5 65.0383 0.5C65.6339 0.5 66.4908 0.591745 67.2302 0.864019L67.2302 3.52905C66.5867 3.30117 66.2062 3.2331 65.753 3.2331C63.8241 3.2331 63.8241 4.55452 63.8241 5.51192L63.8241 6.39977L66.8961 6.39977L66.8961 9.29268L63.8241 9.29268L63.8241 19.1123L60.4892 19.1123L60.4892 9.29416Z" fill="white"/>
                    <path d="M67.4188 12.8248C67.4188 8.90646 70.1815 6.03427 74.0639 6.03427C77.3741 6.03427 80.4708 8.13108 80.4708 12.8012L80.4708 13.6668L70.7538 13.6668C71.0399 15.5801 72.5403 16.6293 74.2789 16.6293C75.637 16.6293 76.8033 15.9234 77.3989 14.602L80.5188 15.8095C79.6371 17.8826 77.3989 19.5 74.3021 19.5C70.3717 19.5 67.4188 16.8572 67.4188 12.8248ZM77.1127 11.4812C76.8745 9.52204 75.5411 8.77033 74.0407 8.77033C72.3253 8.77033 71.2534 9.88606 70.9208 11.4812L77.1127 11.4812Z" fill="white"/>
                    <path d="M81.9001 12.8248C81.9001 8.90646 84.6627 6.03427 88.5452 6.03427C91.8553 6.03427 94.952 8.13108 94.952 12.8012L94.952 13.6668L85.235 13.6668C85.5212 15.5801 87.0216 16.6293 88.7602 16.6293C90.1183 16.6293 91.2846 15.9234 91.8801 14.602L95 15.8095C94.1183 17.8826 91.8801 19.5 88.7834 19.5C84.8529 19.5 81.9001 16.8572 81.9001 12.8248ZM91.5939 11.4812C91.3557 9.52204 90.0224 8.77033 88.522 8.77033C86.8066 8.77033 85.7346 9.88606 85.4021 11.4812L91.5939 11.4812Z" fill="white"/>
                </svg>
            </div>
            <div class="footer-right">
                <svg class="search-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
                </svg>
                <span>네이버에서 "노피 휴대폰" 검색</span>
            </div>
        </div>
    </div>
</body>
</html>
"""
        return html, {
            'generated_at': now,
            'display_date': date,
            'month_label': now.strftime('%Y년 %m월'),
            'sections': blog_sections
        }

    def capture_screenshot(self, html_path, screenshot_path):
        """Node 스크립트를 이용해 HTML을 이미지로 저장"""
        script_path = self.base_path / 'capture_screenshot.js'

        if not script_path.exists():
            print("⚠️ 스크린샷 스크립트를 찾을 수 없어 생략합니다.")
            return

        try:
            result = subprocess.run(
                ['node', str(script_path), str(html_path), str(screenshot_path)],
                capture_output=True,
                text=True,
                check=True
            )
            output = result.stdout.strip()
            if output:
                print(output)
        except FileNotFoundError:
            print("⚠️ Node.js를 찾지 못해 스크린샷 생성을 건너뜁니다.")
        except subprocess.CalledProcessError as err:
            error_msg = err.stderr.strip() or err.stdout.strip()
            print(f"⚠️ 스크린샷 생성 실패: {error_msg}")

    def format_price_text(self, price):
        if price is None:
            return "가격 정보 없음"
        amount = int(round(price / 10000))
        if amount == 0:
            return "0만원"
        sign = "-" if amount < 0 else ""
        return f"{sign}{abs(amount)}만원"

    def clean_hashtag_token(self, text):
        return re.sub(r'[^0-9A-Za-z가-힣]', '', text or '')

    def build_hashtags(self, sections):
        candidates = []
        for section in sections:
            token = self.clean_hashtag_token(section['device'])
            if token:
                candidates.append(f"#{token}")
            for entry in section['entries']:
                region_token = self.clean_hashtag_token(entry.get('region'))
                store_token = self.clean_hashtag_token(entry.get('store'))
                if region_token:
                    candidates.append(f"#{region_token}")
                if region_token and store_token:
                    candidates.append(f"#{region_token}{store_token}")

        carriers = ['#SKT특가', '#KT특가', '#LG유플러스특가']
        base_tags = [
            '#휴대폰시세', '#휴대폰최저가', '#휴대폰성지', '#휴대폰추천', '#번호이동혜택',
            '#기기변경혜택', '#공시지원금', '#추가지원금', '#스마트폰딜', '#핸드폰할인',
            '#아이폰딜', '#갤럭시딜', '#아이폰17', '#아이폰17프로맥스', '#아이폰17프로',
            '#아이폰16프로', '#아이폰16', '#갤럭시S25', '#갤럭시S25울트라', '#갤럭시S24',
            '#갤럭시Z폴드7', '#갤럭시Z플립7', '#폴더블폰', '#서울휴대폰', '#경기휴대폰',
            '#부산휴대폰', '#대구휴대폰', '#대전휴대폰', '#울산휴대폰', '#노피'
        ]
        filler = ['#전국휴대폰시세', '#스마트폰가격', '#통신사비교', '#휴대폰정보', '#휴대폰상담']

        ordered = candidates + carriers + base_tags + filler
        hashtags = []
        for tag in ordered:
            if tag not in hashtags:
                hashtags.append(tag)
            if len(hashtags) == 30:
                break

        while len(hashtags) < 30:
            hashtags.append(f"#노피특가{len(hashtags)+1:02d}")

        return hashtags[:30]

    def generate_blog_post(self, blog_data, path):
        sections = blog_data.get('sections', [])
        generated_at = blog_data.get('generated_at', datetime.now())
        date_str = generated_at.strftime('%Y년 %m월 %d일')
        month_label = blog_data.get('month_label', generated_at.strftime('%Y년 %m월'))

        if not sections:
            with open(path, 'w', encoding='utf-8') as f:
                f.write("데이터가 없어 블로그 요약을 생성하지 못했습니다.")
            return

        # 지역별, 매장별 데이터 수집 + 상품 코드 매핑
        region_stores = {}  # {region: {store: [entries]}}
        all_stores = []
        device_product_codes = {}  # {device_name: product_group_code}

        for section in sections:
            device = section['device']
            product_code = section.get('product_group_code', '')
            if device and product_code:
                device_product_codes[device] = product_code

            for entry in section['entries']:
                region = entry['region']
                store = entry['store'] or '제휴매장'

                if region not in region_stores:
                    region_stores[region] = {}
                if store not in region_stores[region]:
                    region_stores[region][store] = []

                entry_with_device = entry.copy()
                entry_with_device['device'] = device
                entry_with_device['product_group_code'] = product_code
                region_stores[region][store].append(entry_with_device)
                all_stores.append((region, store, entry_with_device))

        # ===== 블로그 콘텐츠 생성 (SEO 최적화) =====
        title = f"{month_label} 전국 휴대폰 최저가 시세표 - 우리 동네 성지 총정리"

        blog_lines = [
            title,
            "",
            f"안녕하세요, 노피입니다!",
            f"{date_str} 기준 전국 휴대폰 최저가 시세를 정리해드립니다.",
            "",
            "오늘은 요즘 핫한 동네 성지 매장들을 소개해드리려고 해요.",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "노피는 우리 동네 성지 매장을 찾을 수 있는 플랫폼이에요.",
            "온라인 문의 없이 직접 매장을 찾아서 발품 파는 게 제일 저렴합니다.",
            "",
            "[ 이용 방법 ]",
            "",
            "1. 노피 웹사이트에서 내 지역을 검색해보세요",
            "   (예: 강남구, 수원시, 부산 해운대구 등)",
            "",
            "2. 우리 동네 성지 매장들의 최저가를 비교하세요",
            "",
            "3. 마음에 드는 매장에 직접 방문하면 끝!",
            "",
            "👉 노피 바로가기",
            "https://nofee.team/",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
        ]

        # 지역별 성지 매장 소개 (SEO 핵심)
        blog_lines.append("[ 지역별 동네 성지 매장 정보 ]")
        blog_lines.append("")
        blog_lines.append("요즘 핫한 동네 성지 매장들을 정리해봤어요!")
        blog_lines.append("")

        top_regions = sorted(region_stores.items(), key=lambda x: len(x[1]), reverse=True)[:10]

        for idx, (region, stores) in enumerate(top_regions, 1):
            sido = region.split()[0] if region else '전국'
            sigungu = ' '.join(region.split()[1:]) if len(region.split()) > 1 else ''

            blog_lines.append(f"{idx}. {region} 동네 성지")
            blog_lines.append("")

            store_list = sorted(stores.items(), key=lambda x: len(x[1]), reverse=True)[:3]

            for store_name, entries in store_list:
                devices = list(set(e['device'] for e in entries))[:3]

                min_price = min(e['price'] for e in entries if e['price'] is not None)
                price_str = self.format_price_text(min_price)

                blog_lines.append(f"▶ {store_name}")
                blog_lines.append(f"   최저가: {price_str}부터")
                blog_lines.append(f"   취급 기종:")
                for dev in devices:
                    product_code = device_product_codes.get(dev, '')
                    if product_code:
                        blog_lines.append(f"   - {dev}")
                        blog_lines.append(f"     https://nofee.team/product/{product_code}")
                    else:
                        blog_lines.append(f"   - {dev}")
                blog_lines.append("")

            blog_lines.append(f"👉 {region} 더 많은 동네 성지 찾기")
            blog_lines.append("https://nofee.team/")
            blog_lines.append("")
            blog_lines.append("━━━━━━━━━━━━━━━━━━━━━━━━")
            blog_lines.append("")

        # 인기 기종별 지역 성지
        blog_lines.append("")
        blog_lines.append("[ 인기 기종별 동네 성지 ]")
        blog_lines.append("")
        blog_lines.append("요즘 가장 많이 찾는 기종들의 동네 성지를 알려드릴게요!")
        blog_lines.append("")

        for idx, section in enumerate(sections[:5], 1):
            device = section['device']
            product_code = section.get('product_group_code', '')
            entries = section['entries']

            if not entries:
                continue

            # 지역별로 그룹화
            device_regions = {}
            for entry in entries:
                region = entry['region']
                store = entry['store'] or '제휴매장'
                key = f"{region} {store}"

                if key not in device_regions:
                    device_regions[key] = entry

            top_locations = sorted(device_regions.items(), key=lambda x: x[1]['price'])[:3]

            blog_lines.append(f"{idx}. {device} 동네 성지")
            if product_code:
                blog_lines.append(f"   https://nofee.team/product/{product_code}")
            blog_lines.append("")

            for location, entry in top_locations:
                price_str = self.format_price_text(entry['price'])
                blog_lines.append(f"   ▶ {location}")
                blog_lines.append(f"      {price_str} / {entry['carrier']} {entry['join_type']}")
            blog_lines.append("")

        # 마무리 섹션
        blog_lines.append("━━━━━━━━━━━━━━━━━━━━━━━━")
        blog_lines.append("")
        blog_lines.append("온라인 문의는 받지 않아요.")
        blog_lines.append("노피는 여러분이 동네 성지를 찾아서 직접 발품 팔 수 있도록 돕는 플랫폼입니다.")
        blog_lines.append("")
        blog_lines.append("직접 매장 방문이 제일 저렴합니다!")
        blog_lines.append("")
        blog_lines.append("👉 노피에서 동네 성지 찾기")
        blog_lines.append("https://nofee.team/")
        blog_lines.append("")

        # 해시태그 (지역 + 매장명 중심)
        hashtags = self.build_seo_hashtags(region_stores, sections)

        blog_lines.append("━━━━━━━━━━━━━━━━━━━━━━━━")
        blog_lines.append("")
        blog_lines.append(" ".join(hashtags[:30]))

        # ===== 인스타그램 콘텐츠 생성 (바이럴 전략 - 감성 중심) =====

        # 지역 샘플 추출 (구체적 숫자 노출 없이)
        sample_regions = list(region_stores.keys())[:3]

        insta_lines = [
            "우리 동네에서",
            "휴대폰 제일 싸게 파는 곳 어디지?",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "노피에서 검색하면",
            "동네 성지 매장 바로 찾을 수 있어요",
            "",
            "온라인 문의 말고",
            "직접 매장 방문하세요",
            "그게 제일 저렴합니다",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
        ]

        # 지역 예시 (심플하게)
        if sample_regions:
            insta_lines.append("예시 지역")
            insta_lines.append("")
            for region in sample_regions:
                insta_lines.append(f"   {region}")
            insta_lines.append("")
            insta_lines.append("내 지역도 검색해보세요")

        insta_lines.append("")
        insta_lines.append("━━━━━━━━━━━━━━━━━━━━━━━━")
        insta_lines.append("")
        insta_lines.append("nofee.team")
        insta_lines.append("")
        insta_lines.append("━━━━━━━━━━━━━━━━━━━━━━━━")
        insta_lines.append("")
        insta_lines.append("휴대폰 바꿀 친구 태그해주세요")
        insta_lines.append("")
        insta_lines.append(" ".join(hashtags[:30]))

        # 파일 저장
        content_lines = ["[BLOG]", ""]
        content_lines.extend(blog_lines)
        content_lines.append("")
        content_lines.append("=" * 80)
        content_lines.append("")
        content_lines.append("[INSTAGRAM]")
        content_lines.append("")
        content_lines.extend(insta_lines)

        with open(path, 'w', encoding='utf-8') as f:
            f.write("\n".join(content_lines))

        print(f"📝 블로그 요약 저장 완료: {path}")

    def build_seo_hashtags(self, region_stores, sections):
        """SEO 최적화된 해시태그 생성 (지역 + 매장명 + 동네성지 중심)"""
        hashtags = []

        # 기본 브랜드 태그 + 동네성지 키워드 강화
        base_tags = ['#노피', '#NOFEE', '#동네성지', '#휴대폰동네성지']
        hashtags.extend(base_tags)

        # 지역 태그 (시도 + 시군구 + 동네성지)
        for region in sorted(region_stores.keys(), key=lambda x: len(region_stores[x]), reverse=True)[:10]:
            parts = region.split()
            if len(parts) >= 1:
                sido = parts[0]
                hashtags.append(f"#{sido}동네성지")
                hashtags.append(f"#{sido}휴대폰")
                if len(parts) >= 2:
                    sigungu = parts[1]
                    hashtags.append(f"#{sido}{sigungu}동네성지")
                    hashtags.append(f"#{sigungu}동네성지")
                    hashtags.append(f"#{sigungu}휴대폰")

        # 매장명 태그
        all_stores = []
        for region, stores in region_stores.items():
            for store_name in stores.keys():
                if store_name and store_name != '제휴매장':
                    clean_name = store_name.replace(' ', '')
                    all_stores.append((region.split()[0] if region else '', clean_name))

        for sido, store_name in all_stores[:10]:
            if store_name:
                hashtags.append(f"#{store_name}")
                if sido:
                    hashtags.append(f"#{sido}{store_name}")

        # 기종 태그
        for section in sections[:8]:
            device = section['device'].replace(' ', '')
            hashtags.append(f"#{device}")
            hashtags.append(f"#{device}최저가")

        # 일반 키워드 (동네성지 강화)
        general_tags = [
            '#휴대폰최저가', '#휴대폰시세', '#휴대폰매장',
            '#아이폰동네성지', '#갤럭시동네성지',
            '#아이폰17', '#갤럭시S25',
            '#번호이동', '#기기변경',
            '#핸드폰싸게사는법', '#휴대폰싸게사는곳'
        ]
        hashtags.extend(general_tags)

        # 중복 제거
        seen = set()
        unique_tags = []
        for tag in hashtags:
            if tag not in seen:
                seen.add(tag)
                unique_tags.append(tag)

        return unique_tags

    def generate(self, output_filename='campaign_price_toss.html'):
        """토스 스타일 단가표 생성"""
        try:
            print("캠페인 데이터 로드 중...")
            df = self.get_campaign_data()

            print("토스 스타일 HTML 생성 중...")
            html, blog_data = self.generate_toss_style_html(df)

            now = datetime.now()
            date_folder = now.strftime('%Y%m%d')
            timestamp = now.strftime('%H%M%S')
            target_dir = self.output_path / date_folder
            target_dir.mkdir(parents=True, exist_ok=True)

            base_name = f"campaign_price_toss_{timestamp}"

            output_file = target_dir / f"{base_name}.html"
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(html)

            print(f"\n✅ 토스 스타일 단가표가 생성되었습니다!")
            print(f"📍 파일 위치: {output_file}")

            screenshot_file = target_dir / f"{base_name}.png"
            self.capture_screenshot(output_file, screenshot_file)

            blog_file = target_dir / f"{base_name}.txt"
            self.generate_blog_post(blog_data, blog_file)

            return output_file

        except Exception as e:
            print(f"오류 발생: {str(e)}")
            import traceback
            traceback.print_exc()
            raise

if __name__ == "__main__":
    generator = CampaignPriceTossStyle()
    generator.generate()
