#!/usr/bin/env python3
"""
기존 테스트 리뷰(review_no=244) 삭제 후 새로운 리뷰 추가
유저 이름 마스킹 없이 실제 이름 사용
"""

import json
import pymysql
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

def update_review():
    """기존 테스트 리뷰 삭제 후 새 리뷰 추가"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            store_no = 44  # 폰신사우만점

            # 1. 기존 테스트 리뷰 삭제 (review_no=244)
            print("1. 기존 테스트 리뷰 삭제 중...")
            cursor.execute("""
                DELETE FROM tb_review_store_phone_virtual
                WHERE review_no = 244 AND store_no = 44
            """)
            deleted = cursor.rowcount
            print(f"   ✓ {deleted}건 삭제됨")

            # 2. 새 리뷰 INSERT (마스킹 없는 실제 이름)
            print("\n2. 새 리뷰 추가 중...")
            test_review = {
                'user_nm': '박지민',
                'store_no': store_no,
                'content': '친절하고 꼼꼼하게 설명해주셔서 좋았습니다. 가격도 합리적이고 매장도 깨끗해요. 추천합니다!',
                'rating': 5.0
            }

            cursor.execute("""
                INSERT INTO tb_review_store_phone_virtual (
                    user_nm,
                    store_no,
                    product_group_code,
                    product_code,
                    apply_no,
                    content,
                    complaint_content,
                    image,
                    view,
                    favorite,
                    complaint,
                    rating,
                    note,
                    clip_yn,
                    created_at,
                    modified_at,
                    deleted_yn,
                    deleted_at
                ) VALUES (
                    %(user_nm)s,
                    %(store_no)s,
                    NULL,
                    NULL,
                    0,
                    %(content)s,
                    NULL,
                    0,
                    0,
                    0,
                    0,
                    %(rating)s,
                    NULL,
                    'N',
                    NOW(),
                    NOW(),
                    'N',
                    NULL
                )
            """, test_review)

            new_review_no = cursor.lastrowid
            connection.commit()

            print(f"   ✓ 새 리뷰 추가 완료! (review_no: {new_review_no})")

            # 3. 결과 확인
            cursor.execute("""
                SELECT review_no, user_nm, store_no, content, rating, created_at
                FROM tb_review_store_phone_virtual
                WHERE review_no = %s
            """, (new_review_no,))
            new_review = cursor.fetchone()

            print("\n3. 추가된 리뷰 확인")
            print("=" * 80)
            print(f"  review_no: {new_review['review_no']}")
            print(f"  user_nm: {new_review['user_nm']}")
            print(f"  store_no: {new_review['store_no']}")
            print(f"  rating: {new_review['rating']}")
            print(f"  content: {new_review['content']}")
            print(f"  created_at: {new_review['created_at']}")
            print("=" * 80)

            # 전체 리뷰 개수
            cursor.execute("""
                SELECT COUNT(*) as cnt
                FROM tb_review_store_phone_virtual
                WHERE store_no = %s
            """, (store_no,))
            total_count = cursor.fetchone()['cnt']

            print(f"\n폰신사우만점 총 리뷰 개수: {total_count}건")

            return new_review_no

    except Exception as e:
        connection.rollback()
        print(f"\n❌ 오류 발생: {e}")
        raise
    finally:
        connection.close()

if __name__ == "__main__":
    print("=" * 80)
    print("폰신사우만점 리뷰 업데이트 (마스킹 제거)")
    print("=" * 80)
    print()

    review_no = update_review()

    print(f"\n✓ 완료! 새로운 리뷰 번호: {review_no}")
