#!/usr/bin/env python3
"""
노피 실제 리뷰 데이터 조회 스크립트
DB에서 리뷰를 가져와서 콘텐츠 제작에 활용
"""

import pymysql
import json
from datetime import datetime
from decimal import Decimal

# DB 연결 정보
db_config = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

def fetch_reviews():
    """DB에서 리뷰 조회"""
    connection = pymysql.connect(**db_config)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            # 리뷰 조회 쿼리 - 평점 높은 순, 최신순
            query = """
            SELECT
                review_no,
                user_nm,
                store_no,
                content,
                rating,
                view,
                favorite,
                created_at,
                DATE_FORMAT(created_at, '%Y-%m-%d') as review_date
            FROM tb_review_store_phone_virtual
            WHERE deleted_yn = 'N'
                AND content IS NOT NULL
                AND rating >= 4.0
            ORDER BY rating DESC, created_at DESC
            LIMIT 100
            """

            cursor.execute(query)
            reviews = cursor.fetchall()

            print(f"=== 총 {len(reviews)}개의 리뷰 조회 완료 ===\n")

            # 통계 정보
            total_reviews = len(reviews)
            avg_rating = sum([r['rating'] for r in reviews]) / total_reviews if total_reviews > 0 else 0
            five_star = len([r for r in reviews if r['rating'] == 5.0])

            print(f"📊 리뷰 통계:")
            print(f"  - 총 리뷰 수: {total_reviews}개")
            print(f"  - 평균 평점: {avg_rating:.2f}점")
            print(f"  - 5점 리뷰: {five_star}개 ({five_star/total_reviews*100:.1f}%)")
            print()

            # 리뷰 샘플 출력
            print("=" * 80)
            print("📝 리뷰 샘플 (최신 10개)")
            print("=" * 80)

            for i, review in enumerate(reviews[:10], 1):
                print(f"\n[{i}] {review['user_nm']} | ⭐ {review['rating']} | {review['review_date']}")
                print(f"    {review['content'][:100]}{'...' if len(review['content']) > 100 else ''}")

            # JSON 파일로 저장
            output_file = 'reviews_data.json'
            with open(output_file, 'w', encoding='utf-8') as f:
                # datetime과 Decimal을 문자열로 변환
                for review in reviews:
                    if isinstance(review['created_at'], datetime):
                        review['created_at'] = review['created_at'].strftime('%Y-%m-%d %H:%M:%S')
                    # Decimal을 float로 변환
                    if isinstance(review['rating'], Decimal):
                        review['rating'] = float(review['rating'])

                json.dump(reviews, f, ensure_ascii=False, indent=2)

            print(f"\n✅ 리뷰 데이터가 '{output_file}' 파일로 저장되었습니다.")

            return reviews

    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        raise
    finally:
        connection.close()

if __name__ == "__main__":
    fetch_reviews()
