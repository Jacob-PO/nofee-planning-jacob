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
        print("📋 tb_pricetable_store_phone_col 테이블 구조:")
        cursor.execute("DESCRIBE tb_pricetable_store_phone_col")
        for row in cursor.fetchall():
            print(f"  - {row['Field']} ({row['Type']})")

        print("\n\n📋 tb_pricetable_store_phone_raw 테이블 구조:")
        cursor.execute("DESCRIBE tb_pricetable_store_phone_raw")
        for row in cursor.fetchall():
            print(f"  - {row['Field']} ({row['Type']})")

        print("\n\n📱 아이폰 17 프로 시세표 샘플 (col):")
        cursor.execute("""
            SELECT *
            FROM tb_pricetable_store_phone_col
            WHERE product_group_code = 'AP-A-17'
            LIMIT 2
        """)
        for idx, row in enumerate(cursor.fetchall(), 1):
            print(f"\n샘플 {idx}:")
            for key, value in row.items():
                if value is not None:
                    print(f"  {key}: {value}")

finally:
    connection.close()
