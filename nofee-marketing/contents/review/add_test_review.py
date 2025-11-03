#!/usr/bin/env python3
"""
폰신사 우만점 테스트 리뷰 1개 추가
기존 데이터는 절대 수정하지 않음 (INSERT ONLY)
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

def find_phoneshinsa_store():
    """폰신사 우만점 정보 찾기"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            # 전체 판매점 조회 (nickname으로 검색)
            cursor.execute("""
                SELECT store_no, nickname, sido_no, sigungu_no, review, review_avg
                FROM tb_store
                WHERE deleted_yn = 'N'
                ORDER BY store_no
            """)
            stores = cursor.fetchall()

            print("전체 판매점 목록:")
            print("-" * 80)
            for store in stores:
                print(f"  [#{store['store_no']:>3}] {store['nickname']:<30} - 리뷰: {store['review']}건 (평균: {store['review_avg']})")

            print("\n'폰신사' 또는 '우만'이 포함된 판매점 찾기...")
            phoneshinsa = None
            for store in stores:
                nickname = store['nickname'] or ''
                if '폰신사' in nickname or '우만' in nickname:
                    phoneshinsa = store
                    print(f"\n✓ 발견: [{store['store_no']}] {store['nickname']}")
                    break

            return phoneshinsa

    finally:
        connection.close()

def add_test_review(store_no):
    """테스트 리뷰 1개 추가 (INSERT ONLY)"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            # 현재 리뷰 개수 확인
            cursor.execute("""
                SELECT COUNT(*) as cnt
                FROM tb_review_store_phone_virtual
                WHERE store_no = %s
            """, (store_no,))
            current_count = cursor.fetchone()['cnt']

            print(f"\n현재 store_no={store_no}의 리뷰 개수: {current_count}건")

            # 테스트 리뷰 데이터
            test_review = {
                'user_nm': '박*민',
                'store_no': store_no,
                'product_group_code': None,  # NULL
                'product_code': None,  # NULL
                'apply_no': 0,
                'content': '친절하고 꼼꼼하게 설명해주셔서 좋았습니다. 가격도 합리적이고 매장도 깨끗해요. 추천합니다!',
                'complaint_content': None,
                'image': 0,
                'view': 0,
                'favorite': 0,
                'complaint': 0,
                'rating': 5.0,
                'note': None,
                'clip_yn': 'N',
                'deleted_yn': 'N'
            }

            print("\n추가할 리뷰 정보:")
            print("-" * 80)
            for key, value in test_review.items():
                print(f"  {key}: {value}")

            # 사용자 확인
            print("\n⚠️  위 내용으로 리뷰를 추가하시겠습니까?")
            print("   기존 데이터는 절대 수정되지 않습니다. (INSERT ONLY)")

            confirm = input("\n계속하려면 'yes'를 입력하세요: ")

            if confirm.lower() != 'yes':
                print("\n❌ 취소되었습니다.")
                return False

            # INSERT 실행
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
                    %(product_group_code)s,
                    %(product_code)s,
                    %(apply_no)s,
                    %(content)s,
                    %(complaint_content)s,
                    %(image)s,
                    %(view)s,
                    %(favorite)s,
                    %(complaint)s,
                    %(rating)s,
                    %(note)s,
                    %(clip_yn)s,
                    NOW(),
                    NOW(),
                    %(deleted_yn)s,
                    NULL
                )
            """, test_review)

            new_review_no = cursor.lastrowid
            connection.commit()

            print(f"\n✓ 리뷰 추가 완료! (review_no: {new_review_no})")

            # 추가된 리뷰 확인
            cursor.execute("""
                SELECT * FROM tb_review_store_phone_virtual
                WHERE review_no = %s
            """, (new_review_no,))
            new_review = cursor.fetchone()

            print("\n추가된 리뷰 확인:")
            print("-" * 80)
            for key, value in new_review.items():
                print(f"  {key}: {value}")

            # 전체 리뷰 개수 재확인
            cursor.execute("""
                SELECT COUNT(*) as cnt
                FROM tb_review_store_phone_virtual
                WHERE store_no = %s
            """, (store_no,))
            new_count = cursor.fetchone()['cnt']

            print(f"\n업데이트된 리뷰 개수: {current_count}건 → {new_count}건 (+{new_count - current_count})")

            return True

    except Exception as e:
        connection.rollback()
        print(f"\n❌ 오류 발생: {e}")
        return False
    finally:
        connection.close()

if __name__ == "__main__":
    print("=" * 80)
    print("폰신사 우만점 테스트 리뷰 추가")
    print("=" * 80)
    print()

    # 1. 폰신사 우만점 찾기
    store = find_phoneshinsa_store()

    if not store:
        print("\n❌ 폰신사 우만점을 찾을 수 없습니다.")
        print("   수동으로 store_no를 확인해주세요.")
    else:
        # 2. 테스트 리뷰 추가
        add_test_review(store['store_no'])
