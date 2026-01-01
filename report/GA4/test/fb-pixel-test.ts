/**
 * FB Pixel 이벤트 테스트 스크립트
 * Playwright를 사용하여 실제 브라우저에서 고객 시나리오 테스트
 *
 * 실행 방법:
 * 1. cd /Users/jacob/Desktop/workspace/nofee/nofee-planning-jacob/report/GA4/test
 * 2. npx ts-node fb-pixel-test.ts
 */

import { chromium, Browser, Page, Request } from 'playwright';

const BASE_URL = 'http://localhost:3001';

interface PixelEvent {
  eventName: string;
  params: Record<string, unknown>;
  timestamp: Date;
  url: string;
}

class FBPixelTester {
  private browser: Browser | null = null;
  private page: Page | null = null;
  private capturedEvents: PixelEvent[] = [];

  async init() {
    console.log('\n🚀 FB Pixel 테스트 시작...\n');

    this.browser = await chromium.launch({
      headless: false, // 브라우저 UI 표시
      slowMo: 500 // 액션 간 딜레이 (ms)
    });

    const context = await this.browser.newContext({
      viewport: { width: 430, height: 932 } // 모바일 뷰포트
    });

    this.page = await context.newPage();

    // FB Pixel 요청 캡처
    this.page.on('request', (request: Request) => {
      const url = request.url();
      if (url.includes('facebook.com/tr')) {
        this.capturePixelEvent(request);
      }
    });

    // 콘솔 로그 캡처
    this.page.on('console', msg => {
      const text = msg.text();
      if (text.includes('[FB') || text.includes('fbq')) {
        console.log(`  📋 Console: ${text}`);
      }
    });
  }

  private capturePixelEvent(request: Request) {
    try {
      const url = new URL(request.url());
      const eventName = url.searchParams.get('ev') || 'Unknown';
      const customData = url.searchParams.get('cd');

      const event: PixelEvent = {
        eventName,
        params: customData ? JSON.parse(customData) : {},
        timestamp: new Date(),
        url: request.url()
      };

      this.capturedEvents.push(event);
      console.log(`  ✅ FB Pixel Event: ${eventName}`);

      if (Object.keys(event.params).length > 0) {
        console.log(`     Params: ${JSON.stringify(event.params)}`);
      }
    } catch (e) {
      // Parse 실패 시 무시
    }
  }

  async testScenario1_HomePageView() {
    console.log('\n📍 시나리오 1: 홈 페이지 방문');
    console.log('─'.repeat(40));

    await this.page!.goto(`${BASE_URL}/home-v2`);
    await this.page!.waitForTimeout(2000);

    console.log('  예상 이벤트: PageView');
  }

  async testScenario2_SPANavigation() {
    console.log('\n📍 시나리오 2: SPA 페이지 이동 (홈 → 비교)');
    console.log('─'.repeat(40));

    // 홈에서 시작
    await this.page!.goto(`${BASE_URL}/home-v2`);
    await this.page!.waitForTimeout(1000);

    // 비교 페이지로 이동
    await this.page!.goto(`${BASE_URL}/compare`);
    await this.page!.waitForTimeout(2000);

    console.log('  예상 이벤트: PageView (라우트 변경)');
  }

  async testScenario3_FilterUse() {
    console.log('\n📍 시나리오 3: 필터 사용');
    console.log('─'.repeat(40));

    await this.page!.goto(`${BASE_URL}/home-v2`);
    await this.page!.waitForTimeout(2000);

    // 브랜드 필터 버튼 클릭 시도
    try {
      const filterButton = await this.page!.$('button:has-text("브랜드")');
      if (filterButton) {
        await filterButton.click();
        await this.page!.waitForTimeout(1000);

        // 아이폰 선택
        const iphoneOption = await this.page!.$('button:has-text("아이폰")');
        if (iphoneOption) {
          await iphoneOption.click();
          await this.page!.waitForTimeout(1000);
          console.log('  예상 이벤트: Search (brand:아이폰)');
        }
      }
    } catch (e) {
      console.log('  ⚠️ 필터 버튼을 찾을 수 없음');
    }
  }

  async testScenario4_DealView() {
    console.log('\n📍 시나리오 4: 딜 상세 조회');
    console.log('─'.repeat(40));

    await this.page!.goto(`${BASE_URL}/home-v2`);
    await this.page!.waitForTimeout(2000);

    // 첫 번째 딜 카드 클릭
    try {
      const dealCard = await this.page!.$('[data-testid="deal-card"]');
      if (dealCard) {
        await dealCard.click();
        await this.page!.waitForTimeout(2000);
        console.log('  예상 이벤트: ViewContent');
      } else {
        // data-testid가 없으면 다른 방법 시도
        console.log('  ⚠️ 딜 카드를 찾을 수 없음 - 직접 URL로 이동');
      }
    } catch (e) {
      console.log('  ⚠️ 딜 카드 클릭 실패');
    }
  }

  async testScenario5_WelcomePageLogin() {
    console.log('\n📍 시나리오 5: Welcome 페이지 → 카카오 로그인 클릭');
    console.log('─'.repeat(40));

    await this.page!.goto(`${BASE_URL}/welcome`);
    await this.page!.waitForTimeout(2000);

    // 카카오 로그인 버튼 찾기
    try {
      const kakaoButton = await this.page!.$('button:has-text("카카오")');
      if (kakaoButton) {
        console.log('  카카오 로그인 버튼 발견');
        // 클릭하면 외부 리다이렉트되므로 클릭 직전까지만
        console.log('  예상 이벤트: Contact (login intent)');
        // await kakaoButton.click(); // 실제 클릭은 하지 않음 (외부 리다이렉트 방지)
      }
    } catch (e) {
      console.log('  ⚠️ 카카오 버튼을 찾을 수 없음');
    }
  }

  printSummary() {
    console.log('\n');
    console.log('═'.repeat(50));
    console.log('📊 테스트 결과 요약');
    console.log('═'.repeat(50));

    console.log(`\n총 캡처된 FB Pixel 이벤트: ${this.capturedEvents.length}개\n`);

    const eventCounts: Record<string, number> = {};
    this.capturedEvents.forEach(event => {
      eventCounts[event.eventName] = (eventCounts[event.eventName] || 0) + 1;
    });

    console.log('이벤트별 횟수:');
    Object.entries(eventCounts).forEach(([name, count]) => {
      console.log(`  - ${name}: ${count}회`);
    });

    console.log('\n상세 이벤트 로그:');
    this.capturedEvents.forEach((event, i) => {
      console.log(`  ${i + 1}. [${event.timestamp.toISOString()}] ${event.eventName}`);
    });
  }

  async cleanup() {
    if (this.browser) {
      await this.browser.close();
    }
  }

  async runAllTests() {
    try {
      await this.init();

      await this.testScenario1_HomePageView();
      await this.testScenario2_SPANavigation();
      await this.testScenario3_FilterUse();
      await this.testScenario4_DealView();
      await this.testScenario5_WelcomePageLogin();

      this.printSummary();

      console.log('\n⏳ 10초 후 브라우저 종료...');
      await this.page!.waitForTimeout(10000);

    } catch (error) {
      console.error('테스트 중 오류 발생:', error);
    } finally {
      await this.cleanup();
    }
  }
}

// 실행
const tester = new FBPixelTester();
tester.runAllTests();
