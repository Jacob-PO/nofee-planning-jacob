#!/usr/bin/env python3
"""
노피 DB 스키마 수집 스크립트
모든 테이블의 스키마 정보 수집
"""

import pymysql
import json
from datetime import datetime
from pathlib import Path

import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv(Path(__file__).parents[4] / '.env')

# DB 설정
DB_CONFIG = {
    'host': os.getenv('DB_HOST'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER'),
    'password': os.getenv('DB_PASSWORD'),
    'database': os.getenv('DB_NAME'),
    'charset': 'utf8mb4'
}

def collect_db_schema():
    """DB 스키마 수집"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            print("=" * 80)
            print("📊 노피 DB 스키마 수집")
            print("=" * 80)

            # 1. 모든 테이블 목록 조회
            cursor.execute("SHOW TABLES")
            tables = [list(row.values())[0] for row in cursor.fetchall()]

            print(f"\n총 테이블 수: {len(tables)}개")

            schema_data = {
                'metadata': {
                    'collected_at': datetime.now().isoformat(),
                    'database': 'db_nofee',
                    'total_tables': len(tables)
                },
                'tables': {}
            }

            # 2. 각 테이블의 스키마 수집
            for table in sorted(tables):
                print(f"\n📋 {table}")

                # 테이블 구조
                cursor.execute(f"DESCRIBE {table}")
                columns = list(cursor.fetchall())

                # 테이블 row 수
                cursor.execute(f"SELECT COUNT(*) as count FROM {table}")
                row_count = cursor.fetchone()['count']

                # 인덱스 정보
                cursor.execute(f"SHOW INDEX FROM {table}")
                indexes = list(cursor.fetchall())

                schema_data['tables'][table] = {
                    'columns': columns,
                    'row_count': row_count,
                    'indexes': indexes
                }

                print(f"   컬럼: {len(columns)}개, 데이터: {row_count:,}건")

            return schema_data

    finally:
        connection.close()

def save_schema(schema_data, output_dir):
    """스키마 데이터 저장"""
    output_path = Path(output_dir) / f"db_schema_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(schema_data, f, ensure_ascii=False, indent=2, default=str)

    print(f"\n💾 스키마 저장: {output_path}")
    return output_path

def main():
    """메인 실행"""
    try:
        # 스키마 수집
        schema_data = collect_db_schema()

        # 저장
        script_dir = Path(__file__).parent
        output_dir = script_dir.parent.parent / '1-raw-data' / 'database'
        output_dir.mkdir(parents=True, exist_ok=True)

        output_path = save_schema(schema_data, output_dir)

        print("\n" + "=" * 80)
        print("✅ DB 스키마 수집 완료!")
        print("=" * 80)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
