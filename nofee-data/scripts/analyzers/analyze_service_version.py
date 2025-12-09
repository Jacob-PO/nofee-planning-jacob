#!/usr/bin/env python3
"""
노피 서비스 버전별 개통율 분석 스크립트

중요: 2025년 7월부터는 일반견적(tb_apply_phone)과 캠페인견적(tb_apply_campaign_phone) 모두 동시 운영
버전별 분류는 시기별 특징을 나타내며, 실제로는 두 유형이 병행 운영됨

버전 1: 7-8월 (동네성지 v1 - 검색 없이 캠페인 위주)
버전 2: 9-10월 (동네성지 v2 - 검색 기능 추가, 어드민 개선)
버전 3: 11월 (공식 출시 - VOC 반영, 안정화)
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

# 서비스 버전 시기 구분 (배포 시기 기준)
# 중요: 7월부터는 일반견적과 캠페인견적 모두 동시 운영
SERVICE_VERSIONS = {
    'v1_beta_phase1': {
        'name': '버전 1: 동네성지 v1 (7-8월)',
        'description': '캠페인 견적 위주, 검색 기능 없음',
        'start_date': '2025-07-01',
        'end_date': '2025-08-31',
        'features': [
            '판매점이 직접 상품 및 가격 업로드',
            '고객이 캠페인 상품 직접 선택',
            '일반 견적도 병행 운영',
            '동네성지 기능 도입 (검색 없음)'
        ]
    },
    'v2_beta_phase2': {
        'name': '버전 2: 동네성지 v2 (9-10월)',
        'description': '검색 기능 추가, 판매점 어드민 개선',
        'start_date': '2025-09-01',
        'end_date': '2025-10-31',
        'features': [
            '동네성지 검색 기능 추가',
            '판매점 어드민 대규모 업데이트',
            '판매점/고객 VOC 기반 UX 개선',
            '일반 견적 + 캠페인 견적 병행',
            '개통율 대폭 향상 (10월 17.40% 달성)'
        ]
    },
    'v3_official_launch': {
        'name': '버전 3: 공식 출시 (11월~)',
        'description': '베타 종료, 공식 서비스 런칭',
        'start_date': '2025-11-01',
        'end_date': '2025-12-31',
        'features': [
            '베타 테스트 완료',
            '공식 서비스 시작',
            'VOC 100% 반영',
            '안정적 개통율 유지',
            '일반 견적 + 캠페인 견적 최적화'
        ]
    }
}

def analyze_table_data(cursor, table, start_date, end_date):
    """단일 테이블 데이터 분석"""
    # 전체 신청 수
    cursor.execute(f"""
        SELECT COUNT(*) as total
        FROM {table}
        WHERE deleted_yn = 'N'
            AND DATE(created_at) >= %s
            AND DATE(created_at) <= %s
    """, (start_date, end_date))
    total = cursor.fetchone()['total']

    # 개통 완료
    cursor.execute(f"""
        SELECT COUNT(*) as count
        FROM {table}
        WHERE deleted_yn = 'N'
            AND step_code = '0201005'
            AND DATE(created_at) >= %s
            AND DATE(created_at) <= %s
    """, (start_date, end_date))
    completed = cursor.fetchone()['count']

    # 월별 추이
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
    monthly = list(cursor.fetchall())

    return {
        'total': total,
        'completed': completed,
        'monthly': monthly
    }

def analyze_version_conversion():
    """서비스 버전별 개통율 분석 (일반견적 + 캠페인견적 통합)"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            print("=" * 80)
            print("📊 노피 서비스 버전별 개통율 분석 (일반견적 + 캠페인견적 통합)")
            print("=" * 80)

            analysis = {
                'metadata': {
                    'analyzed_at': datetime.now().isoformat(),
                    'purpose': '서비스 버전별 개통율 및 성과 분석 (2025년 7월부터 양쪽 테이블 통합)',
                    'version': '2.0',
                    'note': '일반견적(tb_apply_phone)과 캠페인견적(tb_apply_campaign_phone) 동시 운영'
                },
                'service_versions': SERVICE_VERSIONS,
                'version_analysis': {}
            }

            # 각 버전별 분석
            for version_key, version_info in SERVICE_VERSIONS.items():
                print(f"\n{'='*80}")
                print(f"📈 {version_info['name']}")
                print(f"기간: {version_info['start_date']} ~ {version_info['end_date']}")
                print(f"{'='*80}")

                start_date = version_info['start_date']
                end_date = version_info['end_date']

                # 일반 견적 데이터
                general_data = analyze_table_data(cursor, 'tb_apply_phone', start_date, end_date)

                # 캠페인 견적 데이터
                campaign_data = analyze_table_data(cursor, 'tb_apply_campaign_phone', start_date, end_date)

                # 통합 계산
                total_applications = general_data['total'] + campaign_data['total']
                total_completed = general_data['completed'] + campaign_data['completed']

                # 전환율 계산
                conversion_rate = round(total_completed / total_applications * 100, 2) if total_applications > 0 else 0
                general_rate = round(general_data['completed'] / general_data['total'] * 100, 2) if general_data['total'] > 0 else 0
                campaign_rate = round(campaign_data['completed'] / campaign_data['total'] * 100, 2) if campaign_data['total'] > 0 else 0

                # 월별 통합 데이터
                monthly_combined = {}
                for item in general_data['monthly']:
                    month = item['month']
                    if month not in monthly_combined:
                        monthly_combined[month] = {
                            'month': month,
                            'general_total': 0,
                            'general_completed': 0,
                            'campaign_total': 0,
                            'campaign_completed': 0
                        }
                    monthly_combined[month]['general_total'] = item['total_applications']
                    monthly_combined[month]['general_completed'] = item['completed']

                for item in campaign_data['monthly']:
                    month = item['month']
                    if month not in monthly_combined:
                        monthly_combined[month] = {
                            'month': month,
                            'general_total': 0,
                            'general_completed': 0,
                            'campaign_total': 0,
                            'campaign_completed': 0
                        }
                    monthly_combined[month]['campaign_total'] = item['total_applications']
                    monthly_combined[month]['campaign_completed'] = item['completed']

                # 월별 통합 전환율 계산
                monthly_trend = []
                for month, data in sorted(monthly_combined.items()):
                    total_apps = data['general_total'] + data['campaign_total']
                    total_comp = data['general_completed'] + data['campaign_completed']
                    conv_rate = round(total_comp / total_apps * 100, 2) if total_apps > 0 else 0

                    monthly_trend.append({
                        'month': month,
                        'general_applications': data['general_total'],
                        'general_completed': data['general_completed'],
                        'general_conversion_rate': round(data['general_completed'] / data['general_total'] * 100, 2) if data['general_total'] > 0 else 0,
                        'campaign_applications': data['campaign_total'],
                        'campaign_completed': data['campaign_completed'],
                        'campaign_conversion_rate': round(data['campaign_completed'] / data['campaign_total'] * 100, 2) if data['campaign_total'] > 0 else 0,
                        'total_applications': total_apps,
                        'total_completed': total_comp,
                        'total_conversion_rate': conv_rate
                    })

                version_data = {
                    'period': {
                        'start_date': start_date,
                        'end_date': end_date
                    },
                    'general_quote': {
                        'total_applications': general_data['total'],
                        'completed': general_data['completed'],
                        'conversion_rate': f"{general_rate}%"
                    },
                    'campaign_quote': {
                        'total_applications': campaign_data['total'],
                        'completed': campaign_data['completed'],
                        'conversion_rate': f"{campaign_rate}%"
                    },
                    'combined_total': {
                        'total_applications': total_applications,
                        'total_completed': total_completed,
                        'total_conversion_rate': f"{conversion_rate}%"
                    },
                    'monthly_trend': monthly_trend
                }

                analysis['version_analysis'][version_key] = version_data

                # 출력
                print(f"\n📊 일반 견적 (tb_apply_phone):")
                print(f"   총 신청: {general_data['total']:,}건")
                print(f"   개통 완료: {general_data['completed']:,}건")
                print(f"   개통율: {general_rate}%")

                print(f"\n📊 캠페인 견적 (tb_apply_campaign_phone):")
                print(f"   총 신청: {campaign_data['total']:,}건")
                print(f"   개통 완료: {campaign_data['completed']:,}건")
                print(f"   개통율: {campaign_rate}%")

                print(f"\n💯 통합 전환율:")
                print(f"   총 신청: {total_applications:,}건")
                print(f"   총 개통: {total_completed:,}건")
                print(f"   전체 개통율: {conversion_rate}%")

                print(f"\n📅 월별 상세 추이:")
                for item in monthly_trend:
                    print(f"   [{item['month']}]")
                    print(f"     일반: {item['general_applications']:,}건 → {item['general_completed']:,}건 ({item['general_conversion_rate']}%)")
                    print(f"     캠페인: {item['campaign_applications']:,}건 → {item['campaign_completed']:,}건 ({item['campaign_conversion_rate']}%)")
                    print(f"     합계: {item['total_applications']:,}건 → {item['total_completed']:,}건 ({item['total_conversion_rate']}%) ⭐")

            # 버전 간 비교
            print(f"\n{'='*80}")
            print("📊 버전 간 비교 분석")
            print("="*80)

            comparison = {}
            for version_key, version_data in analysis['version_analysis'].items():
                version_name = SERVICE_VERSIONS[version_key]['name']
                combined = version_data['combined_total']

                comparison[version_key] = {
                    'name': version_name,
                    'total_applications': combined['total_applications'],
                    'total_completed': combined['total_completed'],
                    'total_conversion_rate': combined['total_conversion_rate']
                }

                print(f"\n{version_name}:")
                print(f"   총 신청: {combined['total_applications']:,}건")
                print(f"   총 개통: {combined['total_completed']:,}건")
                print(f"   전체 개통율: {combined['total_conversion_rate']}")

            analysis['version_comparison'] = comparison

            return analysis

    finally:
        connection.close()

def save_analysis(analysis, output_dir):
    """분석 결과 저장"""
    # 최신 파일 저장
    latest_path = Path(output_dir) / "service_version_analysis_latest.json"

    # 타임스탬프 파일 저장
    timestamp_path = Path(output_dir) / f"service_version_analysis_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

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

    # 최신 파일 저장
    with open(latest_path, 'w', encoding='utf-8') as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2, default=json_serial)

    # 타임스탬프 파일 저장
    with open(timestamp_path, 'w', encoding='utf-8') as f:
        json.dump(analysis, f, ensure_ascii=False, indent=2, default=json_serial)

    print(f"\n💾 분석 결과 저장:")
    print(f"   최신: {latest_path}")
    print(f"   백업: {timestamp_path}")
    return latest_path

def main():
    """메인 실행"""
    try:
        # 분석 실행
        analysis = analyze_version_conversion()

        # 결과 저장
        script_dir = Path(__file__).parent
        output_dir = script_dir.parent.parent / 'reports'
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
