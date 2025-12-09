#!/usr/bin/env python3
"""
네이버 블로그 크롤러
- 네이버 검색을 통해 휴대폰 업체 블로그 찾기
- 블로그 포스팅에서 전화번호 추출
"""

import re
import csv
import time
import logging
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Set
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('logs/crawler.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class NaverBlogCrawler:
    def __init__(self, headless: bool = False):
        """
        네이버 블로그 크롤러 초기화
        
        Args:
            headless: 헤드리스 모드 여부 (기본값: False)
        """
        self.base_path = Path(__file__).parent
        self.output_path = self.base_path / 'output'
        self.output_path.mkdir(exist_ok=True)
        
        # 로그 폴더 생성
        log_path = self.base_path / 'logs'
        log_path.mkdir(exist_ok=True)
        
        # Chrome 옵션 설정 (봇 탐지 우회)
        chrome_options = Options()
        if headless:
            chrome_options.add_argument('--headless=new')
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-blink-features=AutomationControlled')
        chrome_options.add_experimental_option("excludeSwitches", ["enable-automation", "enable-logging"])
        chrome_options.add_experimental_option('useAutomationExtension', False)
        
        # 더 현실적인 User-Agent
        chrome_options.add_argument('user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
        
        # 추가 봇 탐지 우회 옵션
        chrome_options.add_argument('--disable-web-security')
        chrome_options.add_argument('--disable-features=IsolateOrigins,site-per-process')
        chrome_options.add_argument('--window-size=1920,1080')
        
        # 언어 설정
        chrome_options.add_argument('--lang=ko-KR')
        chrome_options.add_experimental_option('prefs', {
            'intl.accept_languages': 'ko-KR,ko,en-US,en'
        })
        
        self.driver = webdriver.Chrome(options=chrome_options)
        self.driver.implicitly_wait(10)
        
        # 봇 탐지 우회를 위한 JavaScript 실행
        self.driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {
            'source': '''
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
                window.navigator.chrome = {
                    runtime: {}
                };
                Object.defineProperty(navigator, 'plugins', {
                    get: () => [1, 2, 3, 4, 5]
                });
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['ko-KR', 'ko', 'en-US', 'en']
                });
            '''
        })
        
        # 수집된 데이터
        self.results: List[Dict[str, str]] = []
        self.visited_urls: Set[str] = set()
        
        logger.info("네이버 블로그 크롤러 초기화 완료")

    def extract_phone_numbers(self, text: str) -> List[str]:
        """
        텍스트에서 전화번호 추출
        
        전화번호 패턴:
        - 010-XXXX-XXXX
        - 010 XXXX XXXX
        - 010.XXXX.XXXX
        - 010XXXXXXXX
        - 010-XXXX-XXXX (공백 포함)
        
        Args:
            text: 추출할 텍스트
            
        Returns:
            추출된 전화번호 리스트
        """
        # 전화번호 패턴 (010으로 시작하는 11자리 숫자)
        patterns = [
            r'010-\d{4}-\d{4}',  # 010-XXXX-XXXX
            r'010\.\d{4}\.\d{4}',  # 010.XXXX.XXXX
            r'010\s+\d{4}\s+\d{4}',  # 010 XXXX XXXX
            r'010\d{8}',  # 010XXXXXXXX
            r'010\s*-\s*\d{4}\s*-\s*\d{4}',  # 공백 포함 하이픈
        ]
        
        phone_numbers = set()
        
        for pattern in patterns:
            matches = re.findall(pattern, text)
            for match in matches:
                # 숫자만 추출하여 정규화
                digits = re.sub(r'\D', '', match)
                if len(digits) == 11 and digits.startswith('010'):
                    # 표준 형식으로 변환 (010-XXXX-XXXX)
                    formatted = f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
                    phone_numbers.add(formatted)
        
        return sorted(list(phone_numbers))

    def search_naver_blog(self, query: str, max_pages: int = 5) -> List[str]:
        """
        네이버에서 블로그 검색 결과 링크 수집
        
        Args:
            query: 검색어
            max_pages: 최대 검색 페이지 수
            
        Returns:
            블로그 포스트 URL 리스트
        """
        logger.info(f"네이버 검색 시작: '{query}'")
        
        # 네이버 검색 페이지 열기 (직접 검색 URL 사용)
        # 로그인 요구를 피하기 위해 검색 결과 URL을 직접 사용
        encoded_query = query.replace(' ', '+')
        search_url = f"https://search.naver.com/search.naver?where=post&query={encoded_query}"
        
        logger.info(f"검색 URL: {search_url}")
        self.driver.get(search_url)
        time.sleep(5)  # 페이지 로딩 대기 시간 증가
        
        # 로그인 페이지로 리다이렉트되었는지 확인
        current_url = self.driver.current_url
        if 'login' in current_url.lower() or 'nid.naver.com' in current_url:
            logger.warning("로그인 페이지로 리다이렉트되었습니다. 대기 후 재시도...")
            time.sleep(5)
            # 다시 검색 URL로 이동
            self.driver.get(search_url)
            time.sleep(5)
        
        # 현재 URL 확인
        current_url = self.driver.current_url
        logger.info(f"현재 URL: {current_url}")
        
        # 블로그 검색 결과 페이지인지 확인
        if 'where=post' not in current_url and 'blog' not in current_url.lower():
            logger.warning("블로그 검색 결과 페이지가 아닙니다. 블로그 탭으로 전환 시도...")
            blog_tab_found = False
            blog_tab_selectors = [
                "//a[contains(@class, 'tab') and contains(text(), '블로그')]",
                "//a[contains(@href, '/search.naver?where=post')]",
                "//a[contains(@href, 'where=post')]",
                "//a[contains(text(), '블로그') and contains(@class, 'tab')]",
                "//div[@class='lnb']//a[contains(text(), '블로그')]",
                "//ul[@class='lst_type']//a[contains(text(), '블로그')]"
            ]
            
            for selector in blog_tab_selectors:
                try:
                    blog_tab = WebDriverWait(self.driver, 3).until(
                        EC.element_to_be_clickable((By.XPATH, selector))
                    )
                    blog_tab.click()
                    time.sleep(3)
                    logger.info(f"블로그 탭 클릭 완료 (선택자: {selector})")
                    blog_tab_found = True
                    break
                except:
                    continue
            
            if not blog_tab_found:
                logger.warning("블로그 탭을 찾을 수 없습니다. 현재 페이지에서 진행합니다.")
        
        # 블로그 링크 및 내용 수집 (검색 결과 페이지에서 직접 추출)
        blog_data = []  # {'url': ..., 'text': ...} 형태
        page = 1
        
        try:
            while page <= max_pages:
                logger.info(f"페이지 {page} 수집 중...")
                
                # 현재 페이지의 블로그 링크와 내용 찾기
                try:
                    # 여러 선택자로 블로그 항목 찾기
                    blog_selectors = [
                        ".api_subject_bx",
                        ".sh_blog_title",
                        ".total_tit",
                        "li[class*='sh_blog_top']",
                        "div[class*='sh_blog_top']"
                    ]
                    
                    found_items = []
                    for selector in blog_selectors:
                        try:
                            items = self.driver.find_elements(By.CSS_SELECTOR, selector)
                            if items:
                                found_items = items
                                break
                        except:
                            continue
                    
                    # 블로그 항목에서 링크와 텍스트 추출
                    page_urls = set()
                    for item in found_items:
                        try:
                            # 링크 찾기
                            link_elem = item.find_element(By.CSS_SELECTOR, "a[href*='blog.naver.com']")
                            url = link_elem.get_attribute('href')
                            if url and 'blog.naver.com' in url:
                                clean_url = url.split('?')[0].split('#')[0]
                                if clean_url not in page_urls:
                                    page_urls.add(clean_url)
                                    
                                    # 항목의 전체 텍스트 가져오기 (제목, 설명 등)
                                    item_text = item.text
                                    
                                    blog_data.append({
                                        'url': clean_url,
                                        'text': item_text
                                    })
                                    logger.debug(f"블로그 항목 발견: {clean_url}")
                        except:
                            continue
                    
                    logger.info(f"페이지 {page}에서 {len(page_urls)}개 항목 발견 (총 {len(blog_data)}개)")
                    
                    # 다음 페이지로 이동
                    if page < max_pages:
                        next_button_found = False
                        next_selectors = [
                            "//a[contains(@class, 'next')]",
                            "//a[contains(@class, 'btn_next')]",
                            "//a[contains(text(), '다음')]",
                            "//a[@aria-label='다음']",
                            "//a[contains(@href, 'start=') and contains(@class, 'next')]"
                        ]
                        
                        for selector in next_selectors:
                            try:
                                next_button = self.driver.find_element(By.XPATH, selector)
                                if next_button.is_enabled() and next_button.is_displayed():
                                    next_button.click()
                                    time.sleep(3)
                                    page += 1
                                    next_button_found = True
                                    break
                            except:
                                continue
                        
                        if not next_button_found:
                            logger.info("더 이상 페이지가 없습니다.")
                            break
                    else:
                        break
                        
                except Exception as e:
                    logger.error(f"페이지 {page} 수집 중 오류: {str(e)}")
                    break
            
            logger.info(f"총 {len(blog_data)}개의 블로그 항목을 수집했습니다.")
            return blog_data
            
        except Exception as e:
            logger.error(f"검색 중 오류 발생: {str(e)}", exc_info=True)
            return []

    def extract_phones_from_text(self, text: str, url: str) -> List[str]:
        """
        텍스트에서 전화번호 추출 (검색 결과 페이지에서 직접)
        
        Args:
            text: 추출할 텍스트
            url: 블로그 URL
            
        Returns:
            추출된 전화번호 리스트
        """
        if url in self.visited_urls:
            return []
        
        phone_numbers = self.extract_phone_numbers(text)
        
        if phone_numbers:
            logger.info(f"전화번호 {len(phone_numbers)}개 발견: {phone_numbers}")
            self.visited_urls.add(url)
            return phone_numbers
        
        return []
    
    def crawl_blog_post_direct(self, url: str) -> List[str]:
        """
        블로그 포스트 직접 방문하여 전화번호 추출 (로그인 우회 강화)
        
        Args:
            url: 블로그 포스트 URL
            
        Returns:
            추출된 전화번호 리스트
        """
        if url in self.visited_urls:
            return []
        
        try:
            logger.info(f"블로그 포스트 직접 방문: {url}")
            
            # URL 정리
            clean_url = url.split('?')[0].split('#')[0]
            
            # 새 탭에서 열기 (로그인 요구 회피)
            self.driver.execute_script(f"window.open('{clean_url}', '_blank');")
            time.sleep(2)
            
            # 새 탭으로 전환
            self.driver.switch_to.window(self.driver.window_handles[-1])
            time.sleep(3)
            
            # 로그인 페이지로 리다이렉트되었는지 확인
            current_url = self.driver.current_url
            if 'login' in current_url.lower() or 'nid.naver.com' in current_url:
                logger.warning(f"로그인 페이지로 리다이렉트됨: {clean_url}")
                self.driver.close()
                self.driver.switch_to.window(self.driver.window_handles[0])
                return []
            
            # 페이지 내용 가져오기
            try:
                # 여러 선택자로 본문 찾기
                content_selectors = [
                    "div.se-main-container",
                    "div#postViewArea",
                    "div.post-view",
                    "div.post_ct",
                    "div.se-component-content",
                    "div._postView"
                ]
                
                page_text = ""
                for selector in content_selectors:
                    try:
                        element = self.driver.find_element(By.CSS_SELECTOR, selector)
                        page_text = element.text
                        if page_text and len(page_text) > 50:  # 충분한 텍스트가 있는지 확인
                            break
                    except:
                        continue
                
                if not page_text or len(page_text) < 50:
                    # body 전체 텍스트 가져오기
                    page_text = self.driver.find_element(By.TAG_NAME, "body").text
                
                # 전화번호 추출
                phone_numbers = self.extract_phone_numbers(page_text)
                
                # 탭 닫기
                self.driver.close()
                self.driver.switch_to.window(self.driver.window_handles[0])
                
                if phone_numbers:
                    logger.info(f"전화번호 {len(phone_numbers)}개 발견: {phone_numbers}")
                    self.visited_urls.add(url)
                    return phone_numbers
                else:
                    logger.debug(f"전화번호를 찾을 수 없습니다: {clean_url}")
                    return []
                    
            except Exception as e:
                logger.error(f"페이지 내용 추출 중 오류: {str(e)}")
                try:
                    self.driver.close()
                    self.driver.switch_to.window(self.driver.window_handles[0])
                except:
                    pass
                return []
                
        except Exception as e:
            logger.error(f"블로그 포스트 크롤링 중 오류: {str(e)}")
            try:
                if len(self.driver.window_handles) > 1:
                    self.driver.close()
                    self.driver.switch_to.window(self.driver.window_handles[0])
            except:
                pass
            return []

    def crawl(self, query: str, max_pages: int = 5, max_posts: int = 50):
        """
        전체 크롤링 프로세스 실행
        
        Args:
            query: 검색어
            max_pages: 최대 검색 페이지 수
            max_posts: 최대 포스트 수
        """
        logger.info("=" * 60)
        logger.info("네이버 블로그 크롤링 시작")
        logger.info(f"검색어: {query}")
        logger.info(f"최대 페이지: {max_pages}, 최대 포스트: {max_posts}")
        logger.info("=" * 60)
        
        # 1. 블로그 링크 및 내용 수집
        blog_data = self.search_naver_blog(query, max_pages)
        
        if not blog_data:
            logger.warning("수집된 블로그 항목이 없습니다.")
            return
        
        # 2. 검색 결과 페이지에서 직접 전화번호 추출 (개별 포스트 방문 불필요)
        processed = 0
        for blog_item in blog_data[:max_posts]:
            url = blog_item['url']
            text = blog_item['text']
            
            # 검색 결과 페이지의 텍스트에서 전화번호 추출
            phone_numbers = self.extract_phones_from_text(text, url)
            
            if phone_numbers:
                for phone in phone_numbers:
                    self.results.append({
                        'phone_number': phone,
                        'blog_url': url
                    })
                processed += 1
                logger.info(f"✅ 전화번호 발견: {url} -> {phone_numbers}")
            else:
                # 검색 결과에 전화번호가 없으면 개별 포스트 방문 시도
                logger.debug(f"검색 결과에서 전화번호를 찾을 수 없음. 개별 포스트 방문 시도: {url}")
                phone_numbers = self.crawl_blog_post_direct(url)
                
                if phone_numbers:
                    for phone in phone_numbers:
                        self.results.append({
                            'phone_number': phone,
                            'blog_url': url
                        })
                    processed += 1
                    logger.info(f"✅ 개별 포스트에서 전화번호 발견: {url} -> {phone_numbers}")
            
            # 요청 간격 (서버 부하 방지 및 봇 탐지 회피)
            time.sleep(2 + (processed % 3) * 0.5)  # 2~3.5초 랜덤 대기
        
        logger.info(f"크롤링 완료: {len(self.results)}개의 전화번호 수집")

    def save_to_csv(self, filename: str = None):
        """
        결과를 CSV 파일로 저장
        
        Args:
            filename: 저장할 파일명 (None이면 자동 생성)
        """
        if not self.results:
            logger.warning("저장할 데이터가 없습니다.")
            return
        
        if filename is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"naver_blog_phones_{timestamp}.csv"
        
        filepath = self.output_path / filename
        
        # 중복 제거 (같은 전화번호와 링크 조합)
        unique_results = []
        seen = set()
        for result in self.results:
            key = (result['phone_number'], result['blog_url'])
            if key not in seen:
                seen.add(key)
                unique_results.append(result)
        
        # CSV 저장
        with open(filepath, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.DictWriter(f, fieldnames=['phone_number', 'blog_url'])
            writer.writeheader()
            writer.writerows(unique_results)
        
        logger.info(f"CSV 파일 저장 완료: {filepath}")
        logger.info(f"총 {len(unique_results)}개의 고유한 전화번호 저장")

    def close(self):
        """브라우저 종료"""
        if self.driver:
            self.driver.quit()
            logger.info("브라우저 종료")


def main():
    """메인 실행 함수"""
    crawler = None
    try:
        # 크롤러 생성 (headless=False로 브라우저 표시)
        crawler = NaverBlogCrawler(headless=False)
        
        # 검색어 설정
        query = "안양 휴대폰성지 010"
        
        # 크롤링 실행
        crawler.crawl(
            query=query,
            max_pages=5,  # 최대 5페이지 검색
            max_posts=50  # 최대 50개 포스트 크롤링
        )
        
        # 결과 저장
        crawler.save_to_csv()
        
        logger.info("=" * 60)
        logger.info("모든 작업 완료!")
        logger.info("=" * 60)
        
    except KeyboardInterrupt:
        logger.info("사용자에 의해 중단되었습니다.")
    except Exception as e:
        logger.error(f"오류 발생: {str(e)}", exc_info=True)
    finally:
        if crawler:
            crawler.close()


if __name__ == "__main__":
    main()

