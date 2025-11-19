"""
네이버 플레이스 휴대폰 매장 크롤러 (Selenium 버전)
- Chrome 브라우저를 직접 제어
- 네이버 지도/플레이스에서 매장 검색
- 010 전화번호만 필터링하여 수집
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
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException

class NaverPlaceCrawler:
    """네이버 플레이스 휴대폰 매장 크롤러"""

    def __init__(self, google_api_key_path=None, headless=False):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)

        if google_api_key_path is None:
            self.google_api_key_path = Path('/Users/jacob/Desktop/dev/config/google_api_key.json')
        else:
            self.google_api_key_path = Path(google_api_key_path)

        # Chrome 옵션 설정 (봇 탐지 회피)
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
        self.chrome_options.add_argument('--window-size=1920,1080')

        self.driver = None

        # 서울/수도권 지역 (79개 지역)
        self.regions = [
            # 서울 25개 구
            '서울 강남구', '서울 강동구', '서울 강북구', '서울 강서구',
            '서울 관악구', '서울 광진구', '서울 구로구', '서울 금천구',
            '서울 노원구', '서울 도봉구', '서울 동대문구', '서울 동작구',
            '서울 마포구', '서울 서대문구', '서울 서초구', '서울 성동구',
            '서울 성북구', '서울 송파구', '서울 양천구', '서울 영등포구',
            '서울 용산구', '서울 은평구', '서울 종로구', '서울 중구', '서울 중랑구',

            # 경기 남부
            '경기 성남', '경기 수원', '경기 안양', '경기 안산', '경기 용인',
            '경기 광명', '경기 과천', '경기 의왕', '경기 군포', '경기 시흥',
            '경기 부천', '경기 광주', '경기 하남', '경기 화성', '경기 오산',

            # 경기 북부
            '경기 고양', '경기 파주', '경기 의정부', '경기 양주', '경기 동두천',
            '경기 남양주', '경기 구리', '경기 포천', '경기 연천', '경기 가평',

            # 경기 동부
            '경기 이천', '경기 여주', '경기 양평',

            # 경기 서부 + 인천
            '경기 김포', '인천 중구', '인천 동구', '인천 남구',
            '인천 연수구', '인천 남동구', '인천 부평구', '인천 계양구',
            '인천 서구', '인천 강화군', '인천 옹진군',
        ]

        # 검색 키워드 (노피 동네성지 컨셉 기반)
        self.keywords = [
            # 핵심: 동네 판매점 키워드
            '휴대폰매장', '휴대폰성지', '스마트폰매장', '폰매장',
            '휴대폰가게', '핸드폰가게', '동네휴대폰매장',

            # 판매/개통 키워드
            '휴대폰판매', '휴대폰대리점', '핸드폰매장', '핸드폰판매',
            '스마트폰판매', '휴대폰개통', '기기변경', '번호이동',

            # 기기별 키워드
            '아이폰', '갤럭시', '아이폰매장', '갤럭시매장',
            '아이폰판매', '갤럭시판매',

            # 신뢰/후기 키워드
            '휴대폰매장추천', '믿을만한휴대폰매장', '안전한개통',
            '휴대폰성지후기', '휴대폰매장후기',
        ]

        # 수집된 매장 데이터
        self.collected_stores = set()  # 중복 제거용

    def init_driver(self):
        """Chrome 드라이버 초기화"""
        try:
            self.driver = webdriver.Chrome(options=self.chrome_options)

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
            return False

    def close_driver(self):
        """Chrome 드라이버 종료"""
        if self.driver:
            try:
                self.driver.quit()
                self.driver = None
            except:
                pass

    def extract_010_phones(self, text):
        """텍스트에서 010 전화번호만 추출"""
        if not text:
            return []

        # 010으로 시작하는 전화번호 패턴
        pattern = r'010[-\s]?\d{3,4}[-\s]?\d{4}'
        phones = re.findall(pattern, text)

        # 하이픈 통일 (010-XXXX-XXXX 형식으로)
        normalized = []
        for phone in phones:
            # 공백과 하이픈 제거 후 재구성
            digits = re.sub(r'[-\s]', '', phone)
            if len(digits) == 11:  # 010XXXXXXXX
                formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                normalized.append(formatted)
            elif len(digits) == 10:  # 010XXXXXXX
                formatted = f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
                normalized.append(formatted)

        return list(set(normalized))  # 중복 제거

    def search_naver_place(self, region, keyword):
        """네이버 플레이스에서 검색"""
        query = f"{region} {keyword}"
        # 네이버 지도 검색 URL 사용
        search_url = f"https://map.naver.com/p/search/{query}"

        try:
            self.driver.get(search_url)
            time.sleep(random.uniform(3, 5))  # 페이지 로딩 대기

            stores = []

            try:
                # iframe으로 전환
                self.driver.switch_to.frame('searchIframe')
                time.sleep(1)

                # 검색 결과 리스트 (스크롤 가능 영역)
                scroll_div = self.driver.find_element(By.CSS_SELECTOR, "#_pcmap_list_scroll_container")

                # 스크롤하여 더 많은 결과 로드
                for _ in range(2):
                    self.driver.execute_script("arguments[0].scrollTop = arguments[0].scrollHeight", scroll_div)
                    time.sleep(1)

                # 매장 리스트 아이템
                items = self.driver.find_elements(By.CSS_SELECTOR, "li.UEzoS")

                print(f"    📌 {len(items)}개 검색 결과 발견")

                for item in items[:20]:  # 상위 20개만
                    try:
                        # 매장명
                        name_elem = item.find_element(By.CSS_SELECTOR, "span.TYaxT")
                        store_name = name_elem.text.strip()

                        # 링크
                        link_elem = item.find_element(By.CSS_SELECTOR, "a.tzwk0")
                        place_id = link_elem.get_attribute('href')

                        if store_name and place_id:
                            stores.append({
                                'name': store_name,
                                'link': f"https://map.naver.com{place_id}",
                                'region': region
                            })

                    except Exception as e:
                        continue

                # iframe에서 나오기
                self.driver.switch_to.default_content()

            except Exception as e:
                print(f"    ⚠️  검색 결과 파싱 실패: {str(e)}")
                # iframe에서 나오기
                try:
                    self.driver.switch_to.default_content()
                except:
                    pass

            return stores

        except Exception as e:
            print(f"    ❌ 검색 실패: {str(e)}")
            return []

    def get_store_detail(self, store):
        """매장 상세 정보 가져오기 (전화번호 수집)"""
        try:
            self.driver.get(store['link'])
            time.sleep(random.uniform(2, 3))

            # entryIframe으로 전환
            try:
                self.driver.switch_to.frame('entryIframe')
                time.sleep(1)
            except:
                pass

            # 페이지 전체 텍스트에서 010 번호 찾기
            page_text = self.driver.find_element(By.TAG_NAME, 'body').text
            phones = self.extract_010_phones(page_text)

            # iframe에서 나오기
            try:
                self.driver.switch_to.default_content()
            except:
                pass

            if phones:
                return {
                    'region': store['region'],
                    'name': store['name'],
                    'phones': phones,
                    'link': store['link']
                }

            return None

        except Exception as e:
            try:
                self.driver.switch_to.default_content()
            except:
                pass
            return None

    def save_intermediate_results(self, results, search_count):
        """중간 결과 저장"""
        if not results:
            return

        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = self.output_path / f'naver_stores_intermediate_{search_count}searches_{timestamp}.csv'

            df = pd.DataFrame(results)
            df.to_csv(filename, index=False, encoding='utf-8-sig')
            print(f"    💾 중간 저장 완료: {len(results)}개 매장 → {filename.name}")
        except Exception as e:
            print(f"    ⚠️  중간 저장 실패: {str(e)}")

    def crawl(self, max_searches=2000, save_interval=50):
        """네이버 플레이스 크롤링 실행"""
        print("=" * 80)
        print("🗺️  네이버 플레이스 휴대폰 매장 크롤러")
        print("=" * 80)
        print(f"📍 총 지역 수: {len(self.regions)}개")
        print(f"🔑 총 키워드 수: {len(self.keywords)}개")
        print(f"📊 최대 검색 조합: {len(self.regions) * len(self.keywords)}개")
        print("=" * 80)

        all_results = []
        search_count = 0
        max_retries = 3
        retry_count = 0

        for region in self.regions:
            for keyword in self.keywords:
                search_count += 1

                if search_count > max_searches:
                    print("\n✅ 최대 검색 횟수 도달")
                    break

                print(f"\n[{search_count}/{max_searches}] 🔍 {region} {keyword}")

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
                            retry_count = 0

                        # 네이버 플레이스 검색
                        stores = self.search_naver_place(region, keyword)

                        # 각 매장 상세정보에서 010 전화번호 수집
                        for store in stores:
                            detail = self.get_store_detail(store)
                            if detail:
                                for phone in detail['phones']:
                                    store_key = f"{detail['name']}_{phone}"
                                    if store_key not in self.collected_stores:
                                        self.collected_stores.add(store_key)
                                        all_results.append({
                                            '지역명': detail['region'],
                                            '매장명': detail['name'],
                                            '전화번호': phone,
                                            '링크': detail['link']
                                        })
                                        print(f"      💾 저장: {detail['name']} ({phone})")

                            time.sleep(random.uniform(1, 2))  # 매장 간 대기

                        # 중간 저장
                        if search_count % save_interval == 0:
                            print(f"\n📦 중간 저장 시점 ({search_count}번 검색 완료)")
                            self.save_intermediate_results(all_results, search_count)

                        # 검색 간격 (봇 탐지 회피)
                        wait_time = random.uniform(3, 5)
                        print(f"    ⏳ {wait_time:.1f}초 대기 중...")
                        time.sleep(wait_time)

                        break  # 성공하면 재시도 루프 탈출

                    except Exception as e:
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

                retry_count = 0  # 다음 검색을 위해 리셋

            if search_count > max_searches:
                break

        # 최종 저장
        print("\n" + "=" * 80)
        print("✅ 최종 크롤링 완료")
        print("=" * 80)
        print(f"📊 총 검색 횟수: {search_count}회")
        print(f"💾 수집된 매장: {len(all_results)}개")

        if all_results:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

            # 최종 CSV 저장
            df = pd.DataFrame(all_results)
            final_filename = self.output_path / f'naver_stores_{timestamp}.csv'
            df.to_csv(final_filename, index=False, encoding='utf-8-sig')
            print(f"💾 최종 파일 저장: {final_filename}")

            # 중간 저장도 함께
            self.save_intermediate_results(all_results, search_count)

        self.close_driver()
        return all_results

def main():
    """메인 실행 함수"""
    print("🗺️  네이버 플레이스 크롤링 시작...")

    # Headless 모드 활성화 (안정성)
    crawler = NaverPlaceCrawler(headless=True)

    # 크롤링 실행 (2000번 검색, 50개마다 중간 저장)
    results = crawler.crawl(max_searches=2000, save_interval=50)

    print(f"\n🎉 크롤링 완료! 총 {len(results)}개 매장 수집")

if __name__ == "__main__":
    main()
