/**
 * FB Pixel 이벤트 테스트 스크립트
 * 실제 고객 시나리오 기반 테스트
 *
 * 실행: node fb-pixel-test.js
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';

class FBPixelTester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.capturedEvents = [];
    this.networkRequests = [];
  }

  async init() {
    console.log('\n🚀 FB Pixel 이벤트 테스트 시작');
    console.log('═'.repeat(50));

    this.browser = await chromium.launch({
      headless: false,
      slowMo: 500
    });

    const context = await this.browser.newContext({
      viewport: { width: 430, height: 932 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15'
    });

    this.page = await context.newPage();

    // 모든 네트워크 요청 캡처
    this.page.on('request', (request) => {
      const url = request.url();

      // FB Pixel 요청 캡처 (모든 facebook 관련 요청)
      if (url.includes('facebook.com') || url.includes('facebook.net')) {
        this.capturePixelRequest(request);
      }

      // CAPI 요청 캡처
      if (url.includes('/api/fb-conversion')) {
        console.log(`  📤 CAPI 요청: ${url}`);
      }
    });

    // 콘솔 로그 캡처 (FB 관련만)
    this.page.on('console', msg => {
      const text = msg.text();
      if (text.includes('[FB') || text.includes('fbq') || text.includes('Pixel') ||
          text.includes('ViewContent') || text.includes('InitiateCheckout') ||
          text.includes('trackDeal') || text.includes('Search')) {
        console.log(`  📋 ${text}`);
      }
    });
  }

  capturePixelRequest(request) {
    try {
      const url = new URL(request.url());

      // fbevents.js 로드는 건너뛰기
      if (url.pathname.includes('fbevents.js')) {
        console.log('  📦 FB Pixel SDK 로드됨');
        return;
      }

      const eventName = url.searchParams.get('ev');
      if (!eventName) return;

      const event = {
        eventName,
        timestamp: new Date(),
        rawUrl: request.url()
      };

      // cd (custom data) 파싱
      const cd = url.searchParams.get('cd');
      if (cd) {
        try {
          event.customData = JSON.parse(cd);
        } catch (e) {
          event.customData = cd;
        }
      }

      this.capturedEvents.push(event);
      console.log(`  ✅ FB Pixel: ${eventName}`);
      if (event.customData) {
        console.log(`     Data: ${JSON.stringify(event.customData)}`);
      }
    } catch (e) {
      // 무시
    }
  }

  async waitForPixelLoad() {
    console.log('\n⏳ FB Pixel SDK 로드 대기...');
    try {
      await this.page.waitForFunction(() => window.fbq !== undefined, { timeout: 10000 });
      console.log('  ✅ FB Pixel SDK 로드 완료');
      return true;
    } catch (e) {
      console.log('  ⚠️ FB Pixel SDK 로드 실패 (타임아웃)');
      return false;
    }
  }

  // 시나리오 1: 홈 페이지 첫 방문 (PageView)
  async scenario1_HomeFirstVisit() {
    console.log('\n📍 시나리오 1: 홈 페이지 첫 방문');
    console.log('─'.repeat(40));
    console.log('  URL: /home-v2');
    console.log('  예상: PageView 이벤트');

    await this.page.goto(`${BASE_URL}/home-v2`, { waitUntil: 'domcontentloaded' });
    await this.waitForPixelLoad();
    await this.page.waitForTimeout(2000);
  }

  // 시나리오 2: 딜 카드 클릭 → 딜 상세 (ViewContent)
  async scenario2_DealDetailView() {
    console.log('\n📍 시나리오 2: 딜 상세 페이지 조회');
    console.log('─'.repeat(40));
    console.log('  행동: 홈에서 딜 카드 클릭');
    console.log('  예상: ViewContent 이벤트');

    // 홈에서 시작
    await this.page.goto(`${BASE_URL}/home-v2`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForTimeout(3000);

    // 딜 카드 찾기 및 클릭
    const dealCards = await this.page.$$('a[href*="/deal/"]');
    if (dealCards.length > 0) {
      console.log(`  딜 카드 ${dealCards.length}개 발견`);
      const beforeCount = this.capturedEvents.length;
      await dealCards[0].click();
      // 딜 페이지 로드 및 데이터 fetch 완료 대기
      await this.page.waitForTimeout(6000);
      console.log(`  현재 URL: ${this.page.url()}`);
      const afterCount = this.capturedEvents.length;
      console.log(`  이벤트 수: ${beforeCount} → ${afterCount}`);

      // fbq 상태 및 호출 로그 확인
      const fbqStatus = await this.page.evaluate(() => {
        const status = {
          fbqExists: typeof window.fbq === 'function',
          fbqLoaded: window.fbq?.loaded,
          queueLength: window.fbq?.queue?.length || 0,
          pixelId: window.fbq?.getState?.()?.pixelId || 'unknown'
        };

        // 직접 테스트 이벤트 발송
        if (window.fbq) {
          window.fbq('track', 'ViewContent', { content_name: 'TEST_FROM_PLAYWRIGHT', value: 1 });
        }

        return status;
      });
      console.log(`  📊 fbq 상태: ${JSON.stringify(fbqStatus)}`);

      // 테스트 이벤트 요청 대기
      await this.page.waitForTimeout(2000);
      console.log(`  이벤트 수 (테스트 후): ${this.capturedEvents.length}`);
      const lastEvents = this.capturedEvents.slice(-3).map(e => e.eventName);
      console.log(`  최근 이벤트: ${JSON.stringify(lastEvents)}`);
    } else {
      console.log('  ⚠️ 딜 카드를 찾을 수 없음');
    }
  }

  // 시나리오 3: 필터 사용 (Search)
  async scenario3_FilterUse() {
    console.log('\n📍 시나리오 3: 필터 사용');
    console.log('─'.repeat(40));
    console.log('  행동: 브랜드/통신사 필터 선택');
    console.log('  예상: Search 이벤트');

    await this.page.goto(`${BASE_URL}/home-v2`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForTimeout(2000);

    // 필터 영역 스크롤
    await this.page.evaluate(() => window.scrollTo(0, 200));
    await this.page.waitForTimeout(500);

    // 필터 버튼들 확인
    const filterArea = await this.page.$('.flex.gap-2, .flex.space-x-2');
    if (filterArea) {
      const buttons = await filterArea.$$('button');
      for (const btn of buttons) {
        const text = await btn.textContent();
        console.log(`  필터 버튼: ${text}`);
      }
    }
  }

  // 시나리오 4: Welcome 페이지 → 카카오 로그인 (Contact)
  async scenario4_WelcomeLogin() {
    console.log('\n📍 시나리오 4: Welcome 페이지 로그인 시도');
    console.log('─'.repeat(40));
    console.log('  URL: /welcome');
    console.log('  행동: 카카오 로그인 버튼 클릭');
    console.log('  예상: Contact 이벤트');

    await this.page.goto(`${BASE_URL}/welcome`, { waitUntil: 'domcontentloaded' });
    await this.waitForPixelLoad();
    await this.page.waitForTimeout(2000);

    // 카카오 로그인 버튼 클릭
    const kakaoBtn = await this.page.$('button:has-text("카카오")');
    if (kakaoBtn) {
      console.log('  카카오 버튼 발견 - 클릭');

      // 클릭 전 이벤트 수 기록
      const beforeCount = this.capturedEvents.length;

      // 클릭 (외부 리다이렉트는 무시)
      await Promise.race([
        kakaoBtn.click(),
        this.page.waitForTimeout(2000)
      ]);

      // 클릭 후 이벤트 확인
      await this.page.waitForTimeout(1000);
      const afterCount = this.capturedEvents.length;

      if (afterCount > beforeCount) {
        console.log('  ✅ 클릭 시 이벤트 발생 확인');
      }
    } else {
      console.log('  ⚠️ 카카오 버튼 없음');
    }
  }

  // 시나리오 5: 비교 페이지 (PageView - SPA)
  async scenario5_ComparePage() {
    console.log('\n📍 시나리오 5: 비교 페이지 방문');
    console.log('─'.repeat(40));
    console.log('  URL: /compare');
    console.log('  예상: PageView 이벤트');

    // 먼저 홈에 있다가
    await this.page.goto(`${BASE_URL}/home-v2`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForTimeout(1000);

    // 비교 페이지로 이동 (SPA 네비게이션 시뮬레이션)
    await this.page.goto(`${BASE_URL}/compare`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForTimeout(2000);
  }

  // 시나리오 6: 견적 신청 버튼 클릭 (InitiateCheckout)
  async scenario6_EstimateClick() {
    console.log('\n📍 시나리오 6: 견적 신청 버튼 클릭');
    console.log('─'.repeat(40));
    console.log('  행동: 딜 상세에서 CTA 버튼 클릭');
    console.log('  예상: InitiateCheckout 이벤트');

    // 딜 페이지로 직접 이동 (실제 딜 ID 필요)
    await this.page.goto(`${BASE_URL}/home-v2`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForTimeout(2000);

    // 딜 카드 클릭
    const dealCards = await this.page.$$('a[href*="/deal/"]');
    if (dealCards.length > 0) {
      await dealCards[0].click();
      await this.page.waitForTimeout(3000);

      // CTA 버튼 찾기
      const ctaBtn = await this.page.$('button:has-text("채팅"), button:has-text("견적"), button:has-text("알림")');
      if (ctaBtn) {
        console.log('  CTA 버튼 발견');
        const beforeCount = this.capturedEvents.length;
        await ctaBtn.click();
        await this.page.waitForTimeout(2000);
        const afterCount = this.capturedEvents.length;

        if (afterCount > beforeCount) {
          console.log('  ✅ CTA 클릭 시 이벤트 발생 확인');
        }
      }
    }
  }

  printSummary() {
    console.log('\n');
    console.log('═'.repeat(50));
    console.log('📊 테스트 결과 요약');
    console.log('═'.repeat(50));

    console.log(`\n총 캡처된 FB Pixel 이벤트: ${this.capturedEvents.length}개\n`);

    if (this.capturedEvents.length === 0) {
      console.log('⚠️ 이벤트가 캡처되지 않았습니다.');
      console.log('   가능한 원인:');
      console.log('   1. FB Pixel SDK가 로드되지 않음');
      console.log('   2. 환경변수 NEXT_PUBLIC_FB_PIXEL_ID 미설정');
      console.log('   3. 네트워크 문제 (광고 차단 등)');
      return;
    }

    // 이벤트 유형별 집계
    const eventCounts = {};
    this.capturedEvents.forEach(event => {
      eventCounts[event.eventName] = (eventCounts[event.eventName] || 0) + 1;
    });

    console.log('이벤트 유형별 횟수:');
    Object.entries(eventCounts).forEach(([name, count]) => {
      const expected = this.getExpectedEvents().includes(name) ? '✅' : '❓';
      console.log(`  ${expected} ${name}: ${count}회`);
    });

    // 예상 vs 실제 비교
    console.log('\n예상 이벤트 검증:');
    const expected = this.getExpectedEvents();
    expected.forEach(eventName => {
      const found = this.capturedEvents.some(e => e.eventName === eventName);
      console.log(`  ${found ? '✅' : '❌'} ${eventName}: ${found ? '발생함' : '발생하지 않음'}`);
    });

    // 결과 저장
    const fs = require('fs');
    const result = {
      timestamp: new Date().toISOString(),
      summary: {
        total: this.capturedEvents.length,
        byType: eventCounts
      },
      events: this.capturedEvents.map(e => ({
        eventName: e.eventName,
        customData: e.customData,
        timestamp: e.timestamp.toISOString()
      }))
    };

    fs.writeFileSync('./test-result.json', JSON.stringify(result, null, 2));
    console.log('\n📁 상세 결과: ./test-result.json');
  }

  getExpectedEvents() {
    return ['PageView', 'ViewContent', 'Search', 'Contact', 'InitiateCheckout'];
  }

  async cleanup() {
    if (this.browser) {
      await this.browser.close();
    }
  }

  async run() {
    try {
      await this.init();

      await this.scenario1_HomeFirstVisit();
      await this.scenario2_DealDetailView();
      await this.scenario3_FilterUse();
      await this.scenario4_WelcomeLogin();
      await this.scenario5_ComparePage();
      await this.scenario6_EstimateClick();

      this.printSummary();

      console.log('\n⏳ 5초 후 브라우저 종료...');
      console.log('   (브라우저에서 직접 확인하려면 Ctrl+C)');
      await this.page.waitForTimeout(5000);

    } catch (error) {
      console.error('\n❌ 테스트 오류:', error.message);
    } finally {
      await this.cleanup();
      console.log('\n✅ 테스트 완료\n');
    }
  }
}

// 실행
new FBPixelTester().run();
