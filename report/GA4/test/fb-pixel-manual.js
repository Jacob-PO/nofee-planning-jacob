/**
 * FB Pixel 수동 테스트 - 브라우저 열어두기
 * 직접 로그인/회원가입/견적 완료 테스트용
 *
 * 실행: node fb-pixel-manual.js
 * 종료: Ctrl+C
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';

async function runManualTest() {
  console.log('\n🚀 FB Pixel 수동 테스트 시작');
  console.log('═'.repeat(50));
  console.log('브라우저에서 직접 테스트하세요.');
  console.log('FB Pixel 이벤트가 실시간으로 캡처됩니다.');
  console.log('종료하려면 Ctrl+C를 누르세요.');
  console.log('═'.repeat(50));

  const browser = await chromium.launch({
    headless: false,
    slowMo: 100
  });

  const context = await browser.newContext({
    viewport: { width: 430, height: 932 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15'
  });

  const page = await context.newPage();

  const capturedEvents = [];

  // FB Pixel 요청 캡처
  page.on('request', (request) => {
    const url = request.url();

    if (url.includes('facebook.com') || url.includes('facebook.net')) {
      try {
        const urlObj = new URL(url);

        if (urlObj.pathname.includes('fbevents.js')) {
          return; // SDK 로드 무시
        }

        const eventName = urlObj.searchParams.get('ev');
        if (!eventName) return;

        const cd = urlObj.searchParams.get('cd');
        let customData = null;
        if (cd) {
          try {
            customData = JSON.parse(cd);
          } catch (e) {}
        }

        capturedEvents.push({
          eventName,
          customData,
          timestamp: new Date()
        });

        console.log(`\n✅ FB Pixel: ${eventName}`);
        if (customData) {
          console.log(`   Data: ${JSON.stringify(customData, null, 2)}`);
        }
      } catch (e) {}
    }

    if (url.includes('/api/fb-conversion')) {
      console.log(`\n📤 CAPI 요청 발송`);
    }
  });

  // 콘솔 로그 캡처
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('[FB') || text.includes('Login') ||
        text.includes('Registration') || text.includes('Lead') ||
        text.includes('Search')) {
      console.log(`📋 ${text}`);
    }
  });

  // 홈페이지로 이동
  await page.goto(`${BASE_URL}/home-v2`);
  console.log('\n📍 홈 페이지 로드 완료');
  console.log('─'.repeat(40));
  console.log('테스트할 시나리오:');
  console.log('1. 카카오 로그인 → Login 이벤트');
  console.log('2. 신규 회원가입 → CompleteRegistration 이벤트');
  console.log('3. 딜 상세 → CTA 클릭 → 견적 완료 → Lead 이벤트');
  console.log('4. 필터 사용 → Search 이벤트');
  console.log('─'.repeat(40));

  // 무한 대기 (사용자가 Ctrl+C로 종료할 때까지)
  await new Promise(() => {});
}

runManualTest().catch(console.error);
