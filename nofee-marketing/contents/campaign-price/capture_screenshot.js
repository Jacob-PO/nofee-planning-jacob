#!/usr/bin/env node
'use strict';

const path = require('path');
const fs = require('fs');
const puppeteer = require('puppeteer');

async function capture(htmlPath, outputPath) {
  const resolvedHtmlPath = path.resolve(htmlPath);
  const resolvedOutputPath = path.resolve(outputPath);

  if (!fs.existsSync(resolvedHtmlPath)) {
    throw new Error(`HTML 파일을 찾을 수 없습니다: ${resolvedHtmlPath}`);
  }

  fs.mkdirSync(path.dirname(resolvedOutputPath), { recursive: true });

  const browser = await puppeteer.launch({
    headless: 'new',
    defaultViewport: { width: 1000, height: 1000, deviceScaleFactor: 2 },
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  try {
    const page = await browser.newPage();
    await page.goto(`file://${resolvedHtmlPath}`, { waitUntil: 'networkidle0' });
    await new Promise((resolve) => setTimeout(resolve, 500));

    const container = await page.$('.container');

    if (container) {
      await container.screenshot({ path: resolvedOutputPath });
    } else {
      await page.screenshot({ path: resolvedOutputPath, fullPage: false });
    }

    console.log(`📸 스크린샷 저장 완료: ${resolvedOutputPath}`);
  } finally {
    await browser.close();
  }
}

(async () => {
  const [, , htmlArg, outputArg] = process.argv;
  const defaultHtml = path.join(__dirname, 'output', 'campaign_price_toss.html');
  const defaultOutput = path.join(__dirname, 'output', 'campaign_price_toss.png');

  await capture(htmlArg || defaultHtml, outputArg || defaultOutput);
})().catch((err) => {
  console.error('스크린샷 생성 실패:', err.message);
  process.exit(1);
});
