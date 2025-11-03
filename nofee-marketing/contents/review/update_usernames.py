#!/usr/bin/env python3
"""
마스킹된 user_nm을 실제 한국 이름으로 변경
기존 리뷰 내용은 그대로 유지하고 user_nm만 UPDATE
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

# 마스킹된 이름 -> 실제 한국 이름 매핑
NAME_MAPPING = {
    'jlp****': '김민준',
    'yar****': '이서연',
    '5m4****': '박지우',
    'jln****': '최수진',
    '202****': '정하늘',
    'idu****': '강민서',
    'm9****': '윤지호',
    'nom****': '조은별',
    'seo****': '한예진',
    'upl****': '송태양',
    'djm****': '임수아',
    'dlm****': '오건우',
    'acc****': '신유나',
    'kec****': '배준혁',
    'log****': '홍지민',
    'ds5****': '권소윤',
    'ion****': '서민재',
    'jlb****': '노승현',
    'dvh****': '문채원',
    'oyo****': '양지훈',
    '69n****': '안세영',
    'xx_****': '류현우',
    'ff-****': '차은비',
    'wha****': '진도윤',
    'jo-****': '표시우',
    '200****': '구하은',
    'tu8****': '탁재민',
    'ndn****': '황서준',
    'cre****': '석지아',
    'kai****': '변유진',
    'dan****': '남태희',
    'eg****': '도현서',
    '이규황51': '이규황'
}

def update_usernames():
    """마스킹된 user_nm을 실제 이름으로 UPDATE"""
    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            store_no = 44  # 폰신사우만점

            print("=" * 80)
            print(f"폰신사 우만점 리뷰 user_nm 업데이트 (마스킹 제거)")
            print("=" * 80)
            print()

            # 현재 리뷰 조회
            cursor.execute("""
                SELECT review_no, user_nm, content, created_at
                FROM tb_review_store_phone_virtual
                WHERE store_no = %s
                ORDER BY created_at DESC
            """, (store_no,))
            reviews = cursor.fetchall()

            print(f"총 {len(reviews)}건의 리뷰 확인")
            print()

            update_count = 0
            skip_count = 0

            for review in reviews:
                old_name = review['user_nm']

                # 매핑에 있는 경우에만 업데이트
                if old_name in NAME_MAPPING:
                    new_name = NAME_MAPPING[old_name]

                    cursor.execute("""
                        UPDATE tb_review_store_phone_virtual
                        SET user_nm = %s
                        WHERE review_no = %s
                    """, (new_name, review['review_no']))

                    update_count += 1
                    print(f"✓ [#{review['review_no']}] {old_name} → {new_name}")
                else:
                    skip_count += 1
                    print(f"  [#{review['review_no']}] {old_name} (변경 없음)")

            connection.commit()

            print()
            print("=" * 80)
            print(f"✓ 완료!")
            print(f"  업데이트: {update_count}건")
            print(f"  스킵: {skip_count}건")
            print("=" * 80)

            # 결과 확인
            print("\n변경 후 user_nm 목록:")
            cursor.execute("""
                SELECT user_nm, COUNT(*) as cnt
                FROM tb_review_store_phone_virtual
                WHERE store_no = %s
                GROUP BY user_nm
                ORDER BY user_nm
            """, (store_no,))
            result = cursor.fetchall()

            for row in result:
                print(f"  - {row['user_nm']}: {row['cnt']}건")

            return update_count

    except Exception as e:
        connection.rollback()
        print(f"\n❌ 오류 발생: {e}")
        raise
    finally:
        connection.close()

if __name__ == "__main__":
    update_usernames()
