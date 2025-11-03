#!/usr/bin/env python3
"""
노피 데이터 심층 분석
진짜 문제를 찾고 실행 가능한 해결책을 도출
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
print("노피 데이터 심층 분석 - 실행 가능한 인사이트 도출")
print("="*100 + "\n")

# 1. 핵심 지표
print("[1] 현재 상태 스냅샷")
print("-"*100)
snapshot = q("""
SELECT
    (SELECT COUNT(*) FROM tb_user WHERE deleted_yn = 'N') as total_users,
    (SELECT COUNT(*) FROM tb_store WHERE deleted_yn = 'N') as total_stores,
    (SELECT COUNT(*) FROM tb_apply_phone WHERE deleted_yn = 'N') as total_applies,
    (SELECT COUNT(*) FROM tb_apply_phone WHERE deleted_yn = 'N' AND step_code = '0201007') as completed,
    (SELECT COUNT(*) FROM tb_apply_phone WHERE deleted_yn = 'N' AND step_code != '0201007') as incomplete
""")
print(snapshot)

total_applies = snapshot['total_applies'].values[0]
completed = snapshot['completed'].values[0]
incomplete = snapshot['incomplete'].values[0]

print(f"\n전환율: {completed/total_applies*100:.2f}%")
print(f"미완료율: {incomplete/total_applies*100:.2f}%")

# 2. 단계별 분석
print("\n[2] 단계별 이탈 분석")
print("-"*100)
steps = q("""
SELECT
    step_code,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM tb_apply_phone WHERE deleted_yn = 'N'), 2) as percentage
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY step_code
ORDER BY count DESC
""")
print(steps)

# 3. 판매점 성과
print("\n[3] 판매점 전환율 (신청 5건 이상만)")
print("-"*100)
stores = q("""
SELECT
    s.store_no,
    COUNT(a.apply_no) as applies,
    SUM(CASE WHEN a.step_code = '0201007' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN a.step_code = '0201007' THEN 1 ELSE 0 END) * 100.0 / COUNT(a.apply_no), 2) as conversion_rate
FROM tb_store s
JOIN tb_apply_phone a ON s.store_no = a.store_no AND a.deleted_yn = 'N'
WHERE s.deleted_yn = 'N'
GROUP BY s.store_no
HAVING COUNT(a.apply_no) >= 5
ORDER BY conversion_rate DESC
LIMIT 20
""")
print(stores)

# 4. 시간대별 패턴
print("\n[4] 시간대별 신청 및 전환율")
print("-"*100)
hourly = q("""
SELECT
    HOUR(apply_at) as hour,
    COUNT(*) as applies,
    ROUND(AVG(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100, 2) as conversion_rate
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY HOUR(apply_at)
ORDER BY applies DESC
LIMIT 10
""")
print(hourly)

# 5. 요일별 패턴
print("\n[5] 요일별 신청 및 전환율")
print("-"*100)
weekday = q("""
SELECT
    CASE DAYOFWEEK(apply_at)
        WHEN 1 THEN '일'
        WHEN 2 THEN '월'
        WHEN 3 THEN '화'
        WHEN 4 THEN '수'
        WHEN 5 THEN '목'
        WHEN 6 THEN '금'
        WHEN 7 THEN '토'
    END as day,
    COUNT(*) as applies,
    ROUND(AVG(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100, 2) as conversion_rate
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY DAYOFWEEK(apply_at)
ORDER BY DAYOFWEEK(apply_at)
""")
print(weekday)

# 6. 미완료 신청 기간별
print("\n[6] 미완료 신청이 얼마나 오래 방치되었나")
print("-"*100)
abandoned = q("""
SELECT
    CASE
        WHEN DATEDIFF(NOW(), apply_at) <= 7 THEN '1주이내'
        WHEN DATEDIFF(NOW(), apply_at) <= 30 THEN '1개월이내'
        WHEN DATEDIFF(NOW(), apply_at) <= 90 THEN '3개월이내'
        ELSE '3개월이상'
    END as period,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM tb_apply_phone WHERE deleted_yn = 'N' AND step_code != '0201007'), 2) as percentage
FROM tb_apply_phone
WHERE deleted_yn = 'N' AND step_code != '0201007'
GROUP BY period
ORDER BY count DESC
""")
print(abandoned)

# 7. 미신청자 분석
print("\n[7] 가입만 하고 신청 안 한 사람들")
print("-"*100)
non_applicants = q("""
SELECT
    CASE
        WHEN DATEDIFF(NOW(), u.created_at) <= 7 THEN '최근1주'
        WHEN DATEDIFF(NOW(), u.created_at) <= 30 THEN '최근1개월'
        WHEN DATEDIFF(NOW(), u.created_at) <= 90 THEN '최근3개월'
        ELSE '3개월이상'
    END as period,
    COUNT(*) as count
FROM tb_user u
LEFT JOIN tb_apply_phone a ON u.user_no = a.user_no AND a.deleted_yn = 'N'
WHERE u.deleted_yn = 'N' AND a.apply_no IS NULL
GROUP BY period
ORDER BY count DESC
""")
print(non_applicants)

# 8. 통신사별 전환율
print("\n[8] 통신사별 신청 및 전환율")
print("-"*100)
carriers = q("""
SELECT
    apply_carrier_code,
    COUNT(*) as applies,
    ROUND(AVG(CASE WHEN step_code = '0201007' THEN 1 ELSE 0 END) * 100, 2) as conversion_rate
FROM tb_apply_phone
WHERE deleted_yn = 'N'
GROUP BY apply_carrier_code
ORDER BY applies DESC
""")
print(carriers)

print("\n" + "="*100)
print("분석 완료")
print("="*100 + "\n")
