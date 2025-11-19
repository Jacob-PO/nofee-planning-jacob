#!/usr/bin/env python3
"""
노피 서비스 버전별 개통율 분석 스크립트

버전 1: 일반견적 (전국 평균 가격 기반 매칭)
버전 2: 캠페인 견적 (동네성지 v1 - 판매점 직접 상품 업로드)
버전 3: 동네 견적 (동네성지 v2 - 검색 기능, 판매점 어드민 개선)
"""

import pymysql
import json
from datetime import datetime
from pathlib import Path
from collections import defaultdict

# DB 연결 정보
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

# 서비스 버전 시기 구분 (배포 시기 기준)
SERVICE_VERSIONS = {
    'v1_general': {
        'name': '버전 1: 일반견적',
        'description': '전국 평균 가격 기반, 노피 중개 매칭',
        'start_date': '2020-01-01',
        'end_date': '2025-06-30',  # 캠페인 견적 본격화 전까지
        'table': 'tb_apply_phone',
        'features': [
            '노피가 제공하는 전국 평균 가격으로 견적 제공',
            '고객 지역 기반 판매점 매칭',
            'DB 판매 방식'
        ]
    },
    'v2_campaign': {
        'name': '버전 2: 캠페인 견적 (동네성지 v1)',
        'description': '판매점 직접 상품 업로드, 고객 직접 선택',
        'start_date': '2025-07-01',
        'end_date': '2025-08-31',  # 동네성지 v2 업데이트 전까지
        'table': 'tb_apply_campaign_phone',
        'features': [
            '판매점이 직접 상품 및 가격 업로드',
            '고객이 원하는 판매점 상품 직접 선택',
            'DB 즉시 전달 (매칭 단계 축소)',
            '동네성지 기능 도입'
        ]
    },
    'v3_local': {
        'name': '버전 3: 동네 견적 (동네성지 v2)',
        'description': '동네성지 검색, 판매점 어드민 대폭 개선',
        'start_date': '2025-09-01',  # 실제 배포 시기로 업데이트 필요
        'end_date': '2025-12-31',
        'table': 'tb_apply_campaign_phone',  # 캠페인 견적과 동일 테이블 사용
        'features': [
            '동네성지 검색 기능 추가',
            '일반 견적 기능 제거 (100% 동네성지)',
            '판매점 어드민 대규모 업데이트',
            '판매점/고객 VOC 기반 UX 개선',
            '판매점이 모든 상품 직접 관리'
        ]
    }
}

def analyze_version_conversion():
    """서비스 버전별 개통율 분석"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            print("=" * 80)
            print("📊 노피 서비스 버전별 개통율 분석")
            print("=" * 80)

            analysis = {
                'metadata': {
                    'analyzed_at': datetime.now().isoformat(),
                    'purpose': '서비스 버전별 개통율 및 성과 분석',
                    'version': '1.0'
                },
                'service_versions': SERVICE_VERSIONS,
                'version_analysis': {}
            }

            # 각 버전별 분석
            for version_key, version_info in SERVICE_VERSIONS.items():
                print(f"\n{'='*80}")
                print(f"📈 {version_info['name']}")
                print(f"기간: {version_info['start_date']} ~ {version_info['end_date']}")
                print(f"테이블: {version_info['table']}")
                print(f"{'='*80}")

                table = version_info['table']
                start_date = version_info['start_date']
                end_date = version_info['end_date']

                # 1. 전체 신청 수
                cursor.execute(f"""
                    SELECT COUNT(*) as total
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                """, (start_date, end_date))
                total_applications = cursor.fetchone()['total']

                # 2. Step Code별 분포
                cursor.execute(f"""
                    SELECT
                        step_code,
                        COUNT(*) as count,
                        ROUND(COUNT(*) * 100.0 / %s, 2) as percentage
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                    GROUP BY step_code
                    ORDER BY count DESC
                """, (total_applications if total_applications > 0 else 1, start_date, end_date))
                step_distribution = list(cursor.fetchall())

                # 3. 개통 완료 (0201005)
                cursor.execute(f"""
                    SELECT COUNT(*) as count
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND step_code = '0201005'
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                """, (start_date, end_date))
                completed = cursor.fetchone()['count']

                # 4. 개통 안함 (반려 + 취소 + 대응완료 후 미진행)
                cursor.execute(f"""
                    SELECT COUNT(*) as count
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND step_code IN ('0201006', '0201007', '0201003')
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                """, (start_date, end_date))
                not_completed = cursor.fetchone()['count']

                # 5. 진행중 (신청접수, 진행중, 개통 진행중)
                cursor.execute(f"""
                    SELECT COUNT(*) as count
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND step_code IN ('0201001', '0201002', '0201004')
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                """, (start_date, end_date))
                in_progress = cursor.fetchone()['count']

                # 6. 매장 구매 확정 수 (해당 기간)
                cursor.execute(f"""
                    SELECT COUNT(DISTINCT sp.purchase_no) as count
                    FROM tb_store_purchase sp
                    INNER JOIN {table} a ON sp.apply_no = a.apply_no
                    WHERE sp.deleted_yn = 'N'
                        AND a.deleted_yn = 'N'
                        AND DATE(a.created_at) >= %s
                        AND DATE(a.created_at) <= %s
                """, (start_date, end_date))
                store_purchases = cursor.fetchone()['count']

                # 7. 월별 추이
                cursor.execute(f"""
                    SELECT
                        DATE_FORMAT(created_at, '%%Y-%%m') as month,
                        COUNT(*) as total_applications,
                        SUM(CASE WHEN step_code = '0201005' THEN 1 ELSE 0 END) as completed,
                        ROUND(SUM(CASE WHEN step_code = '0201005' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as conversion_rate
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                    GROUP BY month
                    ORDER BY month
                """, (start_date, end_date))
                monthly_trend = list(cursor.fetchall())

                # 8. 평균 개통 소요 시간 (신청 → 개통)
                cursor.execute(f"""
                    SELECT
                        AVG(TIMESTAMPDIFF(DAY, created_at, completed_at)) as avg_days,
                        MIN(TIMESTAMPDIFF(DAY, created_at, completed_at)) as min_days,
                        MAX(TIMESTAMPDIFF(DAY, created_at, completed_at)) as max_days
                    FROM {table}
                    WHERE deleted_yn = 'N'
                        AND step_code = '0201005'
                        AND completed_at IS NOT NULL
                        AND DATE(created_at) >= %s
                        AND DATE(created_at) <= %s
                """, (start_date, end_date))
                completion_time = cursor.fetchone()

                # 계산된 지표
                conversion_rate = round(completed / total_applications * 100, 2) if total_applications > 0 else 0
                purchase_rate = round(store_purchases / total_applications * 100, 2) if total_applications > 0 else 0
                purchase_to_completion = round(completed / store_purchases * 100, 2) if store_purchases > 0 else 0

                version_data = {
                    'basic_metrics': {
                        'total_applications': total_applications,
                        'completed': completed,
                        'not_completed': not_completed,
                        'in_progress': in_progress,
                        'store_purchases': store_purchases
                    },
                    'conversion_rates': {
                        'application_to_completion': f"{conversion_rate}%",
                        'application_to_purchase': f"{purchase_rate}%",
                        'purchase_to_completion': f"{purchase_to_completion}%"
                    },
                    'step_distribution': step_distribution,
                    'monthly_trend': monthly_trend,
                    'completion_time': completion_time if completion_time['avg_days'] else {
                        'avg_days': None,
                        'min_days': None,
                        'max_days': None
                    }
                }

                analysis['version_analysis'][version_key] = version_data

                # 출력
                print(f"\n📊 기본 지표:")
                print(f"   총 신청: {total_applications:,}건")
                print(f"   개통 완료: {completed:,}건")
                print(f"   개통 안함: {not_completed:,}건 (반려/취소/대응완료)")
                print(f"   진행중: {in_progress:,}건")
                print(f"   매장 구매: {store_purchases:,}건")

                print(f"\n💯 전환율:")
                print(f"   신청 → 개통: {conversion_rate}%")
                print(f"   신청 → 매장구매: {purchase_rate}%")
                print(f"   매장구매 → 개통: {purchase_to_completion}%")

                if completion_time and completion_time['avg_days']:
                    print(f"\n⏱️  개통 소요 시간:")
                    print(f"   평균: {completion_time['avg_days']:.1f}일")
                    print(f"   최소: {completion_time['min_days']}일")
                    print(f"   최대: {completion_time['max_days']}일")

                print(f"\n📅 월별 추이 (상위 5개월):")
                for item in monthly_trend[:5]:
                    print(f"   {item['month']}: {item['total_applications']:,}건 신청, {item['completed']:,}건 개통 ({item['conversion_rate']}%)")

            # 버전 간 비교
            print(f"\n{'='*80}")
            print("📊 버전 간 비교 분석")
            print("="*80)

            comparison = {}
            for version_key, version_data in analysis['version_analysis'].items():
                version_name = SERVICE_VERSIONS[version_key]['name']
                metrics = version_data['basic_metrics']
                rates = version_data['conversion_rates']

                comparison[version_key] = {
                    'name': version_name,
                    'total_applications': metrics['total_applications'],
                    'completed': metrics['completed'],
                    'conversion_rate': rates['application_to_completion']
                }

                print(f"\n{version_name}:")
                print(f"   총 신청: {metrics['total_applications']:,}건")
                print(f"   개통 완료: {metrics['completed']:,}건")
                print(f"   개통율: {rates['application_to_completion']}")

            analysis['version_comparison'] = comparison

            return analysis

    finally:
        connection.close()

def save_analysis(analysis, output_dir):
    """분석 결과 저장"""
    output_path = Path(output_dir) / f"service_version_conversion_analysis_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    def json_serial(obj):
        from datetime import date
        from decimal import Decimal
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        if isinstance(obj, Decimal):
            return float(obj)
        if obj is None:
            return None
        raise TypeError(f"Type {type(obj)} not serializable")

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2, default=json_serial)

    print(f"\n💾 분석 결과 저장: {output_path}")
    return output_path

def main():
    """메인 실행"""
    try:
        # 분석 실행
        analysis = analyze_version_conversion()

        # 결과 저장
        script_dir = Path(__file__).parent
        output_dir = script_dir.parent.parent / '2-processed-data' / 'reports'
        output_dir.mkdir(parents=True, exist_ok=True)

        output_path = save_analysis(analysis, output_dir)

        print("\n" + "=" * 80)
        print("✅ 서비스 버전별 개통율 분석 완료!")
        print("=" * 80)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
