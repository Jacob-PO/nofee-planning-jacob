#!/usr/bin/env python3
"""
노피 회사소개서용 DB 데이터 수집 스크립트
수집일: 2025-11-19
"""

import pymysql
import json
from datetime import datetime, date
import os
from pathlib import Path

# 프로젝트 루트에서 .env 읽기
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

def collect_db_data():
    """회사소개서용 핵심 DB 데이터 수집"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            data = {
                'metadata': {
                    'collected_at': datetime.now().isoformat(),
                    'purpose': '회사소개서 작성',
                    'version': '1.0'
                }
            }

            print("=" * 60)
            print("🎯 노피 회사소개서용 DB 데이터 수집")
            print("=" * 60)

            # 1. 핵심 비즈니스 지표
            print("\n📊 1. 핵심 비즈니스 지표 수집 중...")

            cursor.execute("SELECT COUNT(*) as count FROM tb_user")
            data['total_users'] = cursor.fetchone()['count']
            print(f"   ✓ 총 가입자: {data['total_users']:,}명")

            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_phone")
            data['total_quote_applications'] = cursor.fetchone()['count']
            print(f"   ✓ 견적 신청: {data['total_quote_applications']:,}건")

            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_campaign_phone")
            data['total_campaign_applications'] = cursor.fetchone()['count']
            print(f"   ✓ 캠페인 신청: {data['total_campaign_applications']:,}건")

            data['total_applications'] = data['total_quote_applications'] + data['total_campaign_applications']
            print(f"   ✓ 총 신청: {data['total_applications']:,}건")

            cursor.execute("SELECT COUNT(*) as count FROM tb_store_purchase WHERE deleted_yn = 'N'")
            data['total_store_purchases'] = cursor.fetchone()['count']
            print(f"   ✓ 매장 구매 확정: {data['total_store_purchases']:,}건")

            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_phone WHERE step_code = '0201005'")
            data['total_quote_completed'] = cursor.fetchone()['count']

            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_campaign_phone WHERE step_code = '0201005'")
            data['total_campaign_completed'] = cursor.fetchone()['count']

            data['total_completed'] = data['total_quote_completed'] + data['total_campaign_completed']
            print(f"   ✓ 개통 완료: {data['total_completed']:,}건")

            cursor.execute("SELECT COUNT(*) as count FROM tb_store")
            data['total_stores'] = cursor.fetchone()['count']
            print(f"   ✓ 등록 매장: {data['total_stores']:,}개")

            # 2. 월별 신청 추이 (최근 12개월)
            print("\n📈 2. 월별 신청 추이 수집 중...")
            cursor.execute("""
                SELECT
                    DATE_FORMAT(created_at, '%Y-%m') as month,
                    COUNT(*) as count
                FROM (
                    SELECT created_at FROM tb_apply_phone
                    UNION ALL
                    SELECT created_at FROM tb_apply_campaign_phone
                ) combined
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY month
            """)
            data['monthly_applications'] = list(cursor.fetchall())
            print(f"   ✓ {len(data['monthly_applications'])}개월 데이터 수집 완료")

            # 3. 일별 신청 추이 (최근 30일)
            print("\n📅 3. 일별 신청 추이 수집 중...")
            cursor.execute("""
                SELECT
                    DATE(created_at) as date,
                    COUNT(*) as count
                FROM (
                    SELECT created_at FROM tb_apply_phone
                    UNION ALL
                    SELECT created_at FROM tb_apply_campaign_phone
                ) combined
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                GROUP BY DATE(created_at)
                ORDER BY date
            """)
            data['daily_applications_recent'] = list(cursor.fetchall())
            print(f"   ✓ {len(data['daily_applications_recent'])}일 데이터 수집 완료")

            # 4. 매장 퍼포먼스
            print("\n🏪 4. 매장 퍼포먼스 수집 중...")
            cursor.execute("""
                SELECT
                    s.store_no,
                    s.nickname,
                    COUNT(sp.purchase_no) as purchase_count
                FROM tb_store s
                LEFT JOIN tb_store_purchase sp ON s.store_no = sp.store_no AND sp.deleted_yn = 'N'
                GROUP BY s.store_no, s.nickname
                HAVING purchase_count > 0
                ORDER BY purchase_count DESC
                LIMIT 10
            """)
            data['top_stores'] = list(cursor.fetchall())
            print(f"   ✓ 상위 {len(data['top_stores'])}개 매장 데이터 수집 완료")

            # 5. 리뷰 데이터
            print("\n⭐ 5. 리뷰 데이터 수집 중...")

            # 실제 리뷰
            cursor.execute("SELECT COUNT(*) as count FROM tb_review_phone WHERE deleted_yn = 'N'")
            review_phone_count = cursor.fetchone()['count']

            cursor.execute("SELECT COUNT(*) as count FROM tb_review_store_phone WHERE deleted_yn = 'N'")
            review_store_phone_count = cursor.fetchone()['count']

            cursor.execute("SELECT COUNT(*) as count FROM tb_review_campaign_phone WHERE deleted_yn = 'N'")
            review_campaign_count = cursor.fetchone()['count']

            # 가상 리뷰
            cursor.execute("SELECT COUNT(*) as count FROM tb_review_virtual WHERE deleted_yn = 'N'")
            virtual_review_count = cursor.fetchone()['count']

            cursor.execute("SELECT COUNT(*) as count FROM tb_review_store_phone_virtual WHERE deleted_yn = 'N'")
            virtual_store_review_count = cursor.fetchone()['count']

            data['reviews'] = {
                'real_reviews': {
                    'phone': review_phone_count,
                    'store_phone': review_store_phone_count,
                    'campaign': review_campaign_count,
                    'total': review_phone_count + review_store_phone_count + review_campaign_count
                },
                'virtual_reviews': {
                    'general': virtual_review_count,
                    'store': virtual_store_review_count,
                    'total': virtual_review_count + virtual_store_review_count
                }
            }
            data['reviews']['total_reviews'] = data['reviews']['real_reviews']['total'] + data['reviews']['virtual_reviews']['total']

            print(f"   ✓ 실제 리뷰: {data['reviews']['real_reviews']['total']:,}건")
            print(f"   ✓ 가상 리뷰: {data['reviews']['virtual_reviews']['total']:,}건")
            print(f"   ✓ 총 리뷰: {data['reviews']['total_reviews']:,}건")

            # 6. 전환율 계산
            print("\n📊 6. 전환율 계산 중...")
            data['conversion_rates'] = {
                'application_to_purchase': round((data['total_store_purchases'] / data['total_applications'] * 100), 2) if data['total_applications'] > 0 else 0,
                'application_to_completion': round((data['total_completed'] / data['total_applications'] * 100), 2) if data['total_applications'] > 0 else 0,
                'purchase_to_completion': round((data['total_completed'] / data['total_store_purchases'] * 100), 2) if data['total_store_purchases'] > 0 else 0
            }
            print(f"   ✓ 신청→구매: {data['conversion_rates']['application_to_purchase']}%")
            print(f"   ✓ 신청→개통: {data['conversion_rates']['application_to_completion']}%")
            print(f"   ✓ 구매→개통: {data['conversion_rates']['purchase_to_completion']}%")

            # 7. 최근 활동 지표 (최근 30일)
            print("\n🔥 7. 최근 30일 활동 지표 수집 중...")
            cursor.execute("""
                SELECT COUNT(*) as count
                FROM tb_user
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
            """)
            data['recent_30d_users'] = cursor.fetchone()['count']

            cursor.execute("""
                SELECT COUNT(*) as count
                FROM (
                    SELECT created_at FROM tb_apply_phone WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                    UNION ALL
                    SELECT created_at FROM tb_apply_campaign_phone WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                ) combined
            """)
            data['recent_30d_applications'] = cursor.fetchone()['count']

            print(f"   ✓ 최근 30일 가입자: {data['recent_30d_users']:,}명")
            print(f"   ✓ 최근 30일 신청: {data['recent_30d_applications']:,}건")

            return data

    finally:
        connection.close()

def save_data(data, output_dir):
    """데이터를 JSON 파일로 저장"""
    output_path = Path(output_dir) / f"db_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

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
        data = collect_db_data()

        # 데이터 저장
        script_dir = Path(__file__).parent
        data_dir = script_dir.parent / 'data'
        data_dir.mkdir(exist_ok=True)

        output_path = save_data(data, data_dir)

        print("\n" + "=" * 60)
        print("✅ DB 데이터 수집 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        raise

if __name__ == "__main__":
    main()
