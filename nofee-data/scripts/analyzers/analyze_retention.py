#!/usr/bin/env python3
"""
노피 리텐션 분석 스크립트
DAU - 새 사용자 = 재방문자를 계산하여 리텐션 도출
수집일: 2025-11-19
"""

import json
from datetime import datetime, timedelta
from pathlib import Path
from google.analytics.data_v1beta import BetaAnalyticsDataClient
from google.analytics.data_v1beta.types import (
    DateRange,
    Dimension,
    Metric,
    RunReportRequest,
)
from google.oauth2 import service_account

# GA4 설정
GA4_PROPERTY_ID = "properties/474694872"
KEY_FILE = "/Users/jacob/Desktop/workspace/nofee/google_api_key.json"

def get_ga4_client():
    """GA4 클라이언트 생성"""
    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE,
        scopes=["https://www.googleapis.com/auth/analytics.readonly"]
    )
    return BetaAnalyticsDataClient(credentials=credentials)

def fetch_daily_users(client, start_date, end_date):
    """일별 사용자 데이터 조회 (DAU, 새 사용자)"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="date"),
        ],
        metrics=[
            Metric(name="activeUsers"),  # DAU
            Metric(name="newUsers"),      # 새 사용자
            Metric(name="sessions"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        date_str = row.dimension_values[0].value
        active_users = int(row.metric_values[0].value)
        new_users = int(row.metric_values[1].value)
        sessions = int(row.metric_values[2].value)

        # 재방문자 = DAU - 새 사용자
        returning_users = active_users - new_users

        results.append({
            "date": f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:]}",
            "active_users": active_users,
            "new_users": new_users,
            "returning_users": returning_users,
            "sessions": sessions,
        })

    return sorted(results, key=lambda x: x['date'])

def calculate_retention_metrics(daily_data):
    """리텐션 지표 계산"""
    total_active_users = sum(d['active_users'] for d in daily_data)
    total_new_users = sum(d['new_users'] for d in daily_data)
    total_returning_users = sum(d['returning_users'] for d in daily_data)
    total_sessions = sum(d['sessions'] for d in daily_data)

    # 전체 리텐션율
    overall_retention_rate = (total_returning_users / total_active_users * 100) if total_active_users > 0 else 0

    # 일평균
    days_count = len(daily_data)
    daily_avg = {
        "active_users": total_active_users / days_count if days_count > 0 else 0,
        "new_users": total_new_users / days_count if days_count > 0 else 0,
        "returning_users": total_returning_users / days_count if days_count > 0 else 0,
        "sessions": total_sessions / days_count if days_count > 0 else 0,
    }

    return {
        "overall": {
            "total_active_users": total_active_users,
            "total_new_users": total_new_users,
            "total_returning_users": total_returning_users,
            "total_sessions": total_sessions,
            "retention_rate": round(overall_retention_rate, 2),
        },
        "daily_average": {
            "active_users": round(daily_avg["active_users"], 1),
            "new_users": round(daily_avg["new_users"], 1),
            "returning_users": round(daily_avg["returning_users"], 1),
            "sessions": round(daily_avg["sessions"], 1),
        }
    }

def calculate_monthly_retention(daily_data):
    """월별 리텐션 계산"""
    from collections import defaultdict

    monthly = defaultdict(lambda: {
        "active_users": 0,
        "new_users": 0,
        "returning_users": 0,
        "sessions": 0,
    })

    for day in daily_data:
        month = day['date'][:7]  # YYYY-MM
        monthly[month]["active_users"] += day['active_users']
        monthly[month]["new_users"] += day['new_users']
        monthly[month]["returning_users"] += day['returning_users']
        monthly[month]["sessions"] += day['sessions']

    results = []
    for month, data in sorted(monthly.items()):
        retention_rate = (data["returning_users"] / data["active_users"] * 100) if data["active_users"] > 0 else 0
        results.append({
            "month": month,
            "active_users": data["active_users"],
            "new_users": data["new_users"],
            "returning_users": data["returning_users"],
            "sessions": data["sessions"],
            "retention_rate": round(retention_rate, 2),
        })

    return results

def analyze_retention():
    """리텐션 분석 실행"""
    print("=" * 60)
    print("🔄 노피 리텐션 분석")
    print("=" * 60)

    # 클라이언트 생성
    print("\n📊 GA4 클라이언트 연결 중...")
    client = get_ga4_client()

    # 기간 설정 - 최근 12개월
    end_date = datetime.now()
    start_date = end_date - timedelta(days=365)

    start_str = start_date.strftime("%Y-%m-%d")
    end_str = end_date.strftime("%Y-%m-%d")

    print(f"📅 조회 기간: {start_str} ~ {end_str}")

    # 1. 일별 사용자 데이터 수집
    print("\n📊 1. 일별 사용자 데이터 수집 중...")
    daily_data = fetch_daily_users(client, start_str, end_str)
    print(f"   ✓ {len(daily_data)}일 데이터 수집 완료")

    # 2. 전체 리텐션 계산
    print("\n🔄 2. 전체 리텐션 계산 중...")
    retention_metrics = calculate_retention_metrics(daily_data)

    print(f"   ✓ 총 액티브 유저: {retention_metrics['overall']['total_active_users']:,}명")
    print(f"   ✓ 총 신규 유저: {retention_metrics['overall']['total_new_users']:,}명")
    print(f"   ✓ 총 재방문 유저: {retention_metrics['overall']['total_returning_users']:,}명")
    print(f"   ✓ 전체 리텐션율: {retention_metrics['overall']['retention_rate']:.2f}%")

    # 3. 월별 리텐션 계산
    print("\n📈 3. 월별 리텐션 계산 중...")
    monthly_retention = calculate_monthly_retention(daily_data)
    print(f"   ✓ {len(monthly_retention)}개월 데이터 계산 완료")

    for month_data in monthly_retention:
        print(f"   [{month_data['month']}] 리텐션율: {month_data['retention_rate']:.2f}% "
              f"(재방문: {month_data['returning_users']:,}명 / 액티브: {month_data['active_users']:,}명)")

    # 베타테스트 기간 (2025-07 ~ 2025-10) 분석
    print("\n🎯 4. 베타테스트 기간 분석 중...")
    beta_months = [m for m in monthly_retention if '2025-07' <= m['month'] <= '2025-10']

    if beta_months:
        beta_total_active = sum(m['active_users'] for m in beta_months)
        beta_total_returning = sum(m['returning_users'] for m in beta_months)
        beta_retention = (beta_total_returning / beta_total_active * 100) if beta_total_active > 0 else 0

        print(f"   ✓ 베타테스트 기간 (2025-07 ~ 2025-10)")
        print(f"   ✓ 총 액티브 유저: {beta_total_active:,}명")
        print(f"   ✓ 총 재방문 유저: {beta_total_returning:,}명")
        print(f"   ✓ 베타 리텐션율: {beta_retention:.2f}%")

    # 데이터 패키징
    data = {
        "metadata": {
            "collected_at": datetime.now().isoformat(),
            "purpose": "리텐션 분석",
            "start_date": start_str,
            "end_date": end_str,
            "version": "1.0"
        },
        "overall": retention_metrics['overall'],
        "daily_average": retention_metrics['daily_average'],
        "monthly_retention": monthly_retention,
        "daily_data": daily_data,
    }

    # 베타테스트 기간 데이터 추가
    if beta_months:
        data["beta_test_period"] = {
            "period": "2025-07 ~ 2025-10",
            "total_active_users": beta_total_active,
            "total_new_users": sum(m['new_users'] for m in beta_months),
            "total_returning_users": beta_total_returning,
            "total_sessions": sum(m['sessions'] for m in beta_months),
            "retention_rate": round(beta_retention, 2),
            "monthly_breakdown": beta_months,
        }

    return data

def save_data(data, output_dir):
    """데이터를 JSON 파일로 저장"""
    # latest 버전
    latest_path = Path(output_dir) / "retention_analysis_latest.json"

    # 타임스탬프 버전
    timestamp_path = Path(output_dir) / f"retention_analysis_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    # latest 버전 저장
    with open(latest_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # 타임스탬프 버전 저장
    with open(timestamp_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\n💾 데이터 저장 완료:")
    print(f"   • Latest: {latest_path}")
    print(f"   • Archive: {timestamp_path}")

    return latest_path

def main():
    """메인 실행 함수"""
    try:
        # 리텐션 분석
        data = analyze_retention()

        # 데이터 저장
        script_dir = Path(__file__).parent.parent
        reports_dir = script_dir / 'reports'
        reports_dir.mkdir(parents=True, exist_ok=True)

        output_path = save_data(data, reports_dir)

        print("\n" + "=" * 60)
        print("✅ 리텐션 분석 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
