"""
당근마켓 휴대폰 매장 크롤러 (Selenium 버전)
- Chrome 브라우저를 직접 제어
- 당근마켓 사이트에서 매장 검색
- 실시간 진행상황 출력
"""

import time
import re
import random
from datetime import datetime
from pathlib import Path
import pandas as pd
import gspread
from google.oauth2.service_account import Credentials
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException

class DaangnStoreCrawlerSelenium:
    """당근마켓 휴대폰 매장 크롤러 (Selenium)"""

    def __init__(self, google_api_key_path=None, headless=False):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)

        if google_api_key_path is None:
            self.google_api_key_path = Path('/Users/jacob/Desktop/dev/config/google_api_key.json')
        else:
            self.google_api_key_path = Path(google_api_key_path)

        # Chrome 옵션 설정 (봇 탐지 회피 강화)
        self.chrome_options = Options()
        if headless:
            self.chrome_options.add_argument('--headless')
        self.chrome_options.add_argument('--no-sandbox')
        self.chrome_options.add_argument('--disable-dev-shm-usage')
        self.chrome_options.add_argument('--disable-blink-features=AutomationControlled')

        # User-Agent 랜덤화
        user_agents = [
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15'
        ]
        self.chrome_options.add_argument(f'user-agent={random.choice(user_agents)}')

        # 추가 봇 탐지 회피 옵션
        self.chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        self.chrome_options.add_experimental_option('useAutomationExtension', False)
        self.chrome_options.add_argument('--disable-gpu')
        self.chrome_options.add_argument('--lang=ko-KR')

        self.driver = None

        # 전국 지역 (서울/수도권 세분화)
        self.regions = [
            # 서울 25개 구 (세분화)
            '서울 강남구', '서울 강동구', '서울 강북구', '서울 강서구',
            '서울 관악구', '서울 광진구', '서울 구로구', '서울 금천구',
            '서울 노원구', '서울 도봉구', '서울 동대문구', '서울 동작구',
            '서울 마포구', '서울 서대문구', '서울 서초구', '서울 성동구',
            '서울 성북구', '서울 송파구', '서울 양천구', '서울 영등포구',
            '서울 용산구', '서울 은평구', '서울 종로구', '서울 중구', '서울 중랑구',

            # 경기 남부 (서울 인접 지역)
            '경기 성남', '경기 수원', '경기 안양', '경기 안산', '경기 용인',
            '경기 광명', '경기 과천', '경기 의왕', '경기 군포', '경기 시흥',
            '경기 부천', '경기 광주', '경기 하남', '경기 화성', '경기 오산',

            # 경기 북부
            '경기 고양', '경기 파주', '경기 의정부', '경기 양주', '경기 동두천',
            '경기 남양주', '경기 구리', '경기 포천', '경기 연천', '경기 가평',

            # 경기 동부
            '경기 이천', '경기 여주', '경기 양평',

            # 경기 서부
            '경기 김포', '경기 인천', '인천 중구', '인천 동구', '인천 남구',
            '인천 연수구', '인천 남동구', '인천 부평구', '인천 계양구',
            '인천 서구', '인천 강화군', '인천 옹진군',

            # 기타 광역시
            '부산', '대구', '광주', '대전', '울산', '세종',

            # 기타 도
            '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주'
        ]

        # 검색 키워드 (노피 "동네성지" 컨셉 기반)
        self.keywords = [
            # 핵심: 동네 판매점 키워드 (노피의 본질)
            '휴대폰매장', '휴대폰성지', '스마트폰매장', '폰매장',
            '휴대폰가게', '핸드폰가게', '동네휴대폰매장',

            # 판매/개통 키워드 (O2O 신뢰 기반)
            '휴대폰판매', '휴대폰대리점', '핸드폰매장', '핸드폰판매',
            '스마트폰판매', '휴대폰개통', '기기변경', '번호이동',

            # 기기별 키워드 (주요 검색 수요)
            '아이폰', '갤럭시', '아이폰매장', '갤럭시매장',
            '아이폰판매', '갤럭시판매',

            # 신뢰/후기 키워드 (동네성지 핵심)
            '휴대폰매장추천', '믿을만한휴대폰매장', '안전한개통',
            '휴대폰성지후기', '휴대폰매장후기',
        ]

        self.results = []

    def init_driver(self):
        """Chrome 드라이버 초기화"""
        try:
            print("🌐 Chrome 브라우저 시작 중...")
            self.driver = webdriver.Chrome(options=self.chrome_options)
            self.driver.set_window_size(1920, 1080)

            # WebDriver 탐지 우회 스크립트 주입
            self.driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {
                'source': '''
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => undefined
                    });
                '''
            })

            print("✅ Chrome 브라우저 시작 완료 (봇 탐지 회피 활성화)")
            return True
        except Exception as e:
            print(f"❌ Chrome 드라이버 초기화 실패: {str(e)}")
            print("   chromedriver가 설치되어 있는지 확인하세요.")
            return False

    def close_driver(self):
        """Chrome 드라이버 종료"""
        if self.driver:
            self.driver.quit()
            print("🔴 Chrome 브라우저 종료")

    def search_daangn_stores(self, keyword, region=None):
        """당근마켓에서 매장 검색"""
        try:
            search_query = f"{region} {keyword}" if region else keyword
            print(f"  🔍 검색: {search_query}")

            # Google 검색
            google_url = f"https://www.google.com/search?q=당근마켓+{search_query}+site:daangn.com"
            self.driver.get(google_url)

            # 페이지 로드 대기 시간 랜덤화 (3-5초)
            wait_time = random.uniform(3, 5)
            time.sleep(wait_time)

            # 검색 결과에서 당근마켓 링크 수집
            daangn_links = []
            try:
                # 다양한 CSS 셀렉터로 시도
                selectors = [
                    'a[href*="daangn.com"]',
                    'div.g a',
                    'a[jsname]',
                    'a',
                ]

                all_links = []
                for selector in selectors:
                    elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elements:
                        url = elem.get_attribute('href')
                        if url and 'daangn.com' in url:
                            all_links.append(url)

                # 중복 제거
                all_links = list(set(all_links))

                print(f"    📌 총 {len(all_links)}개 daangn.com 링크 발견")

                # local-profile 링크만 필터링 (검색 결과 페이지 제외)
                for url in all_links:
                    if 'local-profile' in url or 'business-profile' in url:
                        # 검색 결과 페이지는 제외 (?in= 와 search= 가 있는 경우)
                        if '?in=' in url and 'search=' in url:
                            continue
                        if url not in daangn_links:
                            daangn_links.append(url)
                            print(f"    ✅ 프로필 링크 발견: {url[:80]}...")

                # 프로필 링크가 없으면 모든 당근마켓 링크 출력 (디버깅용)
                if not daangn_links and all_links:
                    print(f"    ℹ️  발견된 링크 샘플:")
                    for url in all_links[:5]:
                        print(f"      - {url[:100]}")

            except Exception as e:
                print(f"    ⚠️  검색 결과 파싱 실패: {str(e)}")
                import traceback
                traceback.print_exc()

            return daangn_links

        except Exception as e:
            print(f"    ❌ 검색 오류: {str(e)}")
            import traceback
            traceback.print_exc()
            return []

    def extract_store_info_from_page(self, url):
        """당근마켓 페이지에서 매장 정보 추출 (개별 매장 페이지만)"""
        try:
            # 검색 결과 페이지는 건너뛰기
            if '?in=' in url and 'search=' in url:
                print(f"    ⏭️  검색 결과 페이지 - 건너뜀")
                return None

            print(f"    📄 페이지 분석 중...")
            self.driver.get(url)
            time.sleep(3)

            # 개별 매장 페이지만 처리
            if False:  # 검색 결과 페이지 처리 비활성화
                # 검색 결과 페이지 - 여러 매장 리스트
                print(f"      📋 검색 결과 페이지 - 매장 리스트 추출")

                try:
                    # 매장 리스트 아이템 찾기 (XPath 사용)
                    store_items = self.driver.find_elements(By.XPATH, '//ul/li')

                    print(f"      📌 {len(store_items)}개 아이템 발견")

                    count = 0
                    for idx, item in enumerate(store_items[:20], 1):  # 상위 20개만
                        try:
                            # 링크 찾기
                            try:
                                link_elem = item.find_element(By.CSS_SELECTOR, 'a[href*="local-profile"]')
                                store_url = link_elem.get_attribute('href')
                            except:
                                continue

                            # 전체 텍스트 가져오기
                            item_text = item.text

                            # 디버깅: 첫 번째 아이템 출력
                            if idx == 1 and item_text:
                                print(f"      [디버그] 첫 번째 아이템 텍스트:\n{item_text[:200]}")

                            # 전화번호 먼저 확인
                            phone_pattern = r'010-?\d{3,4}-?\d{4}'
                            phones = re.findall(phone_pattern, item_text)

                            if not phones:
                                continue

                            # 매장명 추출
                            store_name = item_text.split('\n')[0].strip()

                            if not store_name or len(store_name) > 50:
                                continue

                            # 지역 추출
                            from urllib.parse import unquote
                            region = '지역 미확인'
                            if '?in=' in url:
                                region_param = url.split('?in=')[1].split('&')[0]
                                region = unquote(region_param)

                            if phones:
                                # 전화번호 정규화
                                normalized_phones = []
                                for phone in phones:
                                    digits = re.sub(r'[^0-9]', '', phone)
                                    if len(digits) == 10:
                                        formatted = f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
                                    elif len(digits) == 11:
                                        formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                                    else:
                                        formatted = phone
                                    normalized_phones.append(formatted)

                                unique_phones = list(set(normalized_phones))

                                results.append({
                                    'store_name': store_name,
                                    'phones': unique_phones,
                                    'region': region,
                                    'url': store_url
                                })
                                count += 1
                                print(f"        [{count}] {store_name}: {unique_phones[0]}")

                        except Exception as e:
                            continue

                    print(f"      ✅ 총 {count}개 매장 전화번호 추출")

                except Exception as e:
                    print(f"      ⚠️  리스트 추출 실패: {str(e)}")
                    import traceback
                    traceback.print_exc()

                return results if results else None

            else:
                # 개별 매장 페이지
                page_text = self.driver.find_element(By.TAG_NAME, 'body').text

                # 매장명 추출 - XPath 사용
                store_name = "매장명 미확인"
                try:
                    # XPath로 매장명 찾기
                    store_element = self.driver.find_element(By.XPATH, '/html/body/main/div[1]/div[2]/div[1]/h1')
                    store_name = store_element.text.strip()
                    print(f"    ✅ 매장명: {store_name}")
                except:
                    # 대체 방법: h1 태그에서 찾기
                    try:
                        title_elements = self.driver.find_elements(By.CSS_SELECTOR, 'h1')
                        if title_elements:
                            store_name = title_elements[0].text.strip()
                    except:
                        pass

                # URL에서도 시도
                if store_name == "매장명 미확인":
                    url_parts = url.split('local-profile/')
                    if len(url_parts) > 1:
                        store_part = url_parts[1].split('-')[0]
                        from urllib.parse import unquote
                        store_name = unquote(store_part)

                # 전화번호 추출
                phone_pattern = r'010-?\d{3,4}-?\d{4}'
                phones = re.findall(phone_pattern, page_text)

                # 정규화
                normalized_phones = []
                for phone in phones:
                    digits = re.sub(r'[^0-9]', '', phone)
                    if len(digits) == 10:
                        formatted = f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
                    elif len(digits) == 11:
                        formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                    else:
                        formatted = phone
                    normalized_phones.append(formatted)

                # 중복 제거
                unique_phones = list(set(normalized_phones))

                # 지역 추출 (상세 - 시/도 + 구/군)
                region = '지역 미확인'

                # URL에서 지역 추출 시도
                if '?in=' in url:
                    from urllib.parse import unquote
                    region_param = url.split('?in=')[1].split('&')[0]
                    region = unquote(region_param)

                # 페이지 텍스트에서 상세 지역 추출
                if region == '지역 미확인':
                    # "서울 강남구", "부산 해운대구" 같은 패턴 찾기
                    region_pattern = r'(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)\s*([가-힣]+[시군구])'
                    region_match = re.search(region_pattern, page_text)
                    if region_match:
                        region = f"{region_match.group(1)} {region_match.group(2)}"
                    else:
                        # 단순히 시/도만 찾기
                        for r in self.regions:
                            if r in page_text:
                                region = r
                                break

                if unique_phones:
                    print(f"    ✅ 매장: {store_name}, 전화번호: {len(unique_phones)}개, 지역: {region}")
                    return [{
                        'store_name': store_name,
                        'phones': unique_phones,
                        'region': region,
                        'url': url
                    }]
                else:
                    print(f"    ⚠️  전화번호 없음")
                    return None

        except Exception as e:
            print(f"    ❌ 페이지 분석 실패: {str(e)}")
            import traceback
            traceback.print_exc()
            return None

    def save_intermediate_results(self, results, search_count):
        """중간 결과 저장"""
        if not results:
            return

        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = self.output_path / f'daangn_stores_intermediate_{search_count}searches_{timestamp}.csv'

            df = pd.DataFrame(results)
            df.to_csv(filename, index=False, encoding='utf-8-sig')
            print(f"    💾 중간 저장 완료: {len(results)}개 매장 → {filename.name}")
        except Exception as e:
            print(f"    ⚠️  중간 저장 실패: {str(e)}")

    def crawl(self, max_searches=30, save_interval=50):
        """크롤링 실행 (자동 재시작 및 중간 저장 기능 포함)"""
        print("=" * 80)
        print("🥕 당근마켓 휴대폰 매장 크롤러 (Selenium)")
        print("=" * 80)

        all_results = []
        visited_urls = set()
        search_count = 0
        retry_count = 0
        max_retries = 3

        # 지역별 키워드 조합
        for region in self.regions:
            if search_count >= max_searches:
                break

            for keyword in self.keywords:
                if search_count >= max_searches:
                    break

                print(f"\n[{search_count + 1}/{max_searches}] 🔍 {region} {keyword}")

                # 자동 재시작 로직
                while retry_count < max_retries:
                    try:
                        # 드라이버가 없거나 연결이 끊어진 경우 재초기화
                        if self.driver is None:
                            print("    🔄 드라이버 재초기화 중...")
                            if not self.init_driver():
                                retry_count += 1
                                print(f"    ⚠️  재시도 {retry_count}/{max_retries}")
                                time.sleep(5)
                                continue
                            retry_count = 0  # 성공시 리셋

                        # 검색
                        daangn_links = self.search_daangn_stores(keyword, region)

                        if not daangn_links:
                            print(f"    ⚠️  링크 없음")
                            search_count += 1
                            break

                        print(f"    📍 {len(daangn_links)}개 링크 발견")

                        # 각 링크 방문하여 정보 추출
                        for link in daangn_links:
                            if link in visited_urls:
                                continue

                            visited_urls.add(link)
                            store_info_list = self.extract_store_info_from_page(link)

                            # 결과가 리스트로 반환됨 (검색 결과 페이지인 경우 여러 매장)
                            if store_info_list:
                                for store_info in store_info_list:
                                    if store_info.get('phones'):
                                        for phone in store_info['phones']:
                                            result = {
                                                '지역명': store_info['region'],
                                                '매장명': store_info['store_name'],
                                                '지역명_매장명': f"{store_info['region']}_{store_info['store_name']}",
                                                '전화번호': phone,
                                                '링크': store_info['url']
                                            }
                                            all_results.append(result)
                                            print(f"      💾 저장: {result['매장명']} ({phone})")

                            time.sleep(5)  # 요청 간격 (2초 → 5초)

                        search_count += 1

                        # 중간 저장 (일정 개수마다)
                        if search_count % save_interval == 0:
                            print(f"\n📦 중간 저장 시점 ({search_count}번 검색 완료)")
                            self.save_intermediate_results(all_results, search_count)

                        # 검색 간격을 10-15초로 랜덤하게 증가 (Google CAPTCHA 회피)
                        wait_time = random.uniform(10, 15)
                        print(f"    ⏳ {wait_time:.1f}초 대기 중... (Google 봇 탐지 회피)")
                        time.sleep(wait_time)

                        # 성공적으로 완료되면 루프 탈출
                        break

                    except Exception as e:
                        # 드라이버 연결 오류 발생시 재시도
                        print(f"    ❌ 오류 발생: {str(e)}")
                        self.close_driver()
                        self.driver = None
                        retry_count += 1
                        if retry_count >= max_retries:
                            print(f"    ❌ 최대 재시도 횟수 초과. 다음 검색으로 이동")
                            search_count += 1
                            break
                        print(f"    🔄 재시도 {retry_count}/{max_retries}...")
                        time.sleep(5)

                # 재시도 카운터 리셋
                retry_count = 0

        self.results = all_results
        print(f"\n✅ 크롤링 완료! 총 {len(all_results)}개 매장 정보 수집")
        print(f"   고유 URL: {len(visited_urls)}개")

        # 최종 결과 저장
        if all_results:
            print(f"\n📦 최종 결과 저장 중...")
            self.save_intermediate_results(all_results, search_count)

        self.close_driver()
        return all_results

    def save_to_csv(self, filename=None):
        """CSV 저장"""
        if not self.results:
            print("⚠️  저장할 데이터가 없습니다.")
            return None

        if filename is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f'daangn_stores_selenium_{timestamp}.csv'

        output_file = self.output_path / filename

        df = pd.DataFrame(self.results)

        # 중복 제거
        df = df.drop_duplicates(subset=['전화번호', '링크'], keep='first')

        # 정렬
        df = df.sort_values(by=['지역명', '매장명'])

        df.to_csv(output_file, index=False, encoding='utf-8-sig')

        print(f"💾 CSV 저장: {output_file}")
        print(f"   총 {len(df)}개 (중복 제거 후)")

        return output_file

    def upload_to_sheets(self, spreadsheet_url, worksheet_name='당근매장_Selenium'):
        """Google Sheets 업로드"""
        if not self.results:
            print("⚠️  업로드할 데이터가 없습니다.")
            return False

        try:
            print("\n📤 Google Sheets 업로드...")

            scopes = [
                'https://www.googleapis.com/auth/spreadsheets',
                'https://www.googleapis.com/auth/drive'
            ]

            creds = Credentials.from_service_account_file(
                str(self.google_api_key_path),
                scopes=scopes
            )

            client = gspread.authorize(creds)

            sheet_id = spreadsheet_url.split('/d/')[1].split('/')[0]
            spreadsheet = client.open_by_key(sheet_id)

            try:
                worksheet = spreadsheet.worksheet(worksheet_name)
            except:
                worksheet = spreadsheet.add_worksheet(title=worksheet_name, rows=3000, cols=10)
                print(f"  ✅ 워크시트 '{worksheet_name}' 생성")

            df = pd.DataFrame(self.results)
            df = df.drop_duplicates(subset=['전화번호', '링크'], keep='first')

            print(f"  📊 업로드: {len(df)}개")

            # 기존 데이터 확인
            existing_data = worksheet.get_all_values()

            # 헤더 포함 작성
            if len(existing_data) == 0:
                all_data = [df.columns.tolist()] + df.values.tolist()
                batch_size = 100
                worksheet.update('A1', all_data[:batch_size], value_input_option='RAW')
                print(f"  ✅ {min(len(all_data), batch_size)}개 행 작성")

                # 나머지
                remaining = all_data[batch_size:]
                if remaining:
                    time.sleep(60)
                    for i in range(0, len(remaining), batch_size):
                        batch = remaining[i:i+batch_size]
                        worksheet.append_rows(batch, value_input_option='RAW')
                        print(f"  ✅ {len(batch)}개 행 추가")
                        time.sleep(60)
            else:
                # 추가
                new_rows = df.values.tolist()
                batch_size = 100
                for i in range(0, len(new_rows), batch_size):
                    batch = new_rows[i:i+batch_size]
                    worksheet.append_rows(batch, value_input_option='RAW')
                    print(f"  ✅ {len(batch)}개 행 추가")
                    if i + batch_size < len(new_rows):
                        time.sleep(60)

            print(f"\n📊 완료: {spreadsheet_url}")
            return True

        except Exception as e:
            print(f"❌ 오류: {str(e)}")
            import traceback
            traceback.print_exc()
            return False

    def print_summary(self):
        """결과 요약"""
        if not self.results:
            print("⚠️  수집된 데이터가 없습니다.")
            return

        df = pd.DataFrame(self.results)
        df_unique = df.drop_duplicates(subset=['전화번호'])

        print("\n" + "=" * 80)
        print("📊 수집 결과 요약")
        print("=" * 80)
        print(f"총 수집: {len(df)}개")
        print(f"고유 전화번호: {len(df_unique)}개")
        print(f"고유 지역: {df['지역명'].nunique()}개")

        print("\n📍 지역별 매장 수:")
        region_counts = df_unique['지역명'].value_counts().head(15)
        for region, count in region_counts.items():
            print(f"   {region}: {count}개")

        print("\n📌 샘플 데이터:")
        print(df_unique.head(10).to_string(index=False, max_colwidth=40))


def main():
    """메인 실행"""

    SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/1IDbMaZucrE78gYPK_dhFGFWN_oixcRhlM1sU9tZMJRo/edit?gid=1073623102#gid=1073623102"

    # Headless 모드 활성화 (브라우저 창 숨김, 안정성 향상)
    crawler = DaangnStoreCrawlerSelenium(headless=True)

    # 크롤링 설정
    # max_searches 값 조정:
    # - 테스트: max_searches=10
    # - 소규모: max_searches=50 (서울 일부 구 + 주요 키워드)
    # - 중규모: max_searches=200 (서울 전체 + 경기 일부)
    # - 대규모: max_searches=500 (서울 + 수도권 전체)
    # - 전체: max_searches=2000 (모든 지역 × 모든 키워드)

    # save_interval: 중간 저장 간격 (기본 50개마다)

    print("\n🎯 서울/수도권 집중 크롤링 모드")
    print(f"📍 총 지역 수: {len(crawler.regions)}개")
    print(f"🔑 총 키워드 수: {len(crawler.keywords)}개")
    print(f"📊 최대 검색 조합: {len(crawler.regions) * len(crawler.keywords)}개")
    print("=" * 80)

    # 크롤링 실행 (전체 지역 × 전체 키워드)
    # 2000개 검색 = 79개 지역 × 26개 키워드 = 2,054개 조합
    # save_interval=50: 50개 검색마다 중간 저장
    results = crawler.crawl(max_searches=2000, save_interval=50)

    # 요약
    crawler.print_summary()

    # CSV 저장
    crawler.save_to_csv()

    # Google Sheets 업로드 (daangn 시트에 업로드)
    if results:
        print("\n⏳ Google Sheets 업로드 중...")
        crawler.upload_to_sheets(SPREADSHEET_URL, worksheet_name='daangn')

    print("\n" + "=" * 80)
    print("✅ 완료!")
    print("=" * 80)


if __name__ == "__main__":
    main()
