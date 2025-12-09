#!/usr/bin/env python3
"""
노피 견적신청 퍼널 분석 스크립트
모든 신청 관련 테이블 및 step_code 분석
"""

import pymysql
import json
from datetime import datetime
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

def analyze_application_funnel():
    """견적신청 퍼널 전체 분석"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            print("=" * 80)
            print("📊 견적신청 퍼널 분석")
            print("=" * 80)

            analysis = {
                'metadata': {
                    'analyzed_at': datetime.now().isoformat(),
                    'purpose': '견적신청 프로세스 퍼널 분석',
                    'version': '1.0'
                }
            }

            # 1. tb_apply_phone - 일반 견적 신청
            print("\n📱 1. 일반 견적 신청 (tb_apply_phone) 분석 중...")
            cursor.execute("""
                SELECT
                    step_code,
                    COUNT(*) as count
                FROM tb_apply_phone
                WHERE deleted_yn = 'N'
                GROUP BY step_code
                ORDER BY count DESC
            """)
            apply_phone_by_step = list(cursor.fetchall())

            # 총 개수
            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_phone WHERE deleted_yn = 'N'")
            total_apply_phone = cursor.fetchone()['count']

            analysis['apply_phone'] = {
                'total': total_apply_phone,
                'by_step_code': apply_phone_by_step
            }

            print(f"   총 일반 견적 신청: {total_apply_phone:,}건")
            for item in apply_phone_by_step:
                print(f"     - step_code {item['step_code']}: {item['count']:,}건")

            # 2. tb_apply_campaign_phone - 캠페인 견적 신청
            print("\n🎯 2. 캠페인 견적 신청 (tb_apply_campaign_phone) 분석 중...")
            cursor.execute("""
                SELECT
                    step_code,
                    COUNT(*) as count
                FROM tb_apply_campaign_phone
                WHERE deleted_yn = 'N'
                GROUP BY step_code
                ORDER BY count DESC
            """)
            apply_campaign_by_step = list(cursor.fetchall())

            cursor.execute("SELECT COUNT(*) as count FROM tb_apply_campaign_phone WHERE deleted_yn = 'N'")
            total_apply_campaign = cursor.fetchone()['count']

            analysis['apply_campaign'] = {
                'total': total_apply_campaign,
                'by_step_code': apply_campaign_by_step
            }

            print(f"   총 캠페인 견적 신청: {total_apply_campaign:,}건")
            for item in apply_campaign_by_step:
                print(f"     - step_code {item['step_code']}: {item['count']:,}건")

            # 3. Step Code 정의 (수동 매핑 - 코드베이스 기반)
            print("\n🔍 3. Step Code 정의 (코드베이스 기반)...")
            step_code_definitions = {
                '0201001': '신청접수',
                '0201002': '견적확인',
                '0201003': '매장선택',
                '0201004': '방문예약',
                '0201005': '개통완료',
                '0201006': '신청취소',
                '0201007': '매칭실패'
            }

            analysis['step_code_definitions'] = step_code_definitions

            print("   Step Code 정의:")
            for code, name in step_code_definitions.items():
                print(f"     - {code}: {name}")

            # 4. 매장 구매 확정 (tb_store_purchase)
            print("\n🏪 4. 매장 구매 확정 (tb_store_purchase) 분석 중...")
            cursor.execute("""
                SELECT COUNT(*) as count
                FROM tb_store_purchase
                WHERE deleted_yn = 'N'
            """)
            total_store_purchases = cursor.fetchone()['count']

            # 일반 견적 → 매장 구매
            cursor.execute("""
                SELECT COUNT(DISTINCT ap.apply_no) as count
                FROM tb_apply_phone ap
                INNER JOIN tb_store_purchase sp ON ap.apply_no = sp.apply_no
                WHERE ap.deleted_yn = 'N' AND sp.deleted_yn = 'N'
            """)
            apply_phone_to_purchase = cursor.fetchone()['count']

            # 캠페인 견적 → 매장 구매
            cursor.execute("""
                SELECT COUNT(DISTINCT acp.apply_no) as count
                FROM tb_apply_campaign_phone acp
                INNER JOIN tb_store_purchase sp ON acp.apply_no = sp.apply_no
                WHERE acp.deleted_yn = 'N' AND sp.deleted_yn = 'N'
            """)
            apply_campaign_to_purchase = cursor.fetchone()['count']

            analysis['store_purchases'] = {
                'total': total_store_purchases,
                'from_apply_phone': apply_phone_to_purchase,
                'from_apply_campaign': apply_campaign_to_purchase
            }

            print(f"   총 매장 구매 확정: {total_store_purchases:,}건")
            print(f"     - 일반 견적 → 매장 구매: {apply_phone_to_purchase:,}건")
            print(f"     - 캠페인 견적 → 매장 구매: {apply_campaign_to_purchase:,}건")

            # 5. 개통 완료 (step_code = '0201005')
            print("\n✅ 5. 개통 완료 분석 중...")

            # 일반 견적에서 개통 완료
            cursor.execute("""
                SELECT COUNT(*) as count
                FROM tb_apply_phone
                WHERE deleted_yn = 'N' AND step_code = '0201005'
            """)
            apply_phone_completed = cursor.fetchone()['count']

            # 캠페인 견적에서 개통 완료
            cursor.execute("""
                SELECT COUNT(*) as count
                FROM tb_apply_campaign_phone
                WHERE deleted_yn = 'N' AND step_code = '0201005'
            """)
            apply_campaign_completed = cursor.fetchone()['count']

            total_completed = apply_phone_completed + apply_campaign_completed

            analysis['completions'] = {
                'total': total_completed,
                'from_apply_phone': apply_phone_completed,
                'from_apply_campaign': apply_campaign_completed
            }

            print(f"   총 개통 완료: {total_completed:,}건")
            print(f"     - 일반 견적: {apply_phone_completed:,}건")
            print(f"     - 캠페인 견적: {apply_campaign_completed:,}건")

            # 6. 전체 퍼널 계산
            print("\n📊 6. 전체 퍼널 분석 중...")

            total_applications = total_apply_phone + total_apply_campaign

            funnel = {
                '1_신청': {
                    'total': total_applications,
                    'apply_phone': total_apply_phone,
                    'apply_campaign': total_apply_campaign,
                    'percentage': 100.0
                },
                '2_매장구매확정': {
                    'total': total_store_purchases,
                    'from_apply_phone': apply_phone_to_purchase,
                    'from_apply_campaign': apply_campaign_to_purchase,
                    'percentage': round(total_store_purchases / total_applications * 100, 2) if total_applications > 0 else 0,
                    'conversion_rate': round(total_store_purchases / total_applications * 100, 2) if total_applications > 0 else 0
                },
                '3_개통완료': {
                    'total': total_completed,
                    'from_apply_phone': apply_phone_completed,
                    'from_apply_campaign': apply_campaign_completed,
                    'percentage': round(total_completed / total_applications * 100, 2) if total_applications > 0 else 0,
                    'conversion_from_application': round(total_completed / total_applications * 100, 2) if total_applications > 0 else 0,
                    'conversion_from_purchase': round(total_completed / total_store_purchases * 100, 2) if total_store_purchases > 0 else 0
                }
            }

            analysis['funnel'] = funnel

            print("\n   전체 퍼널:")
            print(f"     1단계 - 신청: {total_applications:,}건 (100.0%)")
            print(f"     2단계 - 매장 구매 확정: {total_store_purchases:,}건 ({funnel['2_매장구매확정']['percentage']}%)")
            print(f"     3단계 - 개통 완료: {total_completed:,}건 ({funnel['3_개통완료']['percentage']}%)")
            print(f"\n   전환율:")
            print(f"     신청 → 매장 구매: {funnel['2_매장구매확정']['conversion_rate']}%")
            print(f"     신청 → 개통 완료: {funnel['3_개통완료']['conversion_from_application']}%")
            print(f"     매장 구매 → 개통 완료: {funnel['3_개통완료']['conversion_from_purchase']}%")

            # 7. 월별 추이 분석
            print("\n📅 7. 월별 신청 추이 분석 중...")
            cursor.execute("""
                SELECT
                    DATE_FORMAT(created_at, '%Y-%m') as month,
                    COUNT(*) as count
                FROM (
                    SELECT created_at FROM tb_apply_phone WHERE deleted_yn = 'N'
                    UNION ALL
                    SELECT created_at FROM tb_apply_campaign_phone WHERE deleted_yn = 'N'
                ) as all_applications
                GROUP BY month
                ORDER BY month DESC
                LIMIT 12
            """)
            monthly_trend = list(cursor.fetchall())

            analysis['monthly_trend'] = monthly_trend

            print("   최근 12개월 신청 추이:")
            for item in monthly_trend:
                print(f"     - {item['month']}: {item['count']:,}건")

            # 8. 일별 최근 30일 추이
            print("\n📅 8. 최근 30일 일별 신청 추이 분석 중...")
            cursor.execute("""
                SELECT
                    DATE(created_at) as date,
                    COUNT(*) as count
                FROM (
                    SELECT created_at FROM tb_apply_phone WHERE deleted_yn = 'N' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                    UNION ALL
                    SELECT created_at FROM tb_apply_campaign_phone WHERE deleted_yn = 'N' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                ) as recent_applications
                GROUP BY date
                ORDER BY date DESC
            """)
            daily_trend = list(cursor.fetchall())

            analysis['daily_trend_30d'] = daily_trend

            print(f"   최근 30일 총 신청: {sum(item['count'] for item in daily_trend):,}건")
            print(f"   일평균 신청: {sum(item['count'] for item in daily_trend) / len(daily_trend):.1f}건" if daily_trend else "   데이터 없음")

            # 9. 상품별 신청 분석
            print("\n📱 9. 상품별 신청 분석 중...")
            cursor.execute("""
                SELECT
                    pg.product_group_nm as product_name,
                    COUNT(*) as count
                FROM (
                    SELECT apply_product_group_code as product_group_code
                    FROM tb_apply_phone
                    WHERE deleted_yn = 'N' AND apply_product_group_code IS NOT NULL
                    UNION ALL
                    SELECT apply_product_group_code as product_group_code
                    FROM tb_apply_campaign_phone
                    WHERE deleted_yn = 'N' AND apply_product_group_code IS NOT NULL
                ) as all_applications
                INNER JOIN tb_product_group_phone pg ON all_applications.product_group_code = pg.product_group_code
                WHERE pg.deleted_yn = 'N'
                GROUP BY pg.product_group_nm
                ORDER BY count DESC
                LIMIT 10
            """)
            top_products = list(cursor.fetchall())

            analysis['top_products_by_applications'] = top_products

            print("   상위 10개 신청 상품:")
            for idx, item in enumerate(top_products, 1):
                print(f"     {idx}. {item['product_name']}: {item['count']:,}건")

            return analysis

    finally:
        connection.close()

def save_analysis(analysis, output_dir):
    """분석 결과 저장"""
    output_path = Path(output_dir) / f"application_funnel_analysis_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    def json_serial(obj):
        from datetime import date
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        raise TypeError(f"Type {type(obj)} not serializable")

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2, default=json_serial)

    print(f"\n💾 분석 결과 저장: {output_path}")
    return output_path

def main():
    """메인 실행"""
    try:
        # 분석 실행
        analysis = analyze_application_funnel()

        # 결과 저장
        script_dir = Path(__file__).parent
        output_dir = script_dir.parent.parent / '2-processed-data' / 'reports'
        output_dir.mkdir(parents=True, exist_ok=True)

        output_path = save_analysis(analysis, output_dir)

        print("\n" + "=" * 80)
        print("✅ 견적신청 퍼널 분석 완료!")
        print("=" * 80)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
