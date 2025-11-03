import pymysql
import json
from datetime import datetime

# DB 연결 정보
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

def get_review_data():
    """리뷰 데이터 수집"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            data = {}

            # 1. 전체 리뷰 통계
            print("📊 리뷰 통계 수집 중...")
            cursor.execute("""
                SELECT COUNT(*) as count
                FROM tb_review_store_phone_virtual
                WHERE deleted_yn = 'N'
            """)
            data['total_reviews'] = cursor.fetchone()['count']

            # 2. 판매점별 리뷰 통계 (리뷰가 있는 판매점만)
            print("🏪 판매점별 리뷰 통계 수집 중...")
            # 먼저 리뷰 테이블 컬럼 확인
            cursor.execute("DESCRIBE tb_review_store_phone_virtual")
            review_columns = [row['Field'] for row in cursor.fetchall()]
            print(f"  tb_review 컬럼: {review_columns}")

            cursor.execute("""
                SELECT
                    r.store_no,
                    s.store_nm,
                    s.address,
                    s.address_detail,
                    COUNT(r.store_no) as review_count,
                    ROUND(AVG(r.rating), 1) as avg_rating
                FROM tb_review_store_phone_virtual r
                JOIN tb_store s ON r.store_no = s.store_no
                WHERE r.deleted_yn = 'N'
                GROUP BY r.store_no, s.store_nm, s.address, s.address_detail
                HAVING review_count > 0
                ORDER BY review_count DESC, avg_rating DESC
            """)
            data['stores_with_reviews'] = list(cursor.fetchall())

            # 3. 실제 리뷰 내용 (최근 50개)
            print("💬 실제 리뷰 내용 수집 중...")
            cursor.execute("""
                SELECT
                    r.review_no,
                    r.store_no,
                    s.store_nm,
                    s.address,
                    r.rating,
                    r.content as review_content,
                    r.created_at
                FROM tb_review_store_phone_virtual r
                JOIN tb_store s ON r.store_no = s.store_no
                WHERE r.deleted_yn = 'N'
                ORDER BY r.created_at DESC
                LIMIT 50
            """)
            reviews = list(cursor.fetchall())

            # datetime을 문자열로 변환
            for review in reviews:
                if 'created_at' in review and isinstance(review['created_at'], datetime):
                    review['created_at'] = review['created_at'].strftime('%Y-%m-%d %H:%M:%S')

            data['recent_reviews'] = reviews

            # 4. 평점별 분포
            print("⭐ 평점별 분포 수집 중...")
            cursor.execute("""
                SELECT
                    rating,
                    COUNT(*) as count
                FROM tb_review_store_phone_virtual
                WHERE deleted_yn = 'N'
                GROUP BY rating
                ORDER BY rating DESC
            """)
            data['rating_distribution'] = list(cursor.fetchall())

            # 5. 지역별 리뷰 통계 - 일단 스킵
            data['reviews_by_city'] = []

            return data

    finally:
        connection.close()

if __name__ == '__main__':
    print("🚀 리뷰 데이터 수집 시작...")
    data = get_review_data()

    # JSON 저장
    output = {
        'metadata': {
            'generated_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'database': 'db_nofee'
        },
        'data': data
    }

    output_file = '../1-raw-data/database/db_review_data_latest.json'
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2, default=str)

    print(f"\n✅ 리뷰 데이터 수집 완료!")
    print(f"📁 저장 위치: {output_file}")
    print(f"\n📊 요약:")
    print(f"  - 전체 리뷰 수: {data['total_reviews']}개")
    print(f"  - 리뷰가 있는 판매점: {len(data['stores_with_reviews'])}곳")
    if data['stores_with_reviews']:
        top_store = data['stores_with_reviews'][0]
        print(f"  - TOP 판매점: {top_store['store_nm']} ({top_store['review_count']}개 리뷰, ⭐{top_store['avg_rating']}점)")
