#!/usr/bin/env python3
"""
노피 회사소개서용 GA4 데이터 수집 스크립트
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

def fetch_overall_metrics(client, start_date, end_date):
    """전체 기간 핵심 지표 조회"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
            Metric(name="newUsers"),
            Metric(name="screenPageViews"),
            Metric(name="averageSessionDuration"),
            Metric(name="bounceRate"),
        ],
    )

    response = client.run_report(request)

    if response.rows:
        row = response.rows[0]
        return {
            "sessions": int(row.metric_values[0].value),
            "total_users": int(row.metric_values[1].value),
            "new_users": int(row.metric_values[2].value),
            "page_views": int(row.metric_values[3].value),
            "avg_session_duration": float(row.metric_values[4].value),
            "bounce_rate": float(row.metric_values[5].value)
        }
    return {}

def fetch_device_category(client, start_date, end_date):
    """디바이스 카테고리별 통계"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="deviceCategory"),
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
            "device": row.dimension_values[0].value,
            "sessions": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
        })

    return results

def fetch_traffic_channels(client, start_date, end_date):
    """트래픽 채널별 통계"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="sessionDefaultChannelGroup"),
        ],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
            Metric(name="engagementRate"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        results.append({
            "channel": row.dimension_values[0].value,
            "sessions": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
            "engagement_rate": float(row.metric_values[2].value),
        })

    return sorted(results, key=lambda x: x['sessions'], reverse=True)

def fetch_monthly_trend(client, start_date, end_date):
    """월별 트래픽 추이"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="yearMonth"),
        ],
        metrics=[
            Metric(name="sessions"),
            Metric(name="totalUsers"),
            Metric(name="screenPageViews"),
        ],
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        year_month = row.dimension_values[0].value
        results.append({
            "month": f"{year_month[:4]}-{year_month[4:]}",
            "sessions": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
            "page_views": int(row.metric_values[2].value),
        })

    return sorted(results, key=lambda x: x['month'])

def fetch_top_pages(client, start_date, end_date, limit=20):
    """상위 페이지 조회"""
    request = RunReportRequest(
        property=GA4_PROPERTY_ID,
        date_ranges=[DateRange(start_date=start_date, end_date=end_date)],
        dimensions=[
            Dimension(name="pagePath"),
        ],
        metrics=[
            Metric(name="screenPageViews"),
            Metric(name="totalUsers"),
        ],
        limit=limit,
    )

    response = client.run_report(request)

    results = []
    for row in response.rows:
        results.append({
            "page_path": row.dimension_values[0].value,
            "page_views": int(row.metric_values[0].value),
            "users": int(row.metric_values[1].value),
        })

    return results

def collect_ga4_data():
    """회사소개서용 GA4 데이터 수집"""

    print("=" * 60)
    print("🎯 노피 회사소개서용 GA4 데이터 수집")
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

    data = {
        'metadata': {
            'collected_at': datetime.now().isoformat(),
            'purpose': '회사소개서 작성',
            'property_id': GA4_PROPERTY_ID,
            'start_date': start_str,
            'end_date': end_str,
            'version': '1.0'
        }
    }

    # 1. 전체 핵심 지표
    print("\n📊 1. 전체 핵심 지표 수집 중...")
    overall = fetch_overall_metrics(client, start_str, end_str)
    data['overall_metrics'] = overall

    print(f"   ✓ 총 세션: {overall.get('sessions', 0):,}회")
    print(f"   ✓ 총 사용자: {overall.get('total_users', 0):,}명")
    print(f"   ✓ 페이지뷰: {overall.get('page_views', 0):,}회")
    print(f"   ✓ 평균 세션 시간: {overall.get('avg_session_duration', 0):.1f}초")
    print(f"   ✓ 이탈률: {overall.get('bounce_rate', 0):.2%}")

    # 2. 디바이스 카테고리
    print("\n📱 2. 디바이스 카테고리 수집 중...")
    devices = fetch_device_category(client, start_str, end_str)
    data['device_category'] = devices

    total_sessions = sum(d['sessions'] for d in devices)
    for device in devices:
        ratio = (device['sessions'] / total_sessions * 100) if total_sessions > 0 else 0
        print(f"   ✓ {device['device']}: {device['sessions']:,}회 ({ratio:.1f}%)")

    # 3. 트래픽 채널
    print("\n🌐 3. 트래픽 채널 수집 중...")
    channels = fetch_traffic_channels(client, start_str, end_str)
    data['traffic_channels'] = channels

    for channel in channels[:5]:  # 상위 5개만 출력
        print(f"   ✓ {channel['channel']}: {channel['sessions']:,}회 (참여율: {channel['engagement_rate']:.2%})")

    # 4. 월별 추이
    print("\n📈 4. 월별 추이 수집 중...")
    monthly = fetch_monthly_trend(client, start_str, end_str)
    data['monthly_trend'] = monthly
    print(f"   ✓ {len(monthly)}개월 데이터 수집 완료")

    # 5. 상위 페이지
    print("\n📄 5. 상위 페이지 수집 중...")
    top_pages = fetch_top_pages(client, start_str, end_str, limit=10)
    data['top_pages'] = top_pages
    print(f"   ✓ 상위 {len(top_pages)}개 페이지 수집 완료")

    # 6. 일평균 지표 계산
    print("\n📊 6. 일평균 지표 계산 중...")
    days_count = (end_date - start_date).days
    data['daily_average'] = {
        'days_count': days_count,
        'avg_sessions': round(overall.get('sessions', 0) / days_count, 1) if days_count > 0 else 0,
        'avg_users': round(overall.get('total_users', 0) / days_count, 1) if days_count > 0 else 0,
        'avg_page_views': round(overall.get('page_views', 0) / days_count, 1) if days_count > 0 else 0,
    }
    print(f"   ✓ 일평균 세션: {data['daily_average']['avg_sessions']:,.1f}회")
    print(f"   ✓ 일평균 사용자: {data['daily_average']['avg_users']:,.1f}명")
    print(f"   ✓ 일평균 페이지뷰: {data['daily_average']['avg_page_views']:,.1f}회")

    return data

def save_data(data, output_dir):
    """데이터를 JSON 파일로 저장"""
    output_path = Path(output_dir) / f"ga4_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\n💾 데이터 저장 완료: {output_path}")
    return output_path

def main():
    """메인 실행 함수"""
    try:
        # 데이터 수집
        data = collect_ga4_data()

        # 데이터 저장
        script_dir = Path(__file__).parent
        data_dir = script_dir.parent / 'data'
        data_dir.mkdir(exist_ok=True)

        output_path = save_data(data, data_dir)

        print("\n" + "=" * 60)
        print("✅ GA4 데이터 수집 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
