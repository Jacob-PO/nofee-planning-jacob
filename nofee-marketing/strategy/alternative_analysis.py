#!/usr/bin/env python3
"""
노피 데이터 분석 - 0201006 단계 외 다른 인사이트 찾기
"""

import pymysql
import pandas as pd
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4',
}

def q(query):
    conn = pymysql.connect(**DB_CONFIG)
    try:
        return pd.read_sql(query, conn)
    finally:
        conn.close()

print("\n" + "="*100)
print("노피 - 실행 가능한 기회 찾기")
print("="*100 + "\n")

# 1. 가입만 하고 신청 안 한 2,557명 심층 분석
print("[1] 미신청자 2,557명 - 왜 신청 안 했나?")
print("-"*100)

# 가입 경로별 분석
signup_source = q("""
SELECT
    create_type_code,
    COUNT(*) as count
FROM tb_user u
LEFT JOIN tb_apply_phone a ON u.user_no = a.user_no AND a.deleted_yn = 'N'
WHERE u.deleted_yn = 'N' AND a.apply_no IS NULL
GROUP BY create_type_code
ORDER BY count DESC
""")
print("가입 경로별:")
print(signup_source)

# 가입 후 경과 시간
non_apply_time = q("""
SELECT
    CASE
        WHEN DATEDIFF(NOW(), created_at) <= 1 THEN '1일이내'
        WHEN DATEDIFF(NOW(), created_at) <= 7 THEN '1주이내'
        WHEN DATEDIFF(NOW(), created_at) <= 30 THEN '1개월이내'
        WHEN DATEDIFF(NOW(), created_at) <= 90 THEN '3개월이내'
        ELSE '3개월이상'
    END as period,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (
        SELECT COUNT(*) FROM tb_user u
        LEFT JOIN tb_apply_phone a ON u.user_no = a.user_no AND a.deleted_yn = 'N'
        WHERE u.deleted_yn = 'N' AND a.apply_no IS NULL
    ), 2) as percentage
FROM tb_user u
LEFT JOIN tb_apply_phone a ON u.user_no = a.user_no AND a.deleted_yn = 'N'
WHERE u.deleted_yn = 'N' AND a.apply_no IS NULL
GROUP BY period
ORDER BY count DESC
""")
print("\n가입 후 경과 시간:")
print(non_apply_time)

# 2. 재신청 패턴 - 중요!
print("\n[2] 사용자별 신청 횟수 - 재구매 가능성")
print("-"*100)
repeat = q("""
SELECT
    user_no,
    COUNT(*) as apply_count,
    MIN(apply_at) as first_apply,
    MAX(apply_at) as last_apply,
    DATEDIFF(MAX(apply_at), MIN(apply_at)) as days_between,
    SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) as completed_count
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY user_no
HAVING COUNT(*) >= 2
ORDER BY apply_count DESC
LIMIT 20
""")
print("2회 이상 신청한 사용자 (Top 20):")
print(repeat)

# 재신청율
repeat_stats = q("""
SELECT
    '1회만' as type,
    COUNT(*) as user_count
FROM (
    SELECT user_no, COUNT(*) as cnt
    FROM tb_apply_phone
    WHERE deleted_yn = 'N'
    GROUP BY user_no
    HAVING cnt = 1
) sub
UNION ALL
SELECT
    '2회이상' as type,
    COUNT(*) as user_count
FROM (
    SELECT user_no, COUNT(*) as cnt
    FROM tb_apply_phone
    WHERE deleted_yn = 'N'
    GROUP BY user_no
    HAVING cnt >= 2
) sub
""")
print("\n재신청 비율:")
print(repeat_stats)

# 3. 완료자 분석 - 성공 패턴 찾기
print("\n[3] 계약 완료자 1,018명 분석 - 성공 패턴")
print("-"*100)

# 완료까지 걸린 시간
completion_time = q("""
SELECT
    CASE
        WHEN TIMESTAMPDIFF(HOUR, apply_at, completed_at) <= 1 THEN '1시간이내'
        WHEN TIMESTAMPDIFF(HOUR, apply_at, completed_at) <= 24 THEN '1일이내'
        WHEN TIMESTAMPDIFF(DAY, apply_at, completed_at) <= 7 THEN '1주이내'
        WHEN TIMESTAMPDIFF(DAY, apply_at, completed_at) <= 30 THEN '1개월이내'
        ELSE '1개월이상'
    END as period,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM tb_apply_phone WHERE step_code = '0201007'), 2) as percentage
FROM tb_apply_phone
WHERE step_code = '0201007' AND completed_at IS NOT NULL
GROUP BY period
ORDER BY count DESC
""")
print("신청부터 완료까지 걸린 시간:")
print(completion_time)

# 완료자의 통신사 선호도
completed_carrier = q("""
SELECT
    apply_carrier_code,
    COUNT(*) as count,
    ROUND(AVG(apply_margin), 2) as avg_margin,
    ROUND(AVG(apply_month_price), 0) as avg_monthly_price
FROM tb_apply_phone
WHERE step_code = '0201007' AND deleted_yn = 'N'
GROUP BY apply_carrier_code
ORDER BY count DESC
""")
print("\n완료자의 통신사별 선택:")
print(completed_carrier)

# 4. 최근 30일 트렌드
print("\n[4] 최근 30일 트렌드 - 성장/하락 확인")
print("-"*100)

daily_trend = q("""
SELECT
    DATE(apply_at) as date,
    COUNT(*) as applies,
    SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as conversion_rate
FROM tb_apply_phone
WHERE apply_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
    AND deleted_yn = 'N'
GROUP BY DATE(apply_at)
ORDER BY date DESC
LIMIT 30
""")
print("일별 신청 및 전환율:")
print(daily_trend)

# 5. 판매점 심층 분석
print("\n[5] 판매점 - 신청 많은데 전환 안 되는 곳")
print("-"*100)

underperforming = q("""
SELECT
    s.store_no,
    COUNT(a.apply_no) as total_applies,
    SUM(CASE WHEN a.step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN a.step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(a.apply_no), 2) as conversion_rate,
    COUNT(a.apply_no) - SUM(CASE WHEN a.step_code = '0201007' THEN 1 ELSE 0 END) as lost_opportunity
FROM tb_store s
JOIN tb_apply_phone a ON s.store_no = a.store_no AND a.deleted_yn = 'N'
WHERE s.deleted_yn = 'N'
GROUP BY s.store_no
HAVING COUNT(a.apply_no) >= 50
ORDER BY lost_opportunity DESC
""")
print("신청 많은데 전환율 낮은 판매점:")
print(underperforming)

# 6. 상품 분석
print("\n[6] 인기 상품 vs 전환율 높은 상품")
print("-"*100)

popular_products = q("""
SELECT
    apply_product_group_code,
    COUNT(*) as applies,
    SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as conversion_rate
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY apply_product_group_code
HAVING COUNT(*) >= 10
ORDER BY applies DESC
LIMIT 20
""")
print("상품별 신청 및 전환율:")
print(popular_products)

# 7. 가입 유형별 전환율
print("\n[7] 가입 유형별 전환율 (신규/번호이동/기기변경)")
print("-"*100)

join_type = q("""
SELECT
    apply_join_type_code,
    COUNT(*) as applies,
    SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as conversion_rate
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY apply_join_type_code
ORDER BY applies DESC
""")
print("가입 유형별:")
print(join_type)

# 8. 지원 유형별 전환율
print("\n[8] 지원 유형별 전환율 (공시/선택/완납)")
print("-"*100)

support_type = q("""
SELECT
    apply_support_type_code,
    COUNT(*) as applies,
    SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as conversion_rate,
    ROUND(AVG(apply_margin), 2) as avg_margin
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY apply_support_type_code
ORDER BY applies DESC
""")
print("지원 유형별:")
print(support_type)

# 9. 리뷰 작성 여부와 재구매
print("\n[9] 리뷰 작성자의 재구매율")
print("-"*100)

review_impact = q("""
SELECT
    '리뷰작성' as type,
    COUNT(DISTINCT user_no) as users,
    COUNT(*) / COUNT(DISTINCT user_no) as avg_applies_per_user
FROM tb_apply_phone
WHERE deleted_yn = 'N' AND review_yn = 'Y'
UNION ALL
SELECT
    '리뷰미작성' as type,
    COUNT(DISTINCT user_no) as users,
    COUNT(*) / COUNT(DISTINCT user_no) as avg_applies_per_user
FROM tb_apply_phone
WHERE deleted_yn = 'N' AND review_yn = 'N'
""")
print("리뷰 작성 여부에 따른 재신청:")
print(review_impact)

print("\n" + "="*100)
print("분석 완료 - 실행 가능한 인사이트 도출")
print("="*100 + "\n")
