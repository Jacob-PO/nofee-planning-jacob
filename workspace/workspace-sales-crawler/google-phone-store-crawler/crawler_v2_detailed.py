"""
당근마켓 휴대폰 매장 크롤러 V2 (세분화 버전)
- 구 단위 + 역 단위 + 동 단위 검색
- 더 많은 010 전화번호 수집
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

class DaangnStoreCrawlerV2:
    """당근마켓 휴대폰 매장 크롤러 V2 (세분화)"""

    def __init__(self, google_api_key_path=None, headless=False):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)

        if google_api_key_path is None:
            self.google_api_key_path = Path('/Users/jacob/Desktop/dev/config/google_api_key.json')
        else:
            self.google_api_key_path = Path(google_api_key_path)

        # Chrome 옵션 설정
        self.chrome_options = Options()
        if headless:
            self.chrome_options.add_argument('--headless')
        self.chrome_options.add_argument('--no-sandbox')
        self.chrome_options.add_argument('--disable-dev-shm-usage')
        self.chrome_options.add_argument('--disable-blink-features=AutomationControlled')

        # User-Agent 랜덤화
        user_agents = [
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        ]
        self.chrome_options.add_argument(f'user-agent={random.choice(user_agents)}')
        self.chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        self.chrome_options.add_experimental_option('useAutomationExtension', False)
        self.chrome_options.add_argument('--lang=ko-KR')

        self.driver = None

        # 세분화된 지역 (역 단위 + 동 단위)
        self.regions = [
            # 서울 강남구 세분화
            '강남역', '역삼역', '선릉역', '삼성역', '신사역', '압구정역', '청담역',
            '대치동', '역삼동', '삼성동', '청담동', '신사동', '압구정동', '논현동',

            # 서울 강동구 세분화
            '천호역', '강동역', '둔촌동역', '고덕역', '상일동역',
            '천호동', '성내동', '둔촌동', '암사동', '강일동', '상일동',

            # 서울 송파구 세분화
            '잠실역', '석촌역', '송파역', '가락시장역', '문정역', '장지역',
            '잠실동', '신천동', '송파동', '가락동', '문정동', '장지동',

            # 서울 서초구 세분화
            '강남역', '교대역', '서초역', '방배역', '사당역', '남부터미널역',
            '서초동', '방배동', '잠원동', '반포동', '양재동',

            # 서울 영등포구 세분화
            '영등포역', '신길역', '여의도역', '당산역', '문래역',
            '영등포동', '신길동', '여의도동', '당산동', '문래동',

            # 서울 마포구 세분화
            '홍대입구역', '신촌역', '합정역', '상수역', '망원역',
            '홍대', '신촌', '합정', '망원동', '상수동', '연남동',

            # 서울 구로구 세분화
            '구로디지털단지역', '신도림역', '구로역', '가산디지털단지역',
            '구로동', '신도림동', '가리봉동', '가산동',

            # 경기 성남 세분화
            '분당', '판교', '야탑', '서현', '수내', '정자',
            '분당구', '수정구', '중원구', '판교역', '야탑역', '서현역',

            # 경기 수원 세분화
            '수원역', '수원시청역', '영통역', '광교역', '매탄역',
            '팔달구', '영통구', '권선구', '장안구',

            # 인천 세분화
            '부평역', '구월동', '주안역', '부평', '계양',
            '부평구', '계양구', '남동구', '연수구',
        ]

        # 키워드 (기존 유지)
        self.keywords = [
            '휴대폰매장', '휴대폰성지', '스마트폰매장', '폰매장',
            '휴대폰가게', '핸드폰가게', '동네휴대폰매장',
            '휴대폰판매', '휴대폰대리점', '핸드폰매장', '핸드폰판매',
            '스마트폰판매', '휴대폰개통', '기기변경', '번호이동',
            '아이폰', '갤럭시', '아이폰매장', '갤럭시매장',
            '아이폰판매', '갤럭시판매',
            '휴대폰매장추천', '믿을만한휴대폰매장', '안전한개통',
            '휴대폰성지후기', '휴대폰매장후기',
        ]

        self.collected_stores = set()

    def init_driver(self):
        """Chrome 드라이버 초기화"""
        try:
            self.driver = webdriver.Chrome(options=self.chrome_options)
            self.driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {
                'source': '''
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => undefined
                    });
                '''
            })
            print("✅ Chrome 브라우저 시작 완료")
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

        pattern = r'010[-\s]?\d{3,4}[-\s]?\d{4}'
        phones = re.findall(pattern, text)

        normalized = []
        for phone in phones:
            digits = re.sub(r'[-\s]', '', phone)
            if len(digits) == 11:
                formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                normalized.append(formatted)
            elif len(digits) == 10:
                formatted = f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
                normalized.append(formatted)

        return list(set(normalized))

    def search_daangn(self, region, keyword):
        """당근마켓 검색"""
        query = f"{region} {keyword}"
        search_url = f"https://www.google.com/search?q=당근마켓+{query}"

        try:
            self.driver.get(search_url)
            time.sleep(random.uniform(3, 5))

            # 구글 검색 결과에서 daangn.com 링크 수집
            links = self.driver.find_elements(By.TAG_NAME, 'a')
            daangn_links = []

            for link in links:
                href = link.get_attribute('href')
                if href and 'daangn.com/kr/local-profile/' in href:
                    daangn_links.append(href)

            # 중복 제거
            daangn_links = list(set(daangn_links))
            print(f"    📌 {len(daangn_links)}개 daangn.com 링크 발견")

            return daangn_links[:20]  # 상위 20개만

        except Exception as e:
            print(f"    ❌ 검색 실패: {str(e)}")
            return []

    def get_store_detail(self, link):
        """매장 상세 정보"""
        try:
            self.driver.get(link)
            time.sleep(random.uniform(2, 3))

            # 매장명
            try:
                name_elem = self.driver.find_element(By.CSS_SELECTOR, "h1, h2, [class*='name'], [class*='title']")
                store_name = name_elem.text.strip()
            except:
                store_name = "알 수 없음"

            # 전화번호
            page_text = self.driver.find_element(By.TAG_NAME, 'body').text
            phones = self.extract_010_phones(page_text)

            if phones and store_name != "알 수 없음":
                return {
                    'name': store_name,
                    'phones': phones,
                    'link': link
                }

            return None

        except Exception as e:
            return None

    def save_intermediate_results(self, results, search_count):
        """중간 결과 저장"""
        if not results:
            return

        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = self.output_path / f'daangn_v2_intermediate_{search_count}searches_{timestamp}.csv'

            df = pd.DataFrame(results)
            df.to_csv(filename, index=False, encoding='utf-8-sig')
            print(f"    💾 중간 저장 완료: {len(results)}개 매장 → {filename.name}")
        except Exception as e:
            print(f"    ⚠️  중간 저장 실패: {str(e)}")

    def crawl(self, max_searches=3000, save_interval=50):
        """크롤링 실행"""
        print("=" * 80)
        print("🥕 당근마켓 크롤러 V2 (세분화 버전)")
        print("=" * 80)
        print(f"📍 총 지역 수: {len(self.regions)}개 (역/동 단위)")
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
                    break

                print(f"\n[{search_count}/{max_searches}] 🔍 {region} {keyword}")

                while retry_count < max_retries:
                    try:
                        if self.driver is None:
                            print("    🔄 드라이버 재초기화 중...")
                            if not self.init_driver():
                                retry_count += 1
                                time.sleep(5)
                                continue
                            retry_count = 0

                        # 당근마켓 검색
                        links = self.search_daangn(region, keyword)

                        # 각 링크에서 010 전화번호 수집
                        for link in links:
                            detail = self.get_store_detail(link)
                            if detail:
                                for phone in detail['phones']:
                                    store_key = f"{detail['name']}_{phone}"
                                    if store_key not in self.collected_stores:
                                        self.collected_stores.add(store_key)
                                        all_results.append({
                                            '지역명': region,
                                            '매장명': detail['name'],
                                            '전화번호': phone,
                                            '링크': detail['link']
                                        })
                                        print(f"      💾 저장: {detail['name']} ({phone})")

                            time.sleep(random.uniform(1, 2))

                        # 중간 저장
                        if search_count % save_interval == 0:
                            print(f"\n📦 중간 저장 시점 ({search_count}번 검색 완료)")
                            self.save_intermediate_results(all_results, search_count)

                        # 검색 간격 (Google 봇 탐지 회피)
                        wait_time = random.uniform(10, 15)
                        print(f"    ⏳ {wait_time:.1f}초 대기 중...")
                        time.sleep(wait_time)

                        break

                    except Exception as e:
                        print(f"    ❌ 오류 발생: {str(e)}")
                        self.close_driver()
                        self.driver = None
                        retry_count += 1
                        if retry_count >= max_retries:
                            print(f"    ❌ 최대 재시도 횟수 초과")
                            break
                        time.sleep(5)

                retry_count = 0

            if search_count > max_searches:
                break

        print("\n" + "=" * 80)
        print("✅ 최종 크롤링 완료")
        print("=" * 80)
        print(f"📊 총 검색 횟수: {search_count}회")
        print(f"💾 수집된 매장: {len(all_results)}개")

        if all_results:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            df = pd.DataFrame(all_results)
            final_filename = self.output_path / f'daangn_v2_stores_{timestamp}.csv'
            df.to_csv(final_filename, index=False, encoding='utf-8-sig')
            print(f"💾 최종 파일 저장: {final_filename}")

            self.save_intermediate_results(all_results, search_count)

        self.close_driver()
        return all_results

def main():
    """메인 실행 함수"""
    print("🥕 당근마켓 V2 크롤링 시작 (역/동 단위 세분화)...")

    crawler = DaangnStoreCrawlerV2(headless=True)
    results = crawler.crawl(max_searches=3000, save_interval=50)

    print(f"\n🎉 크롤링 완료! 총 {len(results)}개 매장 수집")

if __name__ == "__main__":
    main()
