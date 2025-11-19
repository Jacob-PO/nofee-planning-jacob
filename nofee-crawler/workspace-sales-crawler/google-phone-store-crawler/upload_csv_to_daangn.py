"""
기존 CSV 파일을 daangn 시트에 업로드하는 스크립트
"""

import pandas as pd
import gspread
from google.oauth2 import service_account
from pathlib import Path

# Google Sheets 정보
SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/1IDbMaZucrE78gYPK_dhFGFWN_oixcRhlM1sU9tZMJRo/edit?gid=1073623102#gid=1073623102"
WORKSHEET_NAME = 'daangn'

# CSV 파일 경로
CSV_FILE = Path(__file__).parent / 'output' / 'daangn_stores_selenium_20251104_103628.csv'

# API 키 경로
API_KEY_FILE = Path('/Users/jacob/Desktop/dev/config/google_api_key.json')

def upload_csv_to_sheets():
    """CSV 파일을 Google Sheets에 업로드"""

    print("=" * 60)
    print("CSV → daangn 시트 업로드")
    print("=" * 60)

    # CSV 읽기
    print(f"\n📄 CSV 파일 읽는 중: {CSV_FILE.name}")
    df = pd.read_csv(CSV_FILE)
    print(f"✓ {len(df)}개 행 읽음")

    # Google Sheets API 인증
    print("\n🔐 Google Sheets API 인증 중...")
    scope = [
        'https://spreadsheets.google.com/feeds',
        'https://www.googleapis.com/auth/spreadsheets',
        'https://www.googleapis.com/auth/drive'
    ]

    creds = service_account.Credentials.from_service_account_file(
        str(API_KEY_FILE), scopes=scope)
    client = gspread.authorize(creds)
    print("✓ 인증 완료")

    # 스프레드시트 열기
    print(f"\n📊 스프레드시트 열기...")
    spreadsheet = client.open_by_url(SPREADSHEET_URL)

    # 워크시트 가져오기 또는 생성
    try:
        worksheet = spreadsheet.worksheet(WORKSHEET_NAME)
        print(f"✓ '{WORKSHEET_NAME}' 워크시트 찾음")

        # 기존 데이터 확인
        existing_data = worksheet.get_all_values()
        if len(existing_data) > 1:  # 헤더 제외
            print(f"⚠️  기존 데이터: {len(existing_data)-1}개 행")
            print("   기존 데이터에 추가합니다...")

            # 기존 데이터를 DataFrame으로 변환
            existing_df = pd.DataFrame(existing_data[1:], columns=existing_data[0])

            # 새 데이터와 병합 (중복 제거)
            combined_df = pd.concat([existing_df, df], ignore_index=True)

            # 지역명_매장명 + 전화번호 기준으로 중복 제거
            if '지역명_매장명' in combined_df.columns and '전화번호' in combined_df.columns:
                before_dedup = len(combined_df)
                combined_df = combined_df.drop_duplicates(subset=['지역명_매장명', '전화번호'], keep='first')
                removed = before_dedup - len(combined_df)
                print(f"   - 중복 {removed}개 제거")

            df = combined_df
            print(f"   - 최종 {len(df)}개 행")
    except gspread.WorksheetNotFound:
        worksheet = spreadsheet.add_worksheet(
            title=WORKSHEET_NAME,
            rows=10000,
            cols=20
        )
        print(f"✓ '{WORKSHEET_NAME}' 워크시트 생성")

    # 데이터 업로드
    print(f"\n📤 '{WORKSHEET_NAME}' 시트에 업로드 중...")

    # 시트 초기화
    worksheet.clear()

    # NaN 값을 빈 문자열로 변환
    df = df.fillna('')

    # 헤더 + 데이터 준비
    headers = df.columns.tolist()
    data_rows = df.astype(str).values.tolist()  # 모든 값을 문자열로 변환
    all_data = [headers] + data_rows

    # 업로드
    worksheet.update(values=all_data, range_name='A1')

    print(f"\n✅ 업로드 완료!")
    print(f"   - 총 {len(df)}개 행 업로드")
    print(f"\n확인: {SPREADSHEET_URL}")
    print("=" * 60)

if __name__ == "__main__":
    upload_csv_to_sheets()
