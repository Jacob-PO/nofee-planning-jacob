#!/usr/bin/env python3
"""
tb_review_store_phone_virtual 테이블 상세 분석
폰신사 우만점 리뷰 추가를 위한 데이터 구조 분석
"""

import json
import pymysql
from datetime import datetime
import os

# DB 설정 파일 로드
config_path = os.path.join(os.path.dirname(__file__), '../../../config/db_config.json')
with open(config_path, 'r') as f:
    config = json.load(f)['production']

def connect_db():
    """데이터베이스 연결"""
    return pymysql.connect(
        host=config['host'],
        port=config['port'],
        user=config['user'],
        password=config['password'],
        database=config['database'],
        charset=config['charset'],
        cursorclass=pymysql.cursors.DictCursor
    )

def analyze_review_table():
    """리뷰 테이블 상세 분석"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            print("=" * 80)
            print("tb_review_store_phone_virtual 테이블 분석")
            print("=" * 80)
            print()

            # 1. 테이블 구조 확인
            print("1. 테이블 컬럼 구조")
            print("-" * 80)
            cursor.execute("DESCRIBE tb_review_store_phone_virtual")
            columns = cursor.fetchall()
            for col in columns:
                print(f"  - {col['Field']:<30} {col['Type']:<20} NULL:{col['Null']:<5} Key:{col['Key']:<5} Default:{col['Default']}")
            print()

            # 2. 샘플 데이터 조회 (최근 10건)
            print("2. 최근 샘플 데이터 (10건)")
            print("-" * 80)
            cursor.execute("""
                SELECT * FROM tb_review_store_phone_virtual
                ORDER BY created_at DESC
                LIMIT 10
            """)
            samples = cursor.fetchall()

            if samples:
                print(f"총 {len(samples)}건 조회됨\n")
                for i, row in enumerate(samples, 1):
                    print(f"[샘플 {i}]")
                    for key, value in row.items():
                        if value is not None and len(str(value)) > 100:
                            print(f"  {key}: {str(value)[:100]}...")
                        else:
                            print(f"  {key}: {value}")
                    print()

            # 3. store_no별 리뷰 개수 확인
            print("3. 판매점별 가상 리뷰 개수")
            print("-" * 80)
            cursor.execute("""
                SELECT s.store_no, s.store_nm, COUNT(*) as review_count
                FROM tb_review_store_phone_virtual r
                JOIN tb_store s ON r.store_no = s.store_no
                GROUP BY s.store_no, s.store_nm
                ORDER BY review_count DESC
                LIMIT 20
            """)
            store_reviews = cursor.fetchall()
            for row in store_reviews:
                store_nm = row['store_nm'].decode('utf-8') if isinstance(row['store_nm'], bytes) else row['store_nm']
                print(f"  {store_nm:<30} (store_no: {row['store_no']:>3}) - {row['review_count']:>3}건")
            print()

            # 4. 폰신사 우만점 정보 확인
            print("4. 폰신사 우만점 정보")
            print("-" * 80)
            cursor.execute("""
                SELECT * FROM tb_store
                WHERE store_nm LIKE '%폰신사%' OR store_nm LIKE '%우만%'
            """)
            phoneshinsa = cursor.fetchall()

            if phoneshinsa:
                for store in phoneshinsa:
                    print(f"판매점 발견:")
                    for key, value in store.items():
                        print(f"  {key}: {value}")
                    print()
            else:
                print("  ⚠️ '폰신사' 또는 '우만'이 포함된 판매점을 찾을 수 없습니다.")
                print("  전체 판매점 목록을 확인합니다...\n")

                cursor.execute("""
                    SELECT store_no, store_nm, address_road, sido_code, sigungu_code
                    FROM tb_store
                    WHERE deleted_yn = 'N'
                    ORDER BY store_nm
                """)
                all_stores = cursor.fetchall()
                print(f"  전체 판매점 수: {len(all_stores)}개\n")
                for store in all_stores[:30]:
                    print(f"  [{store['store_no']:>3}] {store['store_nm']:<30} - {store['address_road']}")
            print()

            # 5. product_group_no별 분포 확인
            print("5. 상품 그룹별 리뷰 분포")
            print("-" * 80)
            # product_group_code 컬럼이 NULL이 많으므로 스킵
            cursor.execute("""
                SELECT product_group_code, COUNT(*) as review_count
                FROM tb_review_store_phone_virtual
                WHERE product_group_code IS NOT NULL
                GROUP BY product_group_code
                ORDER BY review_count DESC
                LIMIT 15
            """)
            product_reviews = cursor.fetchall()
            if product_reviews:
                for row in product_reviews:
                    print(f"  {row['product_group_code']:<40} - {row['review_count']:>3}건")
            else:
                print("  ⚠️ product_group_code가 대부분 NULL입니다.")
            print()

            # 6. 평점 분포 확인
            print("6. 평점 분포")
            print("-" * 80)
            cursor.execute("""
                SELECT rating, COUNT(*) as cnt
                FROM tb_review_store_phone_virtual
                GROUP BY rating
                ORDER BY rating DESC
            """)
            star_dist = cursor.fetchall()
            for row in star_dist:
                rating = float(row['rating'])
                bar = '★' * int(rating)
                print(f"  {bar} ({rating}) - {row['cnt']:>3}건")
            print()

            # 7. 이미지 포함 리뷰 확인
            print("7. 이미지 포함 리뷰")
            print("-" * 80)
            cursor.execute("""
                SELECT
                    r.review_no,
                    r.rating,
                    COUNT(i.image_no) as image_count
                FROM tb_review_store_phone_virtual r
                LEFT JOIN tb_review_store_phone_virtual_image i ON r.review_no = i.review_no
                GROUP BY r.review_no, r.rating
                HAVING image_count > 0
                ORDER BY r.created_at DESC
                LIMIT 10
            """)
            image_reviews = cursor.fetchall()
            if image_reviews:
                for row in image_reviews:
                    print(f"  [리뷰 #{row['review_no']}] ★{row['rating']} - 이미지 {row['image_count']}장")
            else:
                print("  이미지 포함 리뷰 없음")
            print()

            # 8. 외래키 관계 분석
            print("8. 참조 테이블 정보")
            print("-" * 80)
            cursor.execute("""
                SELECT
                    CONSTRAINT_NAME,
                    COLUMN_NAME,
                    REFERENCED_TABLE_NAME,
                    REFERENCED_COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = 'db_nofee'
                AND TABLE_NAME = 'tb_review_store_phone_virtual'
                AND REFERENCED_TABLE_NAME IS NOT NULL
            """)
            fk_info = cursor.fetchall()
            if fk_info:
                for fk in fk_info:
                    print(f"  {fk['COLUMN_NAME']} -> {fk['REFERENCED_TABLE_NAME']}.{fk['REFERENCED_COLUMN_NAME']}")
            else:
                print("  외래키 제약조건이 정의되어 있지 않음")
                print("  실제 참조 관계:")
                print("    - store_no -> tb_store.store_no")
                print("    - product_group_no -> tb_product_group_phone.product_group_no")
            print()

    finally:
        connection.close()

def generate_insert_guide():
    """데이터 입력 가이드 생성"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            # 테이블 구조 가져오기
            cursor.execute("DESCRIBE tb_review_store_phone_virtual")
            columns = cursor.fetchall()

            # 샘플 데이터 1건 가져오기
            cursor.execute("""
                SELECT * FROM tb_review_store_phone_virtual
                ORDER BY created_at DESC
                LIMIT 1
            """)
            sample = cursor.fetchone()

            # 가이드 문서 생성
            output_file = os.path.join(os.path.dirname(__file__), 'review_insert_guide.md')

            with open(output_file, 'w', encoding='utf-8') as f:
                f.write("# 폰신사 우만점 가상 리뷰 추가 가이드\n\n")
                f.write("## 1. 테이블 구조\n\n")
                f.write("### tb_review_store_phone_virtual (판매점 휴대폰 가상 리뷰)\n\n")

                f.write("| 컬럼명 | 타입 | NULL | 설명 | 샘플 데이터 |\n")
                f.write("|--------|------|------|------|-------------|\n")

                for col in columns:
                    col_name = col['Field']
                    col_type = col['Type']
                    null_yn = col['Null']
                    sample_value = sample[col_name] if sample and col_name in sample else '-'

                    # 설명 추가
                    description = get_column_description(col_name)

                    f.write(f"| {col_name} | {col_type} | {null_yn} | {description} | {sample_value} |\n")

                f.write("\n## 2. 필수 입력 값 확인\n\n")
                f.write("### 필수 칼럼 (NOT NULL)\n\n")

                required_cols = [col for col in columns if col['Null'] == 'NO' and col['Extra'] != 'auto_increment']
                for col in required_cols:
                    f.write(f"- **{col['Field']}** ({col['Type']})\n")

                f.write("\n### 자동 생성 칼럼\n\n")
                auto_cols = [col for col in columns if col['Extra'] == 'auto_increment' or 'timestamp' in col['Type'].lower()]
                for col in auto_cols:
                    f.write(f"- **{col['Field']}** - {col['Extra'] if col['Extra'] else 'timestamp 자동'}\n")

                f.write("\n## 3. 참조 데이터 조회\n\n")

                # 폰신사 우만점 정보
                f.write("### 3.1 폰신사 우만점 정보\n\n")
                cursor.execute("""
                    SELECT store_no, store_nm, address_road, sido_code, sigungu_code, tel_no
                    FROM tb_store
                    WHERE store_nm LIKE '%폰신사%' OR store_nm LIKE '%우만%'
                """)
                stores = cursor.fetchall()

                if stores:
                    f.write("```sql\n")
                    for store in stores:
                        f.write(f"store_no: {store['store_no']}\n")
                        f.write(f"store_nm: {store['store_nm']}\n")
                        f.write(f"address: {store['address_road']}\n")
                        f.write(f"sido_code: {store['sido_code']}, sigungu_code: {store['sigungu_code']}\n")
                        f.write(f"tel_no: {store['tel_no']}\n")
                        f.write("\n")
                    f.write("```\n\n")
                else:
                    f.write("⚠️ 폰신사 우만점을 찾을 수 없습니다. tb_store 테이블에 먼저 추가해야 합니다.\n\n")

                # 인기 상품 목록
                f.write("### 3.2 인기 상품 목록 (리뷰가 많은 순)\n\n")
                cursor.execute("""
                    SELECT product_group_no, product_group_nm, manufacturer_code
                    FROM tb_product_group_phone
                    WHERE deleted_yn = 'N'
                    ORDER BY product_group_no DESC
                    LIMIT 20
                """)
                products = cursor.fetchall()

                f.write("| product_group_no | 상품명 | 제조사 코드 |\n")
                f.write("|------------------|--------|-------------|\n")
                for p in products:
                    f.write(f"| {p['product_group_no']} | {p['product_group_nm']} | {p['manufacturer_code']} |\n")

                f.write("\n## 4. INSERT 쿼리 템플릿\n\n")
                f.write("```sql\n")
                f.write("INSERT INTO tb_review_store_phone_virtual (\n")
                f.write("    store_no,           -- 판매점 번호 (폰신사 우만점)\n")
                f.write("    product_group_no,   -- 상품 그룹 번호\n")
                f.write("    star,               -- 별점 (1-5)\n")
                f.write("    content,            -- 리뷰 내용\n")
                f.write("    reply,              -- 판매점 답변 (NULL 가능)\n")
                f.write("    reply_at,           -- 답변 일시 (NULL 가능)\n")
                f.write("    user_nm,            -- 작성자 이름 (가상)\n")
                f.write("    created_at,         -- 작성 일시\n")
                f.write("    deleted_yn          -- 삭제 여부 ('N')\n")
                f.write(") VALUES (\n")
                f.write("    ?,                  -- store_no: 폰신사 우만점 번호\n")
                f.write("    ?,                  -- product_group_no: 상품 번호\n")
                f.write("    5,                  -- star: 5점\n")
                f.write("    '친절하고 상세하게 설명해주셔서 좋았습니다. 가격도 저렴하고 추천합니다!',\n")
                f.write("    '감사합니다. 앞으로도 더 좋은 서비스로 보답하겠습니다.',\n")
                f.write("    NOW(),              -- reply_at\n")
                f.write("    '김*수',            -- user_nm: 이름 마스킹\n")
                f.write("    NOW(),              -- created_at\n")
                f.write("    'N'                 -- deleted_yn\n")
                f.write(");\n")
                f.write("```\n\n")

                f.write("## 5. 이미지 추가 (선택사항)\n\n")
                f.write("리뷰에 이미지를 추가하려면 `tb_review_store_phone_virtual_image` 테이블에도 데이터를 추가해야 합니다.\n\n")

                cursor.execute("DESCRIBE tb_review_store_phone_virtual_image")
                image_cols = cursor.fetchall()

                f.write("### tb_review_store_phone_virtual_image 구조\n\n")
                f.write("| 컬럼명 | 타입 | NULL |\n")
                f.write("|--------|------|------|\n")
                for col in image_cols:
                    f.write(f"| {col['Field']} | {col['Type']} | {col['Null']} |\n")

                f.write("\n```sql\n")
                f.write("INSERT INTO tb_review_store_phone_virtual_image (\n")
                f.write("    review_no,          -- 위에서 생성한 리뷰 번호\n")
                f.write("    image_url,          -- 이미지 URL\n")
                f.write("    created_at\n")
                f.write(") VALUES (\n")
                f.write("    LAST_INSERT_ID(),   -- 방금 입력한 리뷰의 auto_increment ID\n")
                f.write("    'https://example.com/review-image.jpg',\n")
                f.write("    NOW()\n")
                f.write(");\n")
                f.write("```\n\n")

                f.write("## 6. 리뷰 작성 팁\n\n")
                f.write("### 리뷰 내용 예시\n\n")
                f.write("```\n")
                reviews = [
                    "친절하고 상세하게 설명해주셔서 좋았습니다. 가격도 저렴하고 추천합니다!",
                    "매장이 깨끗하고 직원분들이 정말 친절하세요. 다음에도 여기서 구매할게요.",
                    "처음 방문했는데 너무 만족스러웠어요. 개통도 빠르고 사은품도 푸짐했습니다.",
                    "가격 비교 여러 곳 했는데 여기가 제일 저렴했어요. 강추!",
                    "점주님이 직접 꼼꼼하게 확인해주셔서 믿고 구매했습니다.",
                ]
                for i, review in enumerate(reviews, 1):
                    f.write(f"{i}. {review}\n")
                f.write("```\n\n")

                f.write("### 사용자 이름 패턴\n\n")
                f.write("```\n")
                names = ["김*수", "이*영", "박*호", "정*민", "최*진", "강*우", "윤*서", "조*희"]
                for name in names:
                    f.write(f"- {name}\n")
                f.write("```\n\n")

                f.write("### 별점 분포 권장\n\n")
                f.write("- ★★★★★ (5점): 70%\n")
                f.write("- ★★★★ (4점): 20%\n")
                f.write("- ★★★ (3점): 10%\n\n")

            print(f"✓ 입력 가이드 생성: {output_file}")

    finally:
        connection.close()

def get_column_description(col_name):
    """컬럼명으로 설명 반환"""
    descriptions = {
        'review_no': '리뷰 번호 (PK, 자동증가)',
        'store_no': '판매점 번호 (FK)',
        'product_group_no': '상품 그룹 번호 (FK)',
        'star': '별점 (1~5)',
        'content': '리뷰 내용',
        'reply': '판매점 답변',
        'reply_at': '답변 일시',
        'user_nm': '작성자 이름 (가상)',
        'created_at': '작성 일시',
        'created_no': '작성자 번호',
        'modified_at': '수정 일시',
        'modified_no': '수정자 번호',
        'deleted_yn': '삭제 여부 (Y/N)',
        'deleted_at': '삭제 일시'
    }
    return descriptions.get(col_name, '-')

if __name__ == "__main__":
    analyze_review_table()
    print()
    generate_insert_guide()
