#!/usr/bin/env python3
"""
상품 데이터 조회 및 HTML/이미지 생성
- 시세표 테이블을 사용하여 실제 판매가 최저가 조회
- 6개 상품을 한 번에 하나의 HTML로 생성
- Puppeteer를 사용하여 각 상품별 이미지 자동 생성 (3x4, 1x1)
- 날짜별 output 폴더 자동 생성
"""
import pymysql
import json
import sys
import os
import subprocess
from datetime import datetime
from pathlib import Path

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

# 6개 상품 목록 (순서대로) - 상품명, 상품코드, 이미지 파일명 매핑
PRODUCT_LIST = [
    {'name': '아이폰 17 프로', 'code': 'AP-P-17', 'image': '아이폰17프로.png'},
    {'name': '아이폰 17', 'code': 'AP-B-17', 'image': '아이폰17.png'},
    {'name': '아이폰 17 프로 맥스', 'code': 'AP-PM-17', 'image': '아이폰 17 프로 맥스.png'},
    {'name': '갤럭시 Z 폴드 7', 'code': 'SM-ZF-7', 'image': '갤럭시 Z 폴드 7.png'},
    {'name': '갤럭시 Z 플립 7', 'code': 'SM-ZP-7', 'image': '갤럭시 Z 플립 7.png'},
    {'name': '갤럭시 S25 울트라', 'code': 'SM-SU-25', 'image': '갤럭시 S25 울트라.png'}
]

def get_product_data(product_name, product_code=None):
    """상품 정보 조회"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            # 상품 그룹 테이블에서 조회
            print(f"🔍 '{product_name}' 상품 정보 조회 중...")

            # 상품 코드가 지정된 경우 정확히 매칭
            if product_code:
                cursor.execute("""
                    SELECT
                        pgp.product_group_code,
                        pgp.product_group_nm as name,
                        pgp.manufacturer_code,
                        cc.nm_ko as manufacturer
                    FROM tb_product_group_phone pgp
                    LEFT JOIN tb_common_code cc ON pgp.manufacturer_code = cc.code
                    WHERE pgp.product_group_code = %s
                    AND pgp.deleted_yn = 'N'
                    LIMIT 1
                """, (product_code,))
            else:
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

                # 출고가 조회 (tb_pricetable_phone 테이블)
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
                else:
                    product['original_price'] = 1550000
                    print(f"   ⚠️ 출고가 정보 없음 - 기본값 사용")

                # 시세표에서 최저가 조회 (tb_pricetable_store_phone_col 테이블)
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
                    product['store_count'] = price_info['store_count']
                else:
                    # 기본값 설정
                    print("   ⚠️ 시세표에 데이터 없음 - 기본값 사용")
                    product['original_price'] = 1550000
                    product['lowest_price'] = 890000
                    product['store_count'] = 0

                # 노피 지원금 계산 (출고가 - 최저가)
                product['nofee_support'] = product['original_price'] - product['lowest_price']

                print(f"   출고가: {product['original_price']:,}원")
                print(f"   최저가: {product['lowest_price']:,}원")
                print(f"   노피지원금: {product['nofee_support']:,}원\n")

                return product
            else:
                print(f"❌ '{product_name}' 상품을 찾을 수 없습니다.\n")
                return None

    finally:
        connection.close()

def format_price(price):
    """가격을 만원 단위로 포맷팅"""
    # 마이너스 가격은 0으로 처리
    formatted = int(price / 10000)
    return max(0, formatted)

def generate_product_html(product, ratio='3x4'):
    """단일 상품 HTML 생성"""
    if not product:
        return ""

    original_price = format_price(product['original_price'])
    lowest_price = format_price(product['lowest_price'])
    nofee_support = format_price(product['nofee_support'])

    # 이미지 파일명 (assets 폴더에서 가져오기)
    image_filename = product.get('image_file', product['name'].replace(' ', '') + '.png')
    # 상대 경로 설정 (output/날짜/ 폴더에서 assets로 접근)
    image_path = f'../../assets/{image_filename}'

    # 상품명 길이 체크
    title_class = 'long' if len(product['name']) > 10 else ''

    # 가격이 큰 경우 compact 클래스 필요
    canvas_class = 'compact' if product['name'] == '아이폰 17 프로 맥스' else ''

    # 1x1 비율인 경우 추가 클래스
    if ratio == '1x1':
        canvas_class += ' square' if canvas_class else 'square'

    return f'''    <!-- {product['name']} -->
    <div class="canvas {canvas_class}">
        <div class="header-banner">
            <div class="banner-line">100% 할부원금만 받아요</div>
            <div class="banner-line">집 근처에서 성지 가격으로</div>
        </div>
        <div class="content">
            <div class="product-title{' ' + title_class if title_class else ''}">
                {product['name']}
            </div>

            <div class="product-info">
                결합없음 ㅣ 추가금없음 ㅣ 즉시개통
            </div>

            <div class="product-image">
                <img src="{image_path}" alt="{product['name']}">
            </div>

            <div class="price-section">
                <div class="price-box original">
                    <div class="price-value">{original_price}만원</div>
                </div>
                <div class="price-box lowest">
                    <div class="price-value"><span class="amount">{lowest_price}</span><span class="unit">만원</span></div>
                </div>
            </div>
        </div>
    </div>
'''

def create_output_directories():
    """output 폴더 구조 생성"""
    today = datetime.now().strftime('%Y%m%d')
    base_dir = Path('output') / today

    # 디렉토리 생성
    (base_dir / '3x4').mkdir(parents=True, exist_ok=True)
    (base_dir / '1x1').mkdir(parents=True, exist_ok=True)

    return base_dir

def generate_multi_product_html(products, output_filename='all_products_3x4.html', ratio='3x4'):
    """여러 상품을 한 HTML로 생성"""
    if not products or len(products) == 0:
        print("❌ 상품 정보가 없어 HTML을 생성할 수 없습니다.")
        return None

    # 각 상품의 HTML 생성
    products_html = '\n'.join([generate_product_html(p, ratio) for p in products if p])

    # 비율에 따른 캔버스 크기 설정
    if ratio == '1x1':
        canvas_width = 1080
        canvas_height = 1080
    else:  # 3x4
        canvas_width = 1080
        canvas_height = 1440

    html_content = f"""<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>노피 최저가 - 아이폰 17 & 갤럭시 시리즈 ({ratio})</title>
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
            flex-wrap: wrap;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            gap: 20px;
        }}

        .canvas {{
            width: {canvas_width}px;
            height: {canvas_height}px;
            background: #fff;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }}

        .header-banner {{
            background: #131FA0;
            width: 100%;
            padding: 35px 40px;
            text-align: center;
        }}

        .banner-line {{
            font-size: 70px;
            font-weight: 700;
            color: #fff;
            letter-spacing: -2.5px;
            line-height: 1.2;
        }}

        .content {{
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 35px 50px;
        }}

        .product-title {{
            font-size: 120px;
            font-weight: 900;
            color: #000;
            text-align: center;
            letter-spacing: -5px;
            margin-bottom: 25px;
            line-height: 1.1;
            word-break: keep-all;
            max-width: 100%;
        }}

        .product-title.long {{
            font-size: 95px;
        }}

        .product-info {{
            font-size: 44px;
            font-weight: 500;
            color: #666;
            text-align: center;
            margin-bottom: 35px;
            letter-spacing: -1.5px;
            white-space: nowrap;
        }}

        .product-image {{
            width: 420px;
            height: 420px;
            margin-bottom: 35px;
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
            gap: 60px;
            background: transparent;
        }}

        .price-box {{
            text-align: center;
            background: transparent;
        }}

        .price-box.original .price-value {{
            font-size: 85px;
            font-weight: 900;
            letter-spacing: -4px;
            color: #999;
            text-decoration: line-through;
            white-space: nowrap;
        }}

        .price-box.lowest .price-value {{
            font-size: 150px;
            font-weight: 900;
            letter-spacing: -6px;
            white-space: nowrap;
        }}

        .canvas.compact .price-box.original .price-value {{
            font-size: 75px;
        }}

        .canvas.compact .price-box.lowest .price-value {{
            font-size: 130px;
        }}

        .price-box.lowest .price-value .amount {{
            color: #131FA0;
        }}

        .price-box.lowest .price-value .unit {{
            color: #000;
        }}

        /* 1x1 비율 전용 스타일 */
        .canvas.square .header-banner {{
            padding: 28px 35px;
        }}

        .canvas.square .banner-line {{
            font-size: 52px;
            font-weight: 700;
            line-height: 1.2;
        }}

        .canvas.square .content {{
            padding: 30px 30px;
        }}

        .canvas.square .product-title {{
            font-size: 105px;
            margin-bottom: 20px;
        }}

        .canvas.square .product-title.long {{
            font-size: 85px;
        }}

        .canvas.square .product-info {{
            font-size: 38px;
            margin-bottom: 30px;
        }}

        .canvas.square .product-image {{
            width: 400px;
            height: 400px;
            margin-bottom: 35px;
        }}

        .canvas.square .price-section {{
            gap: 50px;
        }}

        .canvas.square .price-box.original .price-value {{
            font-size: 70px;
        }}

        .canvas.square .price-box.lowest .price-value {{
            font-size: 120px;
        }}

        .canvas.square.compact .price-box.original .price-value {{
            font-size: 60px;
        }}

        .canvas.square.compact .price-box.lowest .price-value {{
            font-size: 95px;
        }}
    </style>
</head>
<body>
{products_html}
</body>
</html>
"""

    # HTML 파일 저장
    with open(output_filename, 'w', encoding='utf-8') as f:
        f.write(html_content)

    print(f"\n✅ HTML 파일 생성 완료: {output_filename}")
    print(f"   비율: {ratio}")
    print(f"   캔버스 크기: {canvas_width}x{canvas_height}px")
    print(f"   총 {len([p for p in products if p])}개 상품 포함")
    print(f"   생성일시: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    return output_filename

def generate_screenshots(html_file, output_dir, ratio):
    """Node.js 스크립트를 호출하여 스크린샷 생성"""
    try:
        result = subprocess.run(
            ['node', 'screenshot.js', html_file, output_dir, ratio],
            capture_output=True,
            text=True,
            check=True
        )
        print(result.stdout)
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ 스크린샷 생성 실패: {e.stderr}")
        return False
    except FileNotFoundError:
        print("❌ Node.js가 설치되어 있지 않습니다. 'node' 명령어를 찾을 수 없습니다.")
        return False


def generate_content_text(products, output_path):
    """블로그 및 인스타그램 콘텐츠 텍스트 생성"""
    now = datetime.now()
    date_str = now.strftime('%Y년 %m월 %d일')
    month_label = now.strftime('%Y년 %m월')

    valid_products = [p for p in products if p]
    if not valid_products:
        return

    # ===== 블로그 콘텐츠 생성 =====
    blog_lines = [
        f"{month_label} 휴대폰 최저가 총정리 - 아이폰17 & 갤럭시 시리즈",
        "",
        f"안녕하세요, 노피입니다!",
        f"{date_str} 기준 인기 휴대폰 최저가를 정리해드립니다.",
        "",
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "노피는 우리 동네 성지 매장을 찾을 수 있는 플랫폼이에요.",
        "온라인 문의 없이 직접 매장을 찾아서 발품 파는 게 제일 저렴합니다.",
        "",
        "👉 노피 바로가기",
        "https://nofee.team/",
        "",
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "[ 오늘의 최저가 ]",
        "",
    ]

    for idx, product in enumerate(valid_products, 1):
        original_price = format_price(product['original_price'])
        lowest_price = format_price(product['lowest_price'])
        nofee_support = format_price(product['nofee_support'])

        blog_lines.append(f"{idx}. {product['name']}")
        blog_lines.append(f"   https://nofee.team/product/{product['product_group_code']}")
        blog_lines.append("")
        blog_lines.append(f"   출고가: {original_price}만원")
        blog_lines.append(f"   최저가: {lowest_price}만원")
        blog_lines.append(f"   노피지원금: {nofee_support}만원")
        blog_lines.append("")

    blog_lines.extend([
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "[ 이용 방법 ]",
        "",
        "1. 노피 웹사이트에서 원하는 기종을 검색하세요",
        "",
        "2. 내 지역 근처 성지 매장의 최저가를 확인하세요",
        "",
        "3. 마음에 드는 매장에 직접 방문하면 끝!",
        "",
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "온라인 문의는 받지 않아요.",
        "노피는 여러분이 동네 성지를 찾아서 직접 발품 팔 수 있도록 돕는 플랫폼입니다.",
        "",
        "직접 매장 방문이 제일 저렴합니다!",
        "",
        "👉 노피에서 동네 성지 찾기",
        "https://nofee.team/",
        "",
    ])

    # 해시태그 생성
    hashtags = build_product_hashtags(valid_products)
    blog_lines.extend([
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        " ".join(hashtags[:30])
    ])

    # ===== 인스타그램 콘텐츠 생성 =====
    # 최저가 상품 찾기
    best_product = min(valid_products, key=lambda x: x['lowest_price'])
    best_price = format_price(best_product['lowest_price'])

    insta_lines = [
        "100% 할부원금만 받아요",
        "집 근처에서 성지 가격으로",
        "",
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
    ]

    # 간단한 가격 리스트
    for product in valid_products[:4]:
        lowest = format_price(product['lowest_price'])
        insta_lines.append(f"{product['name']} → {lowest}만원")

    insta_lines.extend([
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
        "nofee.team",
        "",
        "━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "휴대폰 바꿀 친구 태그해주세요",
        "",
        " ".join(hashtags[:30])
    ])

    # 파일 저장
    content_lines = ["[BLOG]", ""]
    content_lines.extend(blog_lines)
    content_lines.append("")
    content_lines.append("=" * 80)
    content_lines.append("")
    content_lines.append("[INSTAGRAM]")
    content_lines.append("")
    content_lines.extend(insta_lines)

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(content_lines))

    print(f"📝 블로그/인스타 콘텐츠 저장 완료: {output_path}")


def build_product_hashtags(products):
    """상품 기반 해시태그 생성"""
    hashtags = []

    # 기본 브랜드 태그
    base_tags = ['#노피', '#NOFEE', '#동네성지', '#휴대폰동네성지', '#휴대폰최저가']
    hashtags.extend(base_tags)

    # 상품별 태그
    for product in products:
        name = product['name'].replace(' ', '')
        hashtags.append(f"#{name}")
        hashtags.append(f"#{name}최저가")

        # 시리즈별 태그
        if '아이폰' in product['name']:
            hashtags.append('#아이폰최저가')
            if '17' in product['name']:
                hashtags.append('#아이폰17')
                if '프로' in product['name']:
                    hashtags.append('#아이폰17프로')
                if '프로맥스' in product['name'] or '프로 맥스' in product['name']:
                    hashtags.append('#아이폰17프로맥스')
        elif '갤럭시' in product['name']:
            hashtags.append('#갤럭시최저가')
            if 'S25' in product['name']:
                hashtags.append('#갤럭시S25')
                if '울트라' in product['name']:
                    hashtags.append('#갤럭시S25울트라')
            if '폴드' in product['name']:
                hashtags.append('#갤럭시폴드')
                hashtags.append('#갤럭시Z폴드7')
            if '플립' in product['name']:
                hashtags.append('#갤럭시플립')
                hashtags.append('#갤럭시Z플립7')

    # 일반 키워드
    general_tags = [
        '#휴대폰시세', '#휴대폰성지', '#휴대폰추천',
        '#번호이동', '#기기변경', '#신규가입',
        '#핸드폰최저가', '#스마트폰최저가',
        '#할부원금', '#노피지원금',
        '#폴더블폰', '#아이폰딜', '#갤럭시딜'
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

if __name__ == "__main__":
    print("="*60)
    print("🚀 노피 상품 정보 조회 및 HTML/이미지 생성 시작")
    print("="*60)
    print(f"📋 조회할 상품: {len(PRODUCT_LIST)}개\n")

    # 출력 폴더 생성
    base_dir = create_output_directories()
    print(f"📁 출력 폴더 생성: {base_dir}\n")

    # 모든 상품 데이터 조회
    products = []
    for product_info in PRODUCT_LIST:
        product = get_product_data(product_info['name'], product_info.get('code'))
        if product:
            # 이미지 파일명 추가
            product['image_file'] = product_info['image']
        products.append(product)

    # 성공적으로 조회된 상품 수
    success_count = len([p for p in products if p])
    print("="*60)
    print(f"✅ 조회 완료: {success_count}/{len(PRODUCT_LIST)}개 상품")
    print("="*60)

    if success_count > 0:
        # 3x4 버전 HTML 생성
        print("\n📄 3:4 비율 HTML 생성 중...")
        html_3x4 = str(base_dir / 'all_products_3x4.html')
        generate_multi_product_html(products, html_3x4, ratio='3x4')

        # 1x1 버전 HTML 생성
        print("\n📄 1:1 비율 HTML 생성 중...")
        html_1x1 = str(base_dir / 'all_products_1x1.html')
        generate_multi_product_html(products, html_1x1, ratio='1x1')

        print("\n" + "="*60)
        print("✅ 모든 HTML 파일 생성 완료!")
        print("="*60)

        # 스크린샷 생성
        print("\n" + "="*60)
        print("📸 이미지 스크린샷 생성 시작")
        print("="*60)

        # 3x4 이미지 생성
        output_3x4 = str(base_dir / '3x4')
        if generate_screenshots(html_3x4, output_3x4, '3x4'):
            print(f"✅ 3x4 이미지 저장 위치: {output_3x4}")

        # 1x1 이미지 생성
        output_1x1 = str(base_dir / '1x1')
        if generate_screenshots(html_1x1, output_1x1, '1x1'):
            print(f"✅ 1x1 이미지 저장 위치: {output_1x1}")

        # 블로그/인스타 콘텐츠 텍스트 생성
        print("\n" + "="*60)
        print("📝 블로그/인스타 콘텐츠 생성 시작")
        print("="*60)

        content_path = str(base_dir / 'content.txt')
        generate_content_text(products, content_path)

        print("\n" + "="*60)
        print("✅ 모든 작업 완료!")
        print("="*60)
        print(f"\n📂 결과 파일 위치: {base_dir}")
        print(f"   - HTML: all_products_3x4.html, all_products_1x1.html")
        print(f"   - 3x4 이미지: 3x4/ 폴더")
        print(f"   - 1x1 이미지: 1x1/ 폴더")
        print(f"   - 콘텐츠: content.txt")
    else:
        print("\n❌ 조회된 상품이 없어 HTML을 생성할 수 없습니다.")
