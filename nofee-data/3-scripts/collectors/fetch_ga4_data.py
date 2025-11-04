#!/usr/bin/env python3
"""
GA4 데이터 수집 스크립트
- Google Analytics Data API (GA4)를 사용하여 노피 웹사이트 데이터 수집
"""

import json
from datetime import datetime, timedelta
from google.analytics.data_v1beta import BetaAnalyticsDataClient
from google.analytics.data_v1beta.types import (
    DateRange,
    Dimension,
    Metric,
    RunReportRequest,
)
from google.oauth2 import service_account

# GA4 Property ID
GA4_PROPERTY_ID = "properties/461672035"  # nofee.team

# Google API 서비스 계정 키 파일
KEY_FILE = "/Users/jacob/Desktop/dev/config/google_api_key.json"

def get_ga4_client():
    """GA4 클라이언트 생성"""
    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE,
        scopes=["https://www.googleapis.com/auth/analytics.readonly"]
    )
    return BetaAnalyticsDataClient(credentials=credentials)

def fetch_traffic_overview(client, start_date, end_date):
    """전체 트래픽 개요 조회"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="sessionDefaultChannelGroup"),
        ],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
            Metric(name="newUsers"),
            Metric(name="engagementRate"),
            Metric(name="bounceRate"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        results.append({
            "channel": row.dimension_values[0].value,
            "sessions": int(row.metric_values[0].value),
            "total_users": int(row.metric_values[1].value),
            "new_users": int(row.metric_values[2].value),
            "engagement_rate": float(row.metric_values[3].value),
            "bounce_rate": float(row.metric_values[4].value),
        })

    return results

def fetch_traffic_source(client, start_date, end_date):
    """트래픽 소스별 상세 조회"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="sessionSource"),
            Dimension(name="sessionMedium"),
        ],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        results.append({
            "source": row.dimension_values[0].value,
            "medium": row.dimension_values[1].value,
            "sessions": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
        })

    return results

def fetch_daily_trend(client, start_date, end_date):
    """일별 트래픽 추이"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="date"),
        ],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
            Metric(name="newUsers"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        date_str = row.dimension_values[0].value
        formatted_date = f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"
        results.append({
            "date": formatted_date,
            "sessions": int(row.metric_values[0].value),
            "total_users": int(row.metric_values[1].value),
            "new_users": int(row.metric_values[2].value),
        })

    return sorted(results, key=lambda x: x['date'])

def fetch_page_views(client, start_date, end_date):
    """페이지뷰 통계"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="pagePath"),
            Dimension(name="pageTitle"),
        ],
        metrics=[
            Metric(name="screenPageViews"),
            Metric(name="totalUsers"),
        ],
        limit=20,
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        results.append({
            "page_path": row.dimension_values[0].value,
            "page_title": row.dimension_values[1].value,
            "page_views": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
        })

    return results

def main():
    print("="*80)
    print("🚀 GA4 데이터 수집 시작")
    print("="*80)
    print()

    # 클라이언트 생성
    print("📊 GA4 클라이언트 연결 중...")
    client = get_ga4_client()

    # 기간 설정 (최근 30일)
    end_date = datetime.now()
    start_date = end_date - timedelta(days=30)

    start_str = start_date.strftime("%Y-%m-%d")
    end_str = end_date.strftime("%Y-%m-%d")

    print(f"📅 조회 기간: {start_str} ~ {end_str}")
    print()

    data = {}

    # 1. 트래픽 개요
    print("📈 트래픽 개요 조회 중...")
    data['traffic_overview'] = fetch_traffic_overview(client, start_str, end_str)

    # 2. 트래픽 소스
    print("🔍 트래픽 소스 조회 중...")
    data['traffic_source'] = fetch_traffic_source(client, start_str, end_str)

    # 3. 일별 추이
    print("📊 일별 추이 조회 중...")
    data['daily_trend'] = fetch_daily_trend(client, start_str, end_str)

    # 4. 페이지뷰
    print("📄 페이지뷰 조회 중...")
    data['page_views'] = fetch_page_views(client, start_str, end_str)

    # 결과 저장
    result = {
        'metadata': {
            'generated_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'property_id': GA4_PROPERTY_ID,
            'start_date': start_str,
            'end_date': end_str,
        },
        'data': data
    }

    output_file = '../../1-raw-data/ga4/ga4_nofee_data_20251104.json'
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print()
    print("="*80)
    print("✅ GA4 데이터 수집 완료!")
    print(f"📁 저장 위치: {output_file}")
    print()

    # 간단한 요약
    total_sessions = sum(item['sessions'] for item in data['traffic_overview'])
    total_users = sum(item['total_users'] for item in data['traffic_overview'])

    print("📊 요약:")
    print(f"  - 전체 세션: {total_sessions:,}회")
    print(f"  - 전체 사용자: {total_users:,}명")
    print(f"  - 채널 수: {len(data['traffic_overview'])}개")
    print(f"  - 트래픽 소스: {len(data['traffic_source'])}개")
    print()

if __name__ == '__main__':
    main()
