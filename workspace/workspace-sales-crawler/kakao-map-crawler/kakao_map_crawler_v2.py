"""
카카오맵 휴대폰 성지 크롤러 (개선 버전)
- 특정 검색 URL에서 매장 정보 수집
- Selenium을 사용한 동적 콘텐츠 크롤링
- CSV 파일로 결과 저장
"""

import time
import re
import random
from datetime import datetime
from pathlib import Path
from urllib.parse import quote_plus
import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException, StaleElementReferenceException

class KakaoMapCrawlerV2:
    """카카오맵 휴대폰 매장 크롤러 V2"""

    def __init__(self, headless=False):
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)

        # Chrome 옵션 설정
        self.chrome_options = Options()
        if headless:
            self.chrome_options.add_argument('--headless')
        self.chrome_options.add_argument('--no-sandbox')
        self.chrome_options.add_argument('--disable-dev-shm-usage')
        self.chrome_options.add_argument('--disable-blink-features=AutomationControlled')
        self.chrome_options.add_argument('--window-size=1920,1080')
        
        # User-Agent 설정
        user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        self.chrome_options.add_argument(f'user-agent={user_agent}')
        self.chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        self.chrome_options.add_experimental_option('useAutomationExtension', False)

        self.driver = None
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

    def extract_phone_numbers(self, text):
        """텍스트에서 전화번호 추출 (010, 02, 031 등 모두 포함)"""
        if not text:
            return []

        # 다양한 전화번호 패턴
        patterns = [
            r'010[-\s]?\d{3,4}[-\s]?\d{4}',  # 010
            r'02[-\s]?\d{3,4}[-\s]?\d{4}',   # 서울
            r'0\d{2}[-\s]?\d{3,4}[-\s]?\d{4}' # 기타 지역번호
        ]
        
        phones = []
        for pattern in patterns:
            matches = re.findall(pattern, text)
            phones.extend(matches)

        # 전화번호 정규화
        normalized = []
        for phone in phones:
            digits = re.sub(r'[-\s]', '', phone)
            if len(digits) >= 9 and len(digits) <= 11:
                if digits.startswith('02'):
                    if len(digits) == 9:
                        formatted = f"{digits[:2]}-{digits[2:5]}-{digits[5:]}"
                    else:
                        formatted = f"{digits[:2]}-{digits[2:6]}-{digits[6:]}"
                elif len(digits) == 10:
                    formatted = f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
                elif len(digits) == 11:
                    formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                else:
                    formatted = phone
                normalized.append(formatted)

        unique_numbers = []
        seen = set()
        for num in normalized:
            if num not in seen:
                seen.add(num)
                unique_numbers.append(num)
        return unique_numbers

    def extract_mobile_number(self, text):
        """텍스트에서 010으로 시작하는 번호만 추출"""
        numbers = self.extract_phone_numbers(text)
        for number in numbers:
            if number.startswith('010'):
                return number
        return ""

    def is_mobile_number(self, phone):
        return bool(phone and phone.startswith('010'))

    def scroll_page(self, scroll_container_selector):
        """페이지 스크롤하여 모든 결과 로드"""
        try:
            scroll_container = self.driver.find_element(By.CSS_SELECTOR, scroll_container_selector)
            last_height = self.driver.execute_script("return arguments[0].scrollHeight", scroll_container)
            
            scroll_count = 0
            max_scrolls = 20  # 더 많이 스크롤
            
            while scroll_count < max_scrolls:
                # 스크롤 다운
                self.driver.execute_script("arguments[0].scrollTo(0, arguments[0].scrollHeight);", scroll_container)
                time.sleep(random.uniform(1.5, 2.5))
                
                # 새로운 높이 확인
                new_height = self.driver.execute_script("return arguments[0].scrollHeight", scroll_container)
                
                if new_height == last_height:
                    break
                    
                last_height = new_height
                scroll_count += 1
                print(f"    📜 스크롤 {scroll_count}회 완료")
                
        except Exception as e:
            print(f"    ⚠️  스크롤 중 오류: {str(e)}")

    def click_next_page(self):
        """
        다음 페이지로 이동.
        - 현재 페이지 블록(1~5, 6~10 등)에서 다음 숫자 버튼을 우선 클릭
        - 블록 끝에 도달하면 '다음' 버튼을 눌러 다음 블록으로 전환
        """
        try:
            pagination = self.driver.find_element(By.CSS_SELECTOR, "#info\\.search\\.page .pageWrap")
            page_links = pagination.find_elements(By.CSS_SELECTOR, "a[id^='info\\.search\\.page\\.no']")
            
            found_active = False
            for link in page_links:
                classes_raw = (link.get_attribute("class") or "").upper()
                class_tokens = classes_raw.replace(",", " ").split()
                if "HIDDEN" in class_tokens:
                    continue
                if "ACTIVE" in class_tokens:
                    found_active = True
                    continue
                if found_active:
                    self.driver.execute_script("arguments[0].click();", link)
                    time.sleep(random.uniform(2, 3))
                    return True
            
            # 현재 블록의 마지막 페이지까지 본 경우 - 다음 블록으로 전환
            next_button = pagination.find_element(By.CSS_SELECTOR, "#info\\.search\\.page\\.next")
            button_class = (next_button.get_attribute("class") or "").lower()
            if "disabled" in button_class:
                return False
            
            self.driver.execute_script("arguments[0].click();", next_button)
            time.sleep(random.uniform(2, 3))
            return True
            
        except NoSuchElementException:
            return False
        except Exception as e:
            print(f"    ⚠️  다음 페이지 이동 실패: {str(e)}")
            return False

    def get_current_page_number(self):
        """현재 페이지 번호 텍스트 반환"""
        try:
            active = self.driver.find_element(By.CSS_SELECTOR, "#info\\.search\\.page .ACTIVE")
            return active.text.strip()
        except Exception:
            return ""

    def get_store_info_from_list(self):
        """검색 결과 리스트에서 매장 정보 추출 (현재 페이지만)"""
        stores = []
        
        try:
            # 검색 결과 리스트 대기
            wait = WebDriverWait(self.driver, 10)
            wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "#info\\.search\\.place\\.list")))
            
            # 스크롤하여 모든 결과 로드
            print("    📜 검색 결과 스크롤 중...")
            self.scroll_page("#info\\.search\\.place\\.list")
            
            time.sleep(2)
            
            # 모든 매장 항목 가져오기
            items = self.driver.find_elements(By.CSS_SELECTOR, "#info\\.search\\.place\\.list > li")
            print(f"    📌 현재 페이지: {len(items)}개 매장 발견")
            
            for idx, item in enumerate(items, 1):
                try:
                    # 매장명 - 정확한 선택자 사용
                    name_elem = item.find_element(By.CSS_SELECTOR, ".head_item .tit_name .link_name")
                    store_name = name_elem.text.strip()
                    
                    # 카카오맵 링크
                    link = name_elem.get_attribute('href')
                    
                    # 주소 정보 - XPath 기반으로 정확하게 추출
                    address = ""
                    try:
                        addr_elem = item.find_element(By.CSS_SELECTOR, ".info_item .addr p")
                        address = addr_elem.text.strip()
                    except:
                        pass
                    
                    # 전화번호 - 다양한 선택자 + 텍스트 추출
                    phone_text = ""
                    phone_selectors = [
                        ".info_item .tel",
                        ".info_item .contact .phone",
                        ".contact [data-id='phone']",
                        "[data-id='phone'].phone",
                    ]
                    for selector in phone_selectors:
                        if phone_text:
                            break
                        try:
                            phone_elem = item.find_element(By.CSS_SELECTOR, selector)
                            candidate = phone_elem.text.strip()
                            if candidate:
                                phone_text = candidate
                        except NoSuchElementException:
                            continue
                        except Exception:
                            continue
                    
                    phone = self.extract_mobile_number(phone_text)
                    if not phone:
                        # 카드 전체 텍스트에서 010 번호 다시 확인
                        phone = self.extract_mobile_number(item.text)
                    
                    # 카테고리
                    category = ""
                    try:
                        cat_elem = item.find_element(By.CSS_SELECTOR, ".head_item .subcategory")
                        category = cat_elem.text.strip()
                    except:
                        pass
                    
                    store_data = {
                        'name': store_name,
                        'address': address,
                        'phone': phone,
                        'category': category,
                        'link': link
                    }
                    
                    stores.append(store_data)
                    phone_log = phone if phone else '010없음'
                    print(f"    [{idx}] {store_name} - {address[:20] if address else '주소없음'} - {phone_log}")
                    
                except StaleElementReferenceException:
                    print(f"    ⚠️  항목 {idx} 스킵 (요소 변경됨)")
                    continue
                except Exception as e:
                    print(f"    ⚠️  항목 {idx} 파싱 실패: {str(e)}")
                    continue
                    
        except TimeoutException:
            print("    ❌ 검색 결과를 찾을 수 없습니다")
        except Exception as e:
            print(f"    ❌ 매장 정보 추출 실패: {str(e)}")
            
        return stores

    def get_detailed_info(self, store):
        """매장 상세 페이지에서 추가 정보 수집 - 사용 안 함"""
        # 리스트에서 이미 모든 정보를 수집하므로 상세 페이지 방문 불필요
        return store

    def crawl_from_url(self, url, get_details=True, save_to_file=True, keyword=None):
        """특정 URL에서 크롤링"""
        print("=" * 80)
        print("🗺️  카카오맵 휴대폰 매장 크롤러 V2")
        print("=" * 80)
        print(f"🔗 URL: {url}")
        print("=" * 80)
        if keyword:
            print(f"🔑 검색 키워드: {keyword}")
            print("=" * 80)
        
        if not self.init_driver():
            return []
        
        all_results = []
        
        try:
            # 카카오맵 페이지 열기
            print("\n📱 카카오맵 페이지 로딩 중...")
            self.driver.get(url)
            time.sleep(random.uniform(4, 6))
            
            # 페이지네이션을 통해 모든 매장 정보 수집
            print("\n🔍 매장 정보 수집 중 (페이지네이션 처리)...")
            all_stores = []
            processed_pages = 0
            
            while True:
                current_page_label = self.get_current_page_number()
                display_page = current_page_label or str(processed_pages + 1)
                print(f"\n[페이지 {display_page}] 수집 중...")
                stores = self.get_store_info_from_list()
                
                if not stores:
                    print(f"    ⚠️  페이지 {display_page}에서 매장을 찾을 수 없습니다. 종료합니다.")
                    break
                
                all_stores.extend(stores)
                print(f"    ✅ 페이지 {display_page}: {len(stores)}개 수집 (누적: {len(all_stores)}개)")
                
                processed_pages += 1
                # 다음 페이지로 이동
                if not self.click_next_page():
                    print(f"\n    ✅ 마지막 페이지 도달. 총 {processed_pages}페이지 수집 완료")
                    break
                
                time.sleep(random.uniform(1, 2))
            
            stores = all_stores
            print(f"\n📊 1차 수집 완료: {len(stores)}개 매장 (총 {processed_pages}페이지)")
            
            # 수집된 매장 정보를 결과에 추가
            print(f"\n📝 결과 정리 중...")
            for store in stores:
                phone = store.get('phone', '')
                if not self.is_mobile_number(phone):
                    continue
                store_key = f"{store['name']}_{phone}"
                if store_key not in self.collected_stores:
                    self.collected_stores.add(store_key)
                    all_results.append({
                        '매장명': store['name'],
                        '주소': store.get('address', ''),
                        '전화번호': phone,
                        '카테고리': store.get('category', ''),
                        '카카오맵링크': store.get('link', '')
                    })
            
            print("\n" + "=" * 80)
            print("✅ 크롤링 완료")
            print("=" * 80)
            print(f"💾 수집된 매장: {len(all_results)}개")
            
            # CSV 파일로 저장
            if save_to_file and all_results:
                self.save_results(all_results)
                
        except Exception as e:
            print(f"\n❌ 크롤링 중 오류 발생: {str(e)}")
            import traceback
            traceback.print_exc()
        finally:
            self.close_driver()
        
        return all_results

    def crawl_keywords(self, keywords, get_details=True):
        """여러 키워드를 순차적으로 검색"""
        aggregated_results = []
        for keyword in keywords:
            url = self.build_search_url(keyword)
            keyword_results = self.crawl_from_url(
                url,
                get_details=get_details,
                save_to_file=False,
                keyword=keyword
            )
            aggregated_results.extend(keyword_results)
        
        if aggregated_results:
            self.save_results(aggregated_results, suffix="_multi")
        else:
            print("❌ 모든 키워드에서 유효한 010 전화번호를 찾지 못했습니다.")
        
        return aggregated_results

    def build_search_url(self, keyword):
        encoded = quote_plus(keyword)
        return f"https://map.kakao.com/?from=total&nil_suggest=btn&q={encoded}&tab=place"

    def save_results(self, results, suffix=""):
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = self.output_path / f'kakao_phone_stores_{timestamp}{suffix}.csv'
        df = pd.DataFrame(results)
        df.to_csv(filename, index=False, encoding='utf-8-sig')
        print(f"💾 파일 저장 완료: {filename}")
        
        phone_count = df['전화번호'].apply(lambda x: isinstance(x, str) and x.startswith('010')).sum()
        addr_count = df['주소'].apply(lambda x: isinstance(x, str) and len(x) > 0).sum()
        
        print(f"\n📊 통계:")
        print(f"  - 010 전화번호 있음: {phone_count}개")
        print(f"  - 주소 있음: {addr_count}개")


def main():
    """메인 실행 함수"""
    keywords = [
        "휴대폰 성지",
        "휴대폰 판매점",
        "휴대폰 백화점",
        "휴대폰 대리점",
        "휴대폰 할인점",
        "휴대폰 매장",
    ]
    
    print("🚀 카카오맵 크롤링 시작...")
    print(f"🔑 대상 키워드: {', '.join(keywords)}\n")
    
    # 크롤러 실행
    crawler = KakaoMapCrawlerV2(headless=False)  # headless=True로 설정하면 브라우저 숨김
    results = crawler.crawl_keywords(keywords, get_details=True)
    
    print(f"\n🎉 크롤링 완료! 010 번호 매장 {len(results)}개 수집")


if __name__ == "__main__":
    main()
