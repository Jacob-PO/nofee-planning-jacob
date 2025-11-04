#!/usr/bin/env python3
import pymysql

DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

connection = pymysql.connect(**DB_CONFIG)

try:
    with connection.cursor(pymysql.cursors.DictCursor) as cursor:
        # tb_apply_phone 테이블에서 가격 관련 컬럼만 확인
        print("📋 tb_apply_phone 테이블 구조 (가격 관련 컬럼):")
        cursor.execute("DESCRIBE tb_apply_phone")
        for row in cursor.fetchall():
            if 'price' in row['Field'].lower() or 'device' in row['Field'].lower() or 'installment' in row['Field'].lower():
                print(f"  - {row['Field']} ({row['Type']})")

        # 실제 데이터 샘플 확인
        print("\n📱 아이폰 17 신청 데이터 샘플:")
        cursor.execute("""
            SELECT
                ap.apply_product_group_code,
                pgp.product_group_nm,
                ap.apply_month_price,
                ap.apply_month_device_price,
                ap.apply_installment_principal,
                ap.apply_month_rate_plan_price,
                ap.apply_device_promotion_price,
                ap.apply_device_price
            FROM tb_apply_phone ap
            JOIN tb_product_group_phone pgp ON ap.apply_product_group_code = pgp.product_group_code
            WHERE pgp.product_group_nm LIKE '%아이폰 17%'
            LIMIT 5
        """)
        for row in cursor.fetchall():
            print(f"\n제품: {row['product_group_nm']}")
            for key, value in row.items():
                if key != 'product_group_nm':
                    print(f"  {key}: {value:,}" if value else f"  {key}: None")

        # 캠페인 상품 테이블 확인
        print("\n\n📋 tb_campaign_phone 테이블 구조:")
        cursor.execute("DESCRIBE tb_campaign_phone")
        for row in cursor.fetchall():
            if 'price' in row['Field'].lower():
                print(f"  - {row['Field']} ({row['Type']})")

        # 캠페인 상품 데이터 확인
        print("\n📱 아이폰 17 캠페인 데이터:")
        cursor.execute("""
            SELECT
                cp.campaign_phone_no,
                cp.campaign_phone_nm,
                cp.product_group_code,
                pgp.product_group_nm,
                cp.release_price,
                cp.phone_price,
                cp.promotion_price,
                cp.support_price
            FROM tb_campaign_phone cp
            JOIN tb_product_group_phone pgp ON cp.product_group_code = pgp.product_group_code
            WHERE pgp.product_group_nm LIKE '%아이폰 17%'
            AND cp.deleted_yn = 'N'
            LIMIT 5
        """)
        for row in cursor.fetchall():
            print(f"\n캠페인: {row['campaign_phone_nm']}")
            for key, value in row.items():
                if key not in ['campaign_phone_no', 'campaign_phone_nm', 'product_group_code']:
                    print(f"  {key}: {value:,}" if value and isinstance(value, (int, float)) else f"  {key}: {value}")

finally:
    connection.close()
