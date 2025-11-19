#!/usr/bin/env python3
"""
노피 회사소개서용 통합 데이터 분석 스크립트
DB + GA4 + 상품/매장 데이터 통합 분석
"""

import json
from pathlib import Path
from datetime import datetime

def load_latest_file(data_dir, prefix):
    """특정 prefix로 시작하는 가장 최근 파일 로드"""
    files = sorted(data_dir.glob(f"{prefix}_*.json"), reverse=True)
    if not files:
        raise FileNotFoundError(f"{prefix}로 시작하는 파일을 찾을 수 없습니다.")

    with open(files[0], 'r', encoding='utf-8') as f:
        return json.load(f)

def main():
    """메인 분석 함수"""
    print("=" * 60)
    print("🎯 노피 회사소개서 통합 데이터 분석")
    print("=" * 60)

    # 데이터 디렉토리
    script_dir = Path(__file__).parent
    data_dir = script_dir.parent / 'data'

    try:
        # 1. 데이터 로드
        print("\n📥 데이터 로드 중...")
        db_data = load_latest_file(data_dir, 'db_data')
        ga4_data = load_latest_file(data_dir, 'ga4_data')
        product_data = load_latest_file(data_dir, 'product_store_data')

        print(f"   ✓ DB 데이터 로드 완료")
        print(f"   ✓ GA4 데이터 로드 완료")
        print(f"   ✓ 상품/매장 데이터 로드 완료")

        # 2. 통합 분석
        print("\n" + "=" * 60)
        print("📊 통합 데이터 분석")
        print("=" * 60)

        comprehensive_summary = {
            "metadata": {
                "generated_at": datetime.now().isoformat(),
                "purpose": "회사소개서 작성용 통합 데이터 분석",
                "version": "2.0"
            },
            "비즈니스_핵심_지표": {
                "고객": {
                    "총_가입자": f"{db_data['total_users']:,}명",
                    "총_신청": f"{db_data['total_applications']:,}건",
                    "개통_완료": f"{db_data['total_completed']:,}건",
                    "최근_30일_가입": f"{db_data['recent_30d_users']:,}명",
                    "최근_30일_신청": f"{db_data['recent_30d_applications']:,}건",
                },
                "매장_파트너": {
                    "총_등록_매장": f"{db_data['total_stores']:,}개",
                    "활성_가격표_매장": f"{product_data['summary']['total_active_stores']}개",
                    "매장_구매_확정": f"{db_data['total_store_purchases']:,}건",
                },
                "상품": {
                    "활성_상품_그룹": f"{product_data['summary']['total_active_products']}개",
                    "실제_판매중_상품": f"{product_data['summary']['total_unique_products_with_prices']}개",
                    "총_가격표_항목": f"{product_data['summary']['total_price_items']}개",
                },
                "지역_커버리지": {
                    "서비스_지역": f"{product_data['summary']['total_regions']}개 지역",
                },
                "리뷰_신뢰도": {
                    "총_리뷰": f"{db_data['reviews']['total_reviews']:,}건",
                    "실제_리뷰": f"{db_data['reviews']['real_reviews']['total']:,}건",
                }
            },
            "웹_트래픽_지표": {
                "최근_12개월": {
                    "총_세션": f"{ga4_data['overall_metrics']['sessions']:,}회",
                    "총_사용자": f"{ga4_data['overall_metrics']['total_users']:,}명",
                    "페이지뷰": f"{ga4_data['overall_metrics']['page_views']:,}회",
                },
                "일평균": {
                    "세션": f"{ga4_data['daily_average']['avg_sessions']:.1f}회",
                    "사용자": f"{ga4_data['daily_average']['avg_users']:.1f}명",
                    "페이지뷰": f"{ga4_data['daily_average']['avg_page_views']:.1f}회",
                },
                "참여도": {
                    "세션당_페이지뷰": f"{ga4_data['overall_metrics']['page_views'] / ga4_data['overall_metrics']['sessions']:.1f}페이지",
                    "평균_세션_시간": f"{ga4_data['overall_metrics']['avg_session_duration']:.0f}초 ({ga4_data['overall_metrics']['avg_session_duration']/60:.1f}분)",
                    "이탈률": f"{ga4_data['overall_metrics']['bounce_rate']:.2%}",
                },
                "모바일_최적화": {
                    "모바일_비율": f"{sum(d['sessions'] for d in ga4_data['device_category'] if d['device'] == 'mobile') / sum(d['sessions'] for d in ga4_data['device_category']) * 100:.1f}%",
                }
            },
            "전환_퍼널": {
                "신청_구매_전환율": f"{db_data['conversion_rates']['application_to_purchase']}%",
                "신청_개통_전환율": f"{db_data['conversion_rates']['application_to_completion']}%",
                "구매_개통_전환율": f"{db_data['conversion_rates']['purchase_to_completion']}%",
            },
            "상위_인기_상품": [
                {
                    "순위": idx,
                    "상품명": item['device'],
                    "가격표_수": f"{item['price_count']}개",
                    "판매_매장_수": f"{item['store_count']}개",
                }
                for idx, item in enumerate(product_data['summary']['top_products'][:10], 1)
            ],
            "상위_활성_매장": [
                {
                    "순위": idx,
                    "매장_번호": item['store_no'],
                    "판매_상품_수": f"{item['product_count']}개",
                }
                for idx, item in enumerate(product_data['summary']['top_stores'][:10], 1)
            ],
            "상위_서비스_지역": [
                {
                    "순위": idx,
                    "지역": item['region'],
                    "매장_수": f"{item['store_count']}개",
                    "상품_수": f"{item['product_count']}개",
                }
                for idx, item in enumerate(product_data['summary']['top_regions'][:10], 1)
            ]
        }

        # 3. 회사소개서 핵심 메시지 생성
        print("\n" + "=" * 60)
        print("✨ 회사소개서 핵심 메시지 (업데이트)")
        print("=" * 60)

        key_messages = {
            "1. 검증된 시장 적합성 (PMF)": [
                f"✓ 누적 {db_data['total_applications']:,}건의 실제 고객 신청",
                f"✓ {db_data['total_stores']:,}개 매장 파트너십 구축",
                f"✓ {db_data['reviews']['total_reviews']:,}건의 고객 리뷰 확보",
                f"✓ {product_data['summary']['total_active_products']}개 활성 상품 라인업",
                f"✓ {product_data['summary']['total_regions']}개 지역 서비스 커버리지",
            ],
            "2. 안정적인 트래픽": [
                f"✓ 일평균 {ga4_data['daily_average']['avg_sessions']:.0f}회 세션 (월 {ga4_data['daily_average']['avg_sessions'] * 30:.0f}회)",
                f"✓ 일평균 {ga4_data['daily_average']['avg_users']:.0f}명 사용자 (월 {ga4_data['daily_average']['avg_users'] * 30:.0f}명)",
                f"✓ 모바일 중심 ({sum(d['sessions'] for d in ga4_data['device_category'] if d['device'] == 'mobile') / sum(d['sessions'] for d in ga4_data['device_category']) * 100:.0f}%) MZ세대 타겟팅 성공",
            ],
            "3. 높은 참여도": [
                f"✓ 세션당 {ga4_data['overall_metrics']['page_views'] / ga4_data['overall_metrics']['sessions']:.1f} 페이지뷰",
                f"✓ 평균 세션 시간 {ga4_data['overall_metrics']['avg_session_duration']/60:.1f}분",
                f"✓ 낮은 이탈률 {ga4_data['overall_metrics']['bounce_rate']:.2%}",
            ],
            "4. 효과적인 양면 매칭": [
                f"✓ {db_data['conversion_rates']['application_to_purchase']}%의 신청→매장구매 전환율",
                f"✓ 양면 시장에서 공급자 확보 성공 ({product_data['summary']['total_active_stores']}개 활성 매장)",
                f"✓ {db_data['total_store_purchases']:,}건의 매장 구매 확정",
                f"✓ {product_data['summary']['total_price_items']}개 실시간 가격 정보 제공",
            ],
            "5. 지속적인 성장": [
                f"✓ 최근 30일 {db_data['recent_30d_users']:,}명 신규 가입",
                f"✓ 최근 30일 {db_data['recent_30d_applications']:,}건 신규 신청",
                f"✓ 활발한 제품 개발 (197+ 커밋)",
                f"✓ {product_data['summary']['total_active_products']}개 최신 기종 상시 업데이트",
            ],
            "6. 다양한 상품 포트폴리오": [
                f"✓ {product_data['summary']['total_active_products']}개 활성 상품 그룹",
                f"✓ 실제 판매중인 {product_data['summary']['total_unique_products_with_prices']}개 기종",
                f"✓ 3대 통신사(SKT, KT, LG) 전체 커버",
                f"✓ 신규/번호이동/기기변경 모든 가입유형 지원",
            ]
        }

        comprehensive_summary["회사소개서_핵심_메시지"] = key_messages

        for category, messages in key_messages.items():
            print(f"\n{category}")
            for msg in messages:
                print(f"  {msg}")

        # 4. 개선 포인트 및 성장 잠재력
        print("\n" + "=" * 60)
        print("📈 개선 포인트 및 성장 잠재력")
        print("=" * 60)

        growth_potential = {
            "전환율_최적화": {
                "현재": f"{db_data['conversion_rates']['application_to_completion']}%",
                "목표": "5%+",
                "개선_여지": "2배 이상 성장 가능",
            },
            "매장_네트워크_확대": {
                "현재": f"{db_data['total_stores']}개",
                "활성": f"{product_data['summary']['total_active_stores']}개",
                "목표": "200개+",
                "커버리지": f"현재 {product_data['summary']['total_regions']}개 지역",
            },
            "마케팅_강화": {
                "현재_상태": "유료 광고 최소화 (Organic 중심)",
                "성장_전략": "퍼포먼스 마케팅 투입 시 3배+ 성장 가능",
                "근거": f"현재 일평균 {ga4_data['daily_average']['avg_sessions']:.0f}회 세션을 광고 없이 확보",
            },
            "상품_확대": {
                "현재": f"{product_data['summary']['total_active_products']}개 활성 상품",
                "잠재력": "중고폰, 태블릿, 웨어러블 등 확장 가능",
            }
        }

        comprehensive_summary["성장_잠재력"] = growth_potential

        for category, data in growth_potential.items():
            print(f"\n{category}:")
            for key, value in data.items():
                print(f"  • {key}: {value}")

        # 5. 데이터 저장
        output_path = data_dir / f"comprehensive_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(comprehensive_summary, f, ensure_ascii=False, indent=2)

        print(f"\n💾 통합 분석 보고서 저장: {output_path}")

        print("\n" + "=" * 60)
        print("✅ 통합 데이터 분석 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
