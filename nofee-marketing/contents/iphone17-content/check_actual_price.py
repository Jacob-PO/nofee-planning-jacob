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
        print("📱 아이폰 17 프로 시세표 실제 데이터:")

        # 먼저 product_group_code 확인
        cursor.execute("""
            SELECT product_group_code, product_group_nm
            FROM tb_product_group_phone
            WHERE product_group_nm LIKE '%아이폰 17 프로%'
        """)
        products = cursor.fetchall()
        print("\n상품 코드:")
        for p in products:
            print(f"  - {p['product_group_code']}: {p['product_group_nm']}")

        # 실제 시세표 데이터
        if products:
            product_code = products[0]['product_group_code']
            cursor.execute(f"""
                SELECT
                    pricetable_dt,
                    store_no,
                    product_code,
                    skt_common_mnp,
                    skt_select_mnp,
                    kt_common_mnp,
                    kt_select_mnp,
                    lg_common_mnp,
                    lg_select_mnp
                FROM tb_pricetable_store_phone_col
                WHERE product_group_code = '{product_code}'
                LIMIT 3
            """)

            print(f"\n\n시세표 데이터 (product_group_code: {product_code}):")
            rows = cursor.fetchall()
            if rows:
                for idx, row in enumerate(rows, 1):
                    print(f"\n데이터 {idx}:")
                    for key, value in row.items():
                        if value is not None:
                            print(f"  {key}: {value:,}" if isinstance(value, int) else f"  {key}: {value}")
            else:
                print("  데이터 없음!")

                # product_code로도 확인
                print("\n\nproduct_code별로 확인:")
                cursor.execute("""
                    SELECT DISTINCT product_code, product_group_code
                    FROM tb_pricetable_store_phone_col
                    WHERE product_group_code LIKE '%17%'
                    LIMIT 10
                """)
                for row in cursor.fetchall():
                    print(f"  {row['product_group_code']} / {row['product_code']}")

finally:
    connection.close()
