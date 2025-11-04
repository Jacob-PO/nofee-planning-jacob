"""
복수 플랫폼 데이터 통합 스크립트
- 당근마켓, 네이버 플레이스, 카카오맵, 구글 마이비즈니스 데이터 통합
- 중복 제거 (매장명 + 전화번호 기준)
- 최종 통합 CSV 생성 및 구글 시트 업로드
"""

import pandas as pd
from pathlib import Path
from datetime import datetime
import gspread
from google.oauth2.service_account import Credentials

class PlatformMerger:
    """플랫폼 데이터 통합기"""

    def __init__(self, google_api_key_path=None):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'merged_output'
        self.output_path.mkdir(exist_ok=True)

        if google_api_key_path is None:
            self.google_api_key_path = Path('/Users/jacob/Desktop/dev/config/google_api_key.json')
        else:
            self.google_api_key_path = Path(google_api_key_path)

        # 각 플랫폼 크롤러의 output 폴더 경로
        self.platform_paths = {
            'daangn': self.base_path / 'google-phone-store-crawler' / 'output',
            'naver': self.base_path / 'naver-place-crawler' / 'output',
            'kakao': self.base_path / 'kakao-map-crawler' / 'output',
            'google': self.base_path / 'google-mybusiness-crawler' / 'output',
        }

    def load_platform_data(self, platform_name, file_pattern):
        """특정 플랫폼의 최신 데이터 로드"""
        platform_path = self.platform_paths.get(platform_name)

        if not platform_path or not platform_path.exists():
            print(f"⚠️  {platform_name} 폴더가 존재하지 않습니다: {platform_path}")
            return pd.DataFrame()

        # 최신 CSV 파일 찾기
        csv_files = list(platform_path.glob(file_pattern))

        if not csv_files:
            print(f"⚠️  {platform_name}에서 CSV 파일을 찾을 수 없습니다")
            return pd.DataFrame()

        # 가장 최신 파일 선택
        latest_file = max(csv_files, key=lambda x: x.stat().st_mtime)
        print(f"📂 {platform_name}: {latest_file.name} 로드 중...")

        try:
            df = pd.read_csv(latest_file, encoding='utf-8-sig')
            print(f"   ✅ {len(df)}개 행 로드 완료")
            return df
        except Exception as e:
            print(f"   ❌ 로드 실패: {str(e)}")
            return pd.DataFrame()

    def standardize_columns(self, df, platform_name):
        """컬럼명 표준화"""
        # 모든 플랫폼을 동일한 컬럼명으로 통일
        column_mapping = {
            '지역명': '지역명',
            '매장명': '매장명',
            '전화번호': '전화번호',
            '링크': '링크',
        }

        # 불필요한 컬럼 제거
        if '지역명_매장명' in df.columns:
            df = df.drop(columns=['지역명_매장명'])

        # 플랫폼 출처 컬럼 추가
        df['플랫폼'] = platform_name

        return df

    def remove_duplicates(self, merged_df):
        """중복 제거 (매장명 + 전화번호 기준)"""
        print("\n🔍 중복 제거 중...")

        before_count = len(merged_df)

        # 매장명 + 전화번호 조합으로 중복 제거
        merged_df['unique_key'] = merged_df['매장명'] + '_' + merged_df['전화번호']
        merged_df = merged_df.drop_duplicates(subset=['unique_key'], keep='first')
        merged_df = merged_df.drop(columns=['unique_key'])

        after_count = len(merged_df)
        removed_count = before_count - after_count

        print(f"   제거 전: {before_count}개")
        print(f"   제거 후: {after_count}개")
        print(f"   제거됨: {removed_count}개 중복")

        return merged_df

    def merge_all_platforms(self):
        """모든 플랫폼 데이터 통합"""
        print("=" * 80)
        print("📊 복수 플랫폼 데이터 통합 시작")
        print("=" * 80)

        all_dataframes = []

        # 1. 당근마켓 데이터 로드
        daangn_df = self.load_platform_data('daangn', 'daangn_stores_*.csv')
        if not daangn_df.empty:
            daangn_df = self.standardize_columns(daangn_df, '당근마켓')
            all_dataframes.append(daangn_df)

        # 2. 네이버 플레이스 데이터 로드
        naver_df = self.load_platform_data('naver', 'naver_stores_*.csv')
        if not naver_df.empty:
            naver_df = self.standardize_columns(naver_df, '네이버플레이스')
            all_dataframes.append(naver_df)

        # 3. 카카오맵 데이터 로드
        kakao_df = self.load_platform_data('kakao', 'kakao_stores_*.csv')
        if not kakao_df.empty:
            kakao_df = self.standardize_columns(kakao_df, '카카오맵')
            all_dataframes.append(kakao_df)

        # 4. 구글 마이비즈니스 데이터 로드
        google_df = self.load_platform_data('google', 'google_stores_*.csv')
        if not google_df.empty:
            google_df = self.standardize_columns(google_df, '구글마이비즈니스')
            all_dataframes.append(google_df)

        if not all_dataframes:
            print("\n❌ 통합할 데이터가 없습니다!")
            return None

        # 데이터 통합
        print("\n📦 데이터 통합 중...")
        merged_df = pd.concat(all_dataframes, ignore_index=True)
        print(f"   총 {len(merged_df)}개 행 (중복 포함)")

        # 중복 제거
        merged_df = self.remove_duplicates(merged_df)

        # 플랫폼별 통계
        print("\n📈 플랫폼별 통계:")
        platform_stats = merged_df['플랫폼'].value_counts()
        for platform, count in platform_stats.items():
            print(f"   {platform}: {count}개")

        return merged_df

    def save_merged_data(self, merged_df):
        """통합 데이터 저장"""
        if merged_df is None or merged_df.empty:
            print("\n❌ 저장할 데이터가 없습니다")
            return None

        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = self.output_path / f'merged_all_platforms_{timestamp}.csv'

        # CSV 저장
        merged_df.to_csv(filename, index=False, encoding='utf-8-sig')
        print(f"\n💾 통합 CSV 저장 완료: {filename}")

        return filename

    def upload_to_google_sheets(self, merged_df, worksheet_name='merged'):
        """구글 시트에 업로드"""
        if merged_df is None or merged_df.empty:
            print("\n❌ 업로드할 데이터가 없습니다")
            return False

        try:
            print(f"\n📤 구글 시트 '{worksheet_name}'에 업로드 중...")

            # Google Sheets API 인증
            scope = [
                'https://spreadsheets.google.com/feeds',
                'https://www.googleapis.com/auth/drive'
            ]
            creds = Credentials.from_service_account_file(
                str(self.google_api_key_path),
                scopes=scope
            )
            client = gspread.authorize(creds)

            # 스프레드시트 열기
            spreadsheet_url = 'https://docs.google.com/spreadsheets/d/1_kRQWg7yvwGP8uXGkrXLzL82bNLLVN4S-VYkvI-cJMw/edit?gid=0#gid=0'
            spreadsheet = client.open_by_url(spreadsheet_url)

            # 워크시트 선택 또는 생성
            try:
                worksheet = spreadsheet.worksheet(worksheet_name)
                worksheet.clear()  # 기존 데이터 삭제
            except:
                worksheet = spreadsheet.add_worksheet(
                    title=worksheet_name,
                    rows=len(merged_df) + 1,
                    cols=len(merged_df.columns)
                )

            # NaN 값 처리
            merged_df = merged_df.fillna('')

            # 헤더 + 데이터 준비
            headers = merged_df.columns.tolist()
            data_rows = merged_df.astype(str).values.tolist()
            all_data = [headers] + data_rows

            # 업로드
            worksheet.update(values=all_data, range_name='A1')

            print(f"   ✅ {len(merged_df)}개 행 업로드 완료!")
            print(f"   🔗 {spreadsheet_url}")

            return True

        except Exception as e:
            print(f"   ❌ 업로드 실패: {str(e)}")
            return False

def main():
    """메인 실행 함수"""
    print("🚀 복수 플랫폼 데이터 통합 시작...\n")

    merger = PlatformMerger()

    # 1. 모든 플랫폼 데이터 통합
    merged_df = merger.merge_all_platforms()

    if merged_df is not None and not merged_df.empty:
        # 2. CSV 파일로 저장
        csv_file = merger.save_merged_data(merged_df)

        # 3. 구글 시트에 업로드
        merger.upload_to_google_sheets(merged_df, worksheet_name='merged')

        print("\n" + "=" * 80)
        print(f"🎉 통합 완료! 최종 {len(merged_df)}개 매장 수집")
        print("=" * 80)
    else:
        print("\n❌ 통합할 데이터가 없습니다")

if __name__ == "__main__":
    main()
