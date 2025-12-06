/**
 * 통신사별 API Fetcher
 *
 * 각 통신사 API에서 데이터를 가져와서 통합 스키마로 정규화
 *
 * 통신사별 특성:
 * - SKT: HTML 파싱 필요, 세션 쿠키 (일부)
 * - KT: JSON API, 세션 쿠키 필수, 페이지 12개 고정
 * - LGU+: JSON API, 인증 불필요, 검색 지원
 */

import { UnifiedDeviceData } from './google-sheets';

// 가입유형 코드 매핑
const JOIN_TYPE_MAP = {
  SKT: { '신규': '11', '번호이동': '20', '기기변경': '31' },
  KT: { '신규': '01', '번호이동': '02', '기기변경': '04' },
  'LGU+': { '신규': '3', '번호이동': '2', '기기변경': '1' },
} as const;

// 기기명 정규화 (한글 ↔ 영문 매핑)
const DEVICE_NAME_MAP: Record<string, string[]> = {
  '갤럭시': ['galaxy', 'Galaxy', '갤럭시'],
  'galaxy': ['galaxy', 'Galaxy', '갤럭시'],
  '아이폰': ['iphone', 'iPhone', '아이폰'],
  'iphone': ['iphone', 'iPhone', '아이폰'],
};

/**
 * 검색 키워드 추출
 */
function getSearchKeywords(deviceKeyword: string): string[] {
  const lowerKeyword = deviceKeyword.toLowerCase().trim();
  const keywords = new Set<string>([lowerKeyword]);

  const noSpace = lowerKeyword.replace(/\s+/g, '');
  keywords.add(noSpace);

  const baseKeyword = lowerKeyword.split(/[\s\d]/)[0];
  if (baseKeyword && DEVICE_NAME_MAP[baseKeyword]) {
    for (const mapped of DEVICE_NAME_MAP[baseKeyword]) {
      const mappedLower = mapped.toLowerCase();
      keywords.add(mappedLower);
      keywords.add(lowerKeyword.replace(baseKeyword, mappedLower));
      keywords.add(lowerKeyword.replace(baseKeyword, mappedLower).replace(/\s+/g, ''));
    }
  }

  const sMatch = lowerKeyword.match(/^s\s*(\d+)(\+)?$/) || lowerKeyword.match(/^s(\d+)(\+)?/);
  if (sMatch) {
    const num = sMatch[1];
    const hasPlus = !!sMatch[2];
    const variants = [
      `s${num}`,
      `s${num}+`,
      `갤럭시 s${num}`,
      `갤럭시 s${num}+`,
      `galaxy s${num}`,
      `galaxy s${num}+`,
    ];
    variants.forEach(v => keywords.add(v));
    if (!hasPlus) {
      keywords.add(`s${num}+`);
      keywords.add(`갤럭시 s${num}+`);
      keywords.add(`galaxy s${num}+`);
    }
  }

  const foldMatch = lowerKeyword.match(/(fold|폴드)\s*(\d+)?/);
  if (foldMatch) {
    const num = foldMatch[2];
    const foldVariants = ['z fold', 'zfold', '갤럭시 z 폴드', '갤럭시 폴드'];
    foldVariants.forEach(v => {
      keywords.add(num ? `${v} ${num}` : v);
      keywords.add(num ? `${v}${num}` : v.replace(/\s+/g, ''));
    });
  }

  const flipMatch = lowerKeyword.match(/(flip|플립)\s*(\d+)?/);
  if (flipMatch) {
    const num = flipMatch[2];
    const flipVariants = ['z flip', 'zflip', '갤럭시 z 플립', '갤럭시 플립'];
    flipVariants.forEach(v => {
      keywords.add(num ? `${v} ${num}` : v);
      keywords.add(num ? `${v}${num}` : v.replace(/\s+/g, ''));
    });
  }

  return Array.from(keywords);
}

type ModelInfo = {
  family: 'iphone' | 's' | 'fold' | 'flip' | null;
  number?: string;
  variant?: string;
};

function extractModelInfo(name: string): ModelInfo {
  const lower = name.toLowerCase();

  const iphone = lower.match(/(iphone|아이폰)\s*(\d+)\s*([a-z가-힣+]*)/);
  if (iphone) {
    return { family: 'iphone', number: iphone[2], variant: (iphone[3] || '').replace(/\s+/g, '') };
  }

  const s = lower.match(/(?:갤럭시\s*)?s\s*(\d+)\s*([a-z+가-힣]*)/);
  if (s) {
    return { family: 's', number: s[1], variant: (s[2] || '').replace(/\s+/g, '') };
  }

  const fold = lower.match(/(fold|폴드)\s*(\d*)/);
  if (fold) return { family: 'fold', number: fold[2] || undefined, variant: '' };

  const flip = lower.match(/(flip|플립)\s*(\d*)/);
  if (flip) return { family: 'flip', number: flip[2] || undefined, variant: '' };

  return { family: null };
}

function isDeviceMatch(itemName: string, deviceKeyword: string, filterKeywords: string[], itemDeviceCode?: string): boolean {
  const itemLower = itemName.toLowerCase();
  const keywordLower = deviceKeyword.toLowerCase();

  if (itemDeviceCode && deviceKeyword.toUpperCase() === itemDeviceCode.toUpperCase()) {
    return true;
  }

  const isDeviceCode = /^[A-Z]{1,3}[\d-]+[A-Z]*\d*$/i.test(deviceKeyword) || /^SM-/.test(deviceKeyword);
  if (isDeviceCode && itemDeviceCode) {
    return false;
  }

  if (!filterKeywords.some(kw => itemLower.includes(kw))) return false;

  const itemInfo = extractModelInfo(itemLower);
  const keywordInfo = extractModelInfo(keywordLower);

  if (keywordInfo.family && itemInfo.family && keywordInfo.family !== itemInfo.family) {
    return false;
  }

  if (keywordInfo.number && itemInfo.number && keywordInfo.number !== itemInfo.number) {
    return false;
  }

  const variant = keywordInfo.variant;
  if (variant) {
    if (!itemLower.includes(variant)) return false;
  }

  return true;
}

/**
 * LGU+ API에서 기기 데이터 가져오기
 */
export async function fetchLGUDevice(
  deviceKeyword: string,
  joinType: '신규' | '번호이동' | '기기변경',
  planCode?: string
): Promise<UnifiedDeviceData[]> {
  const joinTypeCode = JOIN_TYPE_MAP['LGU+'][joinType];
  const effectivePlanCode = planCode || 'LPZ0000409';
  const searchKeywords = getSearchKeywords(deviceKeyword);
  const apiKeywords = Array.from(new Set([deviceKeyword, ...searchKeywords.filter(k => /^[a-z]|^갤럭시/.test(k))])).slice(0, 5);

  const results: UnifiedDeviceData[] = [];

  for (const keyword of apiKeywords) {
    const searchUrl = new URL('https://www.lguplus.com/uhdc/fo/prdv/mdlbsufu/v2/mdlb-sufu-list');
    searchUrl.searchParams.set('urcMblPpCd', effectivePlanCode);
    searchUrl.searchParams.set('urcHphnEntrPsblKdCd', joinTypeCode);
    searchUrl.searchParams.set('shwd', keyword);
    searchUrl.searchParams.set('rowSize', '100');
    searchUrl.searchParams.set('sortOrd', '00');

    try {
      const response = await fetch(searchUrl.toString(), {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
          'Accept': 'application/json',
        },
      });

      if (!response.ok) continue;

      const data = await response.json();
      const items = data.dvicMdlbSufuDtoList || [];
      const filterKeywords = getSearchKeywords(deviceKeyword);

      for (const item of items) {
        const itemName = (item.urcTrmMdlNm || '').toLowerCase();
        const itemCode = item.urcTrmMdlCd || '';
        const matchesKeyword = isDeviceMatch(itemName, deviceKeyword, filterKeywords, itemCode);
        if (!matchesKeyword) continue;

        const id = `LGU+-${joinType}-${item.urcTrmMdlCd}-${effectivePlanCode}`;
        if (results.some(r => r.id === id)) continue;

        const msrp = item.dlvrPrc || 0;
        const carrierSubsidy = item.basicPlanPuanSuptAmt || 0;
        const additionalSubsidy = (item.basicPlanAddSuptAmt || 0) + (item.dsnwSupportAmt || 0);
        const installmentPrice = msrp - carrierSubsidy - additionalSubsidy;

        results.push({
          id,
          carrier: 'LGU+',
          joinType,
          discountType: '공시지원',
          deviceName: item.urcTrmMdlNm || '',
          deviceCode: item.urcTrmMdlCd || '',
          storage: extractStorage(item.urcTrmMdlNm || ''),
          color: '',
          planName: '',
          planCode: effectivePlanCode,
          planMonthlyFee: 85000,
          planMaintainMonth: 6,
          msrp,
          carrierSubsidy,
          additionalSubsidy,
          installmentPrice,
          cachedAt: new Date().toISOString(),
        });
      }

      if (results.length > 0) break;
    } catch (error) {
      console.error(`[LGU+] fetch error for "${keyword}":`, error);
    }
  }

  return results;
}

/**
 * SKT API에서 기기 데이터 가져오기
 */
export async function fetchSKTDevice(
  deviceKeyword: string,
  joinType: '신규' | '번호이동' | '기기변경',
  planCode?: string
): Promise<UnifiedDeviceData[]> {
  const joinTypeCode = JOIN_TYPE_MAP['SKT'][joinType];
  const effectivePlanCode = planCode || 'NA00007790';
  const noticeUrl = `https://shop.tworld.co.kr/notice?modelNwType=5G&scrbTypCd=${joinTypeCode}&prodId=${effectivePlanCode}&saleYn=Y`;

  try {
    const response = await fetch(noticeUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        'Accept': 'text/html',
      },
    });

    if (!response.ok) throw new Error(`SKT API error: ${response.status}`);

    const html = await response.text();
    const match = html.match(/_this\.products\s*=\s*parseObject\(\[([\s\S]+?)\]\);/);
    if (!match) return [];

    let items: Array<{
      productNm: string;
      productMem: string;
      modelCd: string;
      prodId: string;
      prodNm: string;
      factoryPrice: number;
      sumSaleAmt: number;
      dsnetSupmAmt: number;
    }>;

    try {
      items = JSON.parse(`[${match[1]}]`);
    } catch {
      return [];
    }

    const results: UnifiedDeviceData[] = [];
    const searchKeywords = getSearchKeywords(deviceKeyword);

    for (const item of items) {
      const itemName = `${item.productNm} ${item.productMem}`.toLowerCase();
      const itemCode = item.modelCd || '';
      if (!isDeviceMatch(itemName, deviceKeyword, searchKeywords, itemCode)) continue;

      const msrp = item.factoryPrice || 0;
      const carrierSubsidy = item.sumSaleAmt || 0;
      const additionalSubsidy = item.dsnetSupmAmt || 0;
      const installmentPrice = msrp - carrierSubsidy - additionalSubsidy;

      results.push({
        id: `SKT-${joinType}-${item.modelCd}-${effectivePlanCode}`,
        carrier: 'SKT',
        joinType,
        discountType: '공시지원',
        deviceName: `${item.productNm} ${item.productMem}`.trim(),
        deviceCode: item.modelCd || '',
        storage: item.productMem || '',
        color: '',
        planName: item.prodNm || '',
        planCode: item.prodId || effectivePlanCode,
        planMonthlyFee: 89000,
        planMaintainMonth: 6,
        msrp,
        carrierSubsidy,
        additionalSubsidy,
        installmentPrice,
        cachedAt: new Date().toISOString(),
      });
    }

    return results;
  } catch (error) {
    console.error('[SKT] fetch error:', error);
    throw error;
  }
}

/**
 * KT API에서 기기 데이터 가져오기
 */
export async function fetchKTDevice(
  deviceKeyword: string,
  joinType: '신규' | '번호이동' | '기기변경',
  planCode?: string
): Promise<UnifiedDeviceData[]> {
  const joinTypeCode = JOIN_TYPE_MAP['KT'][joinType];
  const effectivePlanCode = planCode || 'PL244N945';
  const discountOptionMap: Record<string, string> = { '신규': 'NT', '번호이동': 'MT', '기기변경': 'HT' };
  const discountOption = discountOptionMap[joinType];
  const searchKeywords = getSearchKeywords(deviceKeyword);
  const isDeviceCodeSearch = /^[A-Z]{1,3}[\d-]+[A-Z]*\d*$/i.test(deviceKeyword) || /^SM-/.test(deviceKeyword);

  try {
    const sessionResponse = await fetch('https://shop.kt.com/smart/supportAmtList.do?channel=VS', {
      headers: { 'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36' },
    });

    const cookies = sessionResponse.headers.get('set-cookie') || '';
    const results: UnifiedDeviceData[] = [];
    const maxPages = isDeviceCodeSearch ? 10 : 3;

    for (let pageNo = 1; pageNo <= maxPages; pageNo++) {
      const formData = new URLSearchParams({
        prodNm: 'mobile',
        prdcCd: effectivePlanCode,
        prodType: '30',
        deviceType: 'HDP',
        makrCd: '',
        sortProd: 'oBspnsrPunoDateDesc',
        spnsMonsType: '2',
        dscnOptnCd: discountOption,
        sbscTypeCd: joinTypeCode,
        pageNo: String(pageNo),
      });

      const apiResponse = await fetch('https://shop.kt.com/mobile/retvSuFuList.json', {
        method: 'POST',
        headers: {
          'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'application/json',
          'Referer': 'https://shop.kt.com/smart/supportAmtList.do?channel=VS',
          'Cookie': cookies,
        },
        body: formData.toString(),
      });

      if (!apiResponse.ok) break;

      const data = await apiResponse.json();
      const items = data.LIST_DATA || [];
      if (items.length === 0) break;

      for (const item of items) {
        const itemName = (item.petNm || '').toLowerCase();
        const itemCode = item.hndsetModelNm || '';
        if (!isDeviceMatch(itemName, deviceKeyword, searchKeywords, itemCode)) continue;

        const msrp = parseInt(item.ofwAmt) || 0;
        const carrierSubsidy = parseInt(item.ktSuprtAmt) || 0;
        const installmentPrice = msrp - carrierSubsidy;

        results.push({
          id: `KT-${joinType}-${item.hndsetModelNm}-${effectivePlanCode}`,
          carrier: 'KT',
          joinType,
          discountType: '공시지원',
          deviceName: item.petNm || '',
          deviceCode: item.hndsetModelNm || '',
          storage: extractStorage(item.petNm || ''),
          color: '',
          planName: item.pplNm || '',
          planCode: effectivePlanCode,
          planMonthlyFee: 90000,
          planMaintainMonth: 6,
          msrp,
          carrierSubsidy,
          additionalSubsidy: 0,
          installmentPrice,
          cachedAt: new Date().toISOString(),
        });
      }

      if (isDeviceCodeSearch && results.length > 0) break;
      const pageInfo = data.pageInfoBean || {};
      if (pageNo >= (pageInfo.totalPageCount || 1)) break;
    }

    return results;
  } catch (error) {
    console.error('[KT] fetch error:', error);
    throw error;
  }
}

/**
 * 통합 API: 통신사에 따라 적절한 fetcher 호출
 */
export async function fetchDeviceData(
  carrier: 'SKT' | 'KT' | 'LGU+',
  deviceCodeOrKeyword: string,
  joinType: '신규' | '번호이동' | '기기변경',
  planCode?: string
): Promise<UnifiedDeviceData[]> {
  switch (carrier) {
    case 'SKT': return fetchSKTDevice(deviceCodeOrKeyword, joinType, planCode);
    case 'KT': return fetchKTDevice(deviceCodeOrKeyword, joinType, planCode);
    case 'LGU+': return fetchLGUDevice(deviceCodeOrKeyword, joinType, planCode);
    default: throw new Error(`Unknown carrier: ${carrier}`);
  }
}

function extractStorage(deviceName: string): string {
  const match = deviceName.match(/(\d+)\s*(GB|TB)/i);
  return match ? `${match[1]}${match[2].toUpperCase()}` : '';
}
