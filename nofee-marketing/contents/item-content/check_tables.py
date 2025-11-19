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
        # tb_product_group_phone 테이블 구조 확인
        print("📋 tb_product_group_phone 테이블 구조:")
        cursor.execute("DESCRIBE tb_product_group_phone")
        for row in cursor.fetchall():
            print(f"  - {row['Field']} ({row['Type']})")

        # 샘플 데이터 확인
        print("\n📱 아이폰 제품 샘플:")
        cursor.execute("""
            SELECT *
            FROM tb_product_group_phone
            WHERE product_group_nm LIKE '%아이폰%' OR product_group_nm LIKE '%iPhone%'
            LIMIT 3
        """)
        for row in cursor.fetchall():
            print(f"\n제품명: {row['product_group_nm']}")
            for key, value in row.items():
                print(f"  {key}: {value}")

finally:
    connection.close()
