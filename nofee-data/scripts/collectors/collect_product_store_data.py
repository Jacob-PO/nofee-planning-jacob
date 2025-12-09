#!/usr/bin/env python3
"""
노피 회사소개서용 상품/가격/매장 데이터 수집 스크립트
수집일: 2025-11-19
campaign-price의 generate.py 로직을 활용
"""

import pymysql
import json
from datetime import datetime, date
from pathlib import Path
from collections import defaultdict

import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv(Path(__file__).parents[4] / '.env')

# DB 연결 정보
DB_CONFIG = {
    'host': os.getenv('DB_HOST'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER'),
    'password': os.getenv('DB_PASSWORD'),
    'database': os.getenv('DB_NAME'),
    'charset': 'utf8mb4'
}

def collect_product_data():
    """판매중인 상품 데이터 수집"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            data = {
                'metadata': {
                    'collected_at': datetime.now().isoformat(),
                    'purpose': '회사소개서 - 상품/가격/매장 정보',
                    'version': '1.0'
                }
            }

            print("=" * 60)
            print("🎯 상품/가격/매장 데이터 수집")
            print("=" * 60)

            # 1. 활성화된 상품 그룹 수집
            print("\n📱 1. 활성화된 상품 그룹 수집 중...")
            cursor.execute("""
                SELECT
                    product_group_code,
                    product_group_nm,
                    state_code,
                    created_at
                FROM tb_product_group_phone
                WHERE deleted_yn = 'N' AND state_code = '0204002'
                ORDER BY created_at DESC
            """)
            active_products = list(cursor.fetchall())
            data['active_products'] = {
                'count': len(active_products),
                'products': active_products
            }
            print(f"   ✓ 활성화된 상품: {len(active_products)}개")

            # 2. 가격표가 있는 매장 수집
            print("\n🏪 2. 가격표 노출 매장 수집 중...")
            cursor.execute("""
                SELECT
                    s.store_no,
                    COALESCE(s.nickname, CONVERT(s.store_nm USING utf8mb4)) as store_name,
                    CONCAT(IFNULL(sido.sido_nm, ''), ' ', IFNULL(sigungu.sigungu_nm, '')) as region,
                    s.pricetable_exposure_yn,
                    s.step_code,
                    s.created_at
                FROM tb_store s
                LEFT JOIN tb_area_sido sido ON s.sido_no = sido.sido_no
                LEFT JOIN tb_area_sigungu sigungu ON s.sigungu_no = sigungu.sigungu_no
                WHERE s.deleted_yn = 'N'
                    AND s.pricetable_exposure_yn = 'Y'
                    AND s.step_code = '0202003'
                ORDER BY s.created_at DESC
            """)
            active_stores = list(cursor.fetchall())
            data['active_stores'] = {
                'count': len(active_stores),
                'stores': active_stores
            }
            print(f"   ✓ 가격표 노출 매장: {len(active_stores)}개")

            # 3. 현재 판매중인 가격표 데이터 수집 (generate.py와 동일한 로직)
            print("\n💰 3. 판매중인 가격표 데이터 수집 중...")
            cursor.execute("""
                SELECT
                    pg.product_group_nm as device_name,
                    pg.product_group_code,
                    CONCAT(IFNULL(sido.sido_nm, ''), ' ', IFNULL(sigungu.sigungu_nm, '')) as region,
                    pr.installment_principal as price,
                    pr.carrier_code,
                    pr.join_type_code,
                    pr.store_no,
                    COALESCE(s.nickname, CONVERT(s.store_nm USING utf8mb4)) as store_name,
                    pr.pricetable_dt as start_at,
                    pr.created_at
                FROM tb_pricetable_store_phone_row pr
                INNER JOIN tb_product_phone p ON p.product_code = pr.product_code AND p.deleted_yn = 'N'
                INNER JOIN tb_product_group_phone pg ON pg.product_group_code = pr.product_group_code AND pg.deleted_yn = 'N' AND pg.state_code = '0204002'
                LEFT JOIN tb_store s ON s.store_no = pr.store_no AND s.deleted_yn = 'N' AND s.pricetable_exposure_yn = 'Y' AND s.step_code = '0202003'
                LEFT JOIN tb_area_sido sido ON s.sido_no = sido.sido_no
                LEFT JOIN tb_area_sigungu sigungu ON s.sigungu_no = sigungu.sigungu_no
                WHERE pr.product_code IS NOT NULL
                    AND s.store_no IS NOT NULL
                ORDER BY pg.product_group_nm, pr.installment_principal ASC
            """)
            price_table_data = list(cursor.fetchall())

            # 통신사 코드 변환
            carrier_map = {
                '0301001001': 'SKT',
                '0301001002': 'KT',
                '0301001003': 'LG'
            }

            # 가입유형 코드 변환
            join_type_map = {
                '0301007001': '신규',
                '0301007002': '번호이동',
                '0301007003': '기기변경'
            }

            # 데이터 변환
            for item in price_table_data:
                item['carrier'] = carrier_map.get(item['carrier_code'], item['carrier_code'])
                item['join_type'] = join_type_map.get(item['join_type_code'], item['join_type_code'])

            data['price_table'] = {
                'count': len(price_table_data),
                'data': price_table_data
            }
            print(f"   ✓ 가격표 항목: {len(price_table_data)}개")

            # 4. 통계 계산
            print("\n📊 4. 통계 계산 중...")

            # 상품별 통계
            product_stats = defaultdict(lambda: {
                'price_count': 0,
                'min_price': float('inf'),
                'max_price': float('-inf'),
                'stores': set(),
                'carriers': set()
            })

            # 매장별 통계
            store_stats = defaultdict(lambda: {
                'product_count': 0,
                'products': set(),
                'min_price': float('inf')
            })

            # 지역별 통계
            region_stats = defaultdict(lambda: {
                'store_count': 0,
                'stores': set(),
                'product_count': 0,
                'products': set()
            })

            for item in price_table_data:
                device = item['device_name']
                store_no = item['store_no']
                region = item['region']
                price = item['price']
                carrier = item['carrier']

                # 상품별 통계
                product_stats[device]['price_count'] += 1
                product_stats[device]['min_price'] = min(product_stats[device]['min_price'], price)
                product_stats[device]['max_price'] = max(product_stats[device]['max_price'], price)
                product_stats[device]['stores'].add(store_no)
                product_stats[device]['carriers'].add(carrier)

                # 매장별 통계
                store_stats[store_no]['product_count'] += 1
                store_stats[store_no]['products'].add(device)
                store_stats[store_no]['min_price'] = min(store_stats[store_no]['min_price'], price)

                # 지역별 통계
                region_stats[region]['stores'].add(store_no)
                region_stats[region]['products'].add(device)

            # Set을 list로 변환 (JSON 직렬화 가능하도록)
            for device, stats in product_stats.items():
                stats['stores'] = list(stats['stores'])
                stats['carriers'] = list(stats['carriers'])
                stats['store_count'] = len(stats['stores'])

            for store_no, stats in store_stats.items():
                stats['products'] = list(stats['products'])

            for region, stats in region_stats.items():
                stats['stores'] = list(stats['stores'])
                stats['store_count'] = len(stats['stores'])
                stats['products'] = list(stats['products'])
                stats['product_count'] = len(stats['products'])

            data['statistics'] = {
                'product_stats': dict(product_stats),
                'store_stats': dict(store_stats),
                'region_stats': dict(region_stats)
            }

            print(f"   ✓ 상품별 통계: {len(product_stats)}개 상품")
            print(f"   ✓ 매장별 통계: {len(store_stats)}개 매장")
            print(f"   ✓ 지역별 통계: {len(region_stats)}개 지역")

            # 5. 핵심 요약 지표
            print("\n📈 5. 핵심 요약 지표 계산 중...")

            # 상위 상품 (가격표 항목 수 기준)
            top_products = sorted(
                product_stats.items(),
                key=lambda x: x[1]['price_count'],
                reverse=True
            )[:10]

            # 상위 매장 (상품 수 기준)
            top_stores = sorted(
                store_stats.items(),
                key=lambda x: x[1]['product_count'],
                reverse=True
            )[:10]

            # 상위 지역 (매장 수 기준)
            top_regions = sorted(
                region_stats.items(),
                key=lambda x: x[1]['store_count'],
                reverse=True
            )[:10]

            data['summary'] = {
                'total_active_products': len(active_products),
                'total_active_stores': len(active_stores),
                'total_price_items': len(price_table_data),
                'total_unique_products_with_prices': len(product_stats),
                'total_regions': len(region_stats),
                'top_products': [
                    {
                        'device': device,
                        'price_count': stats['price_count'],
                        'store_count': stats['store_count'],
                        'min_price': stats['min_price'],
                        'max_price': stats['max_price']
                    }
                    for device, stats in top_products
                ],
                'top_stores': [
                    {
                        'store_no': store_no,
                        'product_count': stats['product_count'],
                        'min_price': stats['min_price']
                    }
                    for store_no, stats in top_stores
                ],
                'top_regions': [
                    {
                        'region': region,
                        'store_count': stats['store_count'],
                        'product_count': stats['product_count']
                    }
                    for region, stats in top_regions
                ]
            }

            print(f"   ✓ 총 활성 상품: {len(active_products)}개")
            print(f"   ✓ 총 활성 매장: {len(active_stores)}개")
            print(f"   ✓ 총 가격표 항목: {len(price_table_data)}개")
            print(f"   ✓ 실제 판매중인 상품: {len(product_stats)}개")
            print(f"   ✓ 커버 지역: {len(region_stats)}개")

            print("\n📱 상위 10개 상품:")
            for idx, (device, stats) in enumerate(top_products, 1):
                print(f"   {idx}. {device}: {stats['price_count']}개 가격, {stats['store_count']}개 매장")

            print("\n🏪 상위 10개 매장:")
            for idx, (store_no, stats) in enumerate(top_stores, 1):
                print(f"   {idx}. 매장 #{store_no}: {stats['product_count']}개 상품")

            print("\n🗺️  상위 10개 지역:")
            for idx, (region, stats) in enumerate(top_regions, 1):
                print(f"   {idx}. {region}: {stats['store_count']}개 매장, {stats['product_count']}개 상품")

            return data

    finally:
        connection.close()

def save_data(data, output_dir):
    """데이터를 JSON 파일로 저장"""
    output_path = Path(output_dir) / f"product_store_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    # JSON 직렬화를 위해 datetime 객체 변환
    def json_serial(obj):
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        raise TypeError(f"Type {type(obj)} not serializable")

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2, default=json_serial)

    print(f"\n💾 데이터 저장 완료: {output_path}")
    return output_path

def main():
    """메인 실행 함수"""
    try:
        # 데이터 수집
        data = collect_product_data()

        # 데이터 저장
        script_dir = Path(__file__).parent
        data_dir = script_dir.parent / 'data'
        data_dir.mkdir(exist_ok=True)

        output_path = save_data(data, data_dir)

        print("\n" + "=" * 60)
        print("✅ 상품/가격/매장 데이터 수집 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
