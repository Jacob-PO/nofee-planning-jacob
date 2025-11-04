#!/usr/bin/env python3
"""
상품 데이터 조회 및 HTML 생성
- 시세표 테이블을 사용하여 실제 판매가 최저가 조회
"""
import pymysql
import json
import sys

# DB 연결 정보
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

def get_product_data(product_name='아이폰 17 프로'):
    """상품 정보 조회"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            # 상품 그룹 테이블에서 조회
            print(f"🔍 '{product_name}' 상품 정보 조회 중...")

            cursor.execute("""
                SELECT
                    pgp.product_group_code,
                    pgp.product_group_nm as name,
                    pgp.manufacturer_code,
                    cc.nm_ko as manufacturer
                FROM tb_product_group_phone pgp
                LEFT JOIN tb_common_code cc ON pgp.manufacturer_code = cc.code
                WHERE pgp.product_group_nm LIKE %s
                AND pgp.deleted_yn = 'N'
                LIMIT 1
            """, (f'%{product_name}%',))

            product = cursor.fetchone()

            if product:
                print(f"✅ 상품 찾음: {product['name']}")
                print(f"   제조사: {product['manufacturer']}")

                # 출고가 조회 (tb_pricetable_phone 테이블)
                print("\n📊 출고가 조회 중...")
                cursor.execute("""
                    SELECT DISTINCT retail_price
                    FROM tb_pricetable_phone
                    WHERE product_group_code = %s
                    ORDER BY retail_price
                    LIMIT 1
                """, (product['product_group_code'],))

                retail_price_info = cursor.fetchone()

                if retail_price_info and retail_price_info['retail_price']:
                    product['original_price'] = int(retail_price_info['retail_price'])
                    print(f"   출고가: {product['original_price']:,}원")
                else:
                    product['original_price'] = 1550000
                    print(f"   ⚠️ 출고가 정보 없음 - 기본값 사용: {product['original_price']:,}원")

                # 시세표에서 최저가 조회 (tb_pricetable_store_phone_col 테이블)
                print("\n📊 최저가 조회 중...")
                cursor.execute("""
                    SELECT
                        MIN(LEAST(
                            COALESCE(skt_common_mnp, 999999999),
                            COALESCE(skt_common_chg, 999999999),
                            COALESCE(skt_common_new, 999999999),
                            COALESCE(skt_select_mnp, 999999999),
                            COALESCE(skt_select_chg, 999999999),
                            COALESCE(skt_select_new, 999999999),
                            COALESCE(kt_common_mnp, 999999999),
                            COALESCE(kt_common_chg, 999999999),
                            COALESCE(kt_common_new, 999999999),
                            COALESCE(kt_select_mnp, 999999999),
                            COALESCE(kt_select_chg, 999999999),
                            COALESCE(kt_select_new, 999999999),
                            COALESCE(lg_common_mnp, 999999999),
                            COALESCE(lg_common_chg, 999999999),
                            COALESCE(lg_common_new, 999999999),
                            COALESCE(lg_select_mnp, 999999999),
                            COALESCE(lg_select_chg, 999999999),
                            COALESCE(lg_select_new, 999999999)
                        )) as lowest_price,
                        COUNT(DISTINCT store_no) as store_count
                    FROM tb_pricetable_store_phone_col
                    WHERE product_group_code = %s
                """, (product['product_group_code'],))

                price_info = cursor.fetchone()

                if price_info and price_info['lowest_price'] and price_info['lowest_price'] < 999999999:
                    # 시세표는 만 단위로 저장되어 있으므로 10,000을 곱함
                    product['lowest_price'] = int(price_info['lowest_price']) * 10000
                    product['avg_price'] = int((product['original_price'] + product['lowest_price']) / 2)
                    product['store_count'] = price_info['store_count']
                    print(f"   최저가: {product['lowest_price']:,}원")
                    print(f"   평균가: {product['avg_price']:,}원")
                    print(f"   매장수: {product['store_count']:,}개")
                else:
                    # 기본값 설정
                    print("   ⚠️ 시세표에 데이터 없음 - 기본값 사용")
                    product['original_price'] = 1550000
                    product['lowest_price'] = 890000
                    product['avg_price'] = 1000000
                    product['store_count'] = 0

                # 노피 지원금 계산 (출고가 - 최저가)
                product['nofee_support'] = product['original_price'] - product['lowest_price']
                print(f"\n💰 노피지원금: {product['nofee_support']:,}원")

                return product
            else:
                print(f"❌ '{product_name}' 상품을 찾을 수 없습니다.")
                # 상품 목록 조회
                cursor.execute("""
                    SELECT product_group_nm
                    FROM tb_product_group_phone
                    WHERE product_group_nm LIKE '%아이폰%' OR product_group_nm LIKE '%iPhone%'
                    ORDER BY product_group_nm
                    LIMIT 10
                """)
                products = cursor.fetchall()
                print("\n사용 가능한 아이폰 상품:")
                for p in products:
                    print(f"  - {p['product_group_nm']}")
                return None

    finally:
        connection.close()

def format_price(price):
    """가격을 만원 단위로 포맷팅"""
    return int(price / 10000)

def generate_html(product, output_filename=None):
    """상품 정보로 HTML 생성"""
    if not product:
        print("❌ 상품 정보가 없어 HTML을 생성할 수 없습니다.")
        return

    # 파일명 자동 생성 (지정되지 않은 경우)
    if not output_filename:
        # 상품명을 파일명으로 변환 (공백 제거, 소문자)
        safe_name = product['name'].replace(' ', '_').replace('/', '_')
        output_filename = f"{safe_name}_3x4.html"

    original_price_formatted = format_price(product['original_price'])
    lowest_price_formatted = format_price(product['lowest_price'])
    nofee_support_formatted = format_price(product['nofee_support'])

    html_content = f"""<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{product['name']} - 3:4</title>
    <link href="https://cdn.jsdelivr.net/gh/sun-typeface/SUIT@2/fonts/variable/woff2/SUIT-Variable.css" rel="stylesheet">
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: 'SUIT Variable', -apple-system, sans-serif;
            background: #fff;
            padding: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }}

        .canvas {{
            width: 1080px;
            height: 1440px;
            background: #fff;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }}

        .content {{
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 80px 60px;
        }}

        .tags {{
            display: flex;
            gap: 15px;
            margin-bottom: 40px;
        }}

        .tag {{
            background: #131FA0;
            color: #fff;
            font-size: 42px;
            font-weight: 700;
            padding: 18px 36px;
            border-radius: 12px;
            letter-spacing: -1px;
        }}

        .product-title {{
            font-size: 140px;
            font-weight: 900;
            color: #000;
            text-align: center;
            letter-spacing: -5px;
            margin-bottom: 30px;
            line-height: 1.0;
        }}

        .product-info {{
            font-size: 48px;
            font-weight: 500;
            color: #666;
            text-align: center;
            margin-bottom: 50px;
            letter-spacing: -1.5px;
        }}

        .product-image {{
            width: 500px;
            height: 500px;
            margin-bottom: 60px;
            display: flex;
            align-items: center;
            justify-content: center;
        }}

        .product-image img {{
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
            filter: drop-shadow(0 30px 60px rgba(0, 0, 0, 0.3));
        }}

        .price-section {{
            display: flex;
            justify-content: center;
            align-items: center;
            width: 100%;
            gap: 80px;
            background: transparent;
        }}

        .price-box {{
            text-align: center;
            background: transparent;
        }}

        .price-box.original .price-value {{
            font-size: 100px;
            font-weight: 900;
            letter-spacing: -4px;
            color: #999;
            text-decoration: line-through;
        }}

        .price-box.lowest .price-value {{
            font-size: 180px;
            font-weight: 900;
            letter-spacing: -6px;
        }}

        .price-box.lowest .price-value .amount {{
            color: #131FA0;
        }}

        .price-box.lowest .price-value .unit {{
            color: #000;
        }}
    </style>
</head>
<body>
    <div class="canvas">
        <div class="content">
            <div class="tags">
                <div class="tag">노피 지원금 {nofee_support_formatted}만원</div>
                <div class="tag">누적 5만명 유저</div>
            </div>

            <div class="product-title">
                {product['name']}
            </div>

            <div class="product-info">
                결합없음 ㅣ 추가금없음 ㅣ 즉시개통
            </div>

            <div class="product-image">
                <img src="./{product['name'].replace(' ', '')}.png" alt="{product['name']}">
            </div>

            <div class="price-section">
                <div class="price-box original">
                    <div class="price-value">{original_price_formatted}만원</div>
                </div>
                <div class="price-box lowest">
                    <div class="price-value"><span class="amount">{lowest_price_formatted}</span><span class="unit">만원</span></div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
"""

    # HTML 파일 저장
    with open(output_filename, 'w', encoding='utf-8') as f:
        f.write(html_content)

    print(f"\n✅ HTML 파일 생성 완료: {output_filename}")
    print(f"   기기명: {product['name']}")
    print(f"   출고가: {original_price_formatted}만원")
    print(f"   최저가: {lowest_price_formatted}만원")
    print(f"   노피지원금: {nofee_support_formatted}만원")

if __name__ == "__main__":
    product_name = sys.argv[1] if len(sys.argv) > 1 else '아이폰 17 프로'

    print("="*60)
    print(f"🚀 '{product_name}' 상품 정보 조회 시작")
    print("="*60)

    product = get_product_data(product_name)

    if product:
        generate_html(product)
    else:
        print("\n사용법: python3 fetch_product_data.py '상품명'")
        print("예시: python3 fetch_product_data.py '아이폰 17 프로'")
