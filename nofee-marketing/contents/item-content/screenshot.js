#!/usr/bin/env node
/**
 * HTML 파일을 각 상품별 이미지로 스크린샷 생성
 * - Puppeteer를 사용하여 각 .canvas 요소를 개별 이미지로 저장
 * - 3x4 비율: 1080x1440px
 * - 1x1 비율: 1080x1080px
 */

import puppeteer from 'puppeteer';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function captureScreenshots(htmlFile, outputDir, ratio) {
    console.log(`\n📸 ${ratio} 비율 스크린샷 생성 중...`);
    console.log(`   HTML: ${htmlFile}`);
    console.log(`   출력 폴더: ${outputDir}`);

    // 비율에 따른 크기 설정
    const dimensions = ratio === '1x1'
        ? { width: 1080, height: 1080 }
        : { width: 1080, height: 1440 };

    const browser = await puppeteer.launch({
        headless: 'new',
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--force-device-scale-factor=1']
    });

    try {
        const page = await browser.newPage();

        // 뷰포트 설정 (충분히 크게)
        await page.setViewport({
            width: 2000,
            height: 3000,
            deviceScaleFactor: 1
        });

        // HTML 파일 로드
        const htmlPath = `file://${path.resolve(htmlFile)}`;
        await page.goto(htmlPath, { waitUntil: 'networkidle0' });

        // 모든 .canvas 요소 찾기
        const canvasElements = await page.$$('.canvas');
        console.log(`   찾은 상품: ${canvasElements.length}개`);

        // 각 상품별로 스크린샷 생성
        for (let i = 0; i < canvasElements.length; i++) {
            const element = canvasElements[i];

            // 상품명 추출 (.product-title 텍스트)
            const productName = await element.$eval('.product-title', el => el.textContent.trim());

            // 파일명 생성 (공백 제거)
            const filename = `${productName.replace(/\s+/g, '_')}_${ratio}.png`;
            const outputPath = path.join(outputDir, filename);

            // 요소 스크린샷 (clip 사용하여 정확한 크기로)
            const boundingBox = await element.boundingBox();

            await page.screenshot({
                path: outputPath,
                clip: {
                    x: boundingBox.x,
                    y: boundingBox.y,
                    width: dimensions.width,
                    height: dimensions.height
                }
            });

            console.log(`   ✅ ${i + 1}/${canvasElements.length}: ${filename}`);
        }

        console.log(`\n✅ ${ratio} 비율 스크린샷 ${canvasElements.length}개 생성 완료!`);

    } catch (error) {
        console.error(`❌ 스크린샷 생성 중 오류:`, error);
        throw error;
    } finally {
        await browser.close();
    }
}

async function main() {
    if (process.argv.length < 5) {
        console.error('사용법: node screenshot.js <HTML파일> <출력폴더> <비율(3x4|1x1)>');
        process.exit(1);
    }

    const htmlFile = process.argv[2];
    const outputDir = process.argv[3];
    const ratio = process.argv[4];

    // 출력 폴더가 없으면 생성
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    await captureScreenshots(htmlFile, outputDir, ratio);
}

main().catch(error => {
    console.error('실행 중 오류:', error);
    process.exit(1);
});
