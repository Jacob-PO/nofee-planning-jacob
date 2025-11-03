#!/usr/bin/env python3
"""
노피 운영 DB 테이블 구조 분석 스크립트
"""

import json
import pymysql
from datetime import datetime
import os

# DB 설정 파일 로드
config_path = os.path.join(os.path.dirname(__file__), '../../../config/db_config.json')
with open(config_path, 'r') as f:
    config = json.load(f)['production']

def connect_db():
    """데이터베이스 연결"""
    return pymysql.connect(
        host=config['host'],
        port=config['port'],
        user=config['user'],
        password=config['password'],
        database=config['database'],
        charset=config['charset'],
        cursorclass=pymysql.cursors.DictCursor
    )

def get_all_tables(cursor):
    """모든 테이블 목록 조회"""
    cursor.execute("SHOW TABLES")
    tables = [list(row.values())[0] for row in cursor.fetchall()]
    return tables

def get_table_structure(cursor, table_name):
    """테이블 구조 상세 조회"""
    # 컬럼 정보
    cursor.execute(f"DESCRIBE {table_name}")
    columns = cursor.fetchall()

    # 테이블 코멘트
    cursor.execute(f"""
        SELECT TABLE_COMMENT
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = '{config['database']}'
        AND TABLE_NAME = '{table_name}'
    """)
    table_comment = cursor.fetchone()['TABLE_COMMENT']

    # 인덱스 정보
    cursor.execute(f"SHOW INDEX FROM {table_name}")
    indexes = cursor.fetchall()

    # 행 개수
    cursor.execute(f"SELECT COUNT(*) as cnt FROM {table_name}")
    row_count = cursor.fetchone()['cnt']

    return {
        'table_name': table_name,
        'comment': table_comment,
        'row_count': row_count,
        'columns': columns,
        'indexes': indexes
    }

def analyze_database():
    """전체 데이터베이스 분석"""
    print("=" * 80)
    print("노피 운영 DB 테이블 구조 분석 시작")
    print("=" * 80)
    print()

    connection = connect_db()

    try:
        with connection.cursor() as cursor:
            # 모든 테이블 조회
            tables = get_all_tables(cursor)
            print(f"총 테이블 개수: {len(tables)}")
            print()

            all_structures = {}

            for i, table in enumerate(tables, 1):
                print(f"[{i}/{len(tables)}] 분석 중: {table}")
                structure = get_table_structure(cursor, table)
                all_structures[table] = structure

            # 결과를 JSON 파일로 저장
            output_file = os.path.join(os.path.dirname(__file__), 'db_structure_analysis.json')
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(all_structures, f, ensure_ascii=False, indent=2, default=str)

            print()
            print(f"✓ 분석 완료: {output_file}")

            # 마크다운 리포트 생성
            generate_markdown_report(all_structures)

    finally:
        connection.close()

def generate_markdown_report(structures):
    """마크다운 형식의 분석 리포트 생성"""
    output_file = os.path.join(os.path.dirname(__file__), 'db_structure_report.md')

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("# 노피 운영 DB 테이블 구조 분석 리포트\n\n")
        f.write(f"**분석 일시**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"**데이터베이스**: {config['database']}\n")
        f.write(f"**총 테이블 수**: {len(structures)}\n\n")

        f.write("---\n\n")

        # 목차
        f.write("## 목차\n\n")
        for table_name in sorted(structures.keys()):
            structure = structures[table_name]
            comment = structure['comment'] if structure['comment'] else '설명 없음'
            f.write(f"- [{table_name}](#{table_name.lower()}) - {comment} ({structure['row_count']:,}건)\n")

        f.write("\n---\n\n")

        # 각 테이블 상세 정보
        for table_name in sorted(structures.keys()):
            structure = structures[table_name]

            f.write(f"## {table_name}\n\n")

            if structure['comment']:
                f.write(f"**설명**: {structure['comment']}\n\n")

            f.write(f"**데이터 건수**: {structure['row_count']:,}건\n\n")

            # 컬럼 정보
            f.write("### 컬럼 구조\n\n")
            f.write("| 컬럼명 | 타입 | NULL | 키 | 기본값 | Extra |\n")
            f.write("|--------|------|------|-----|--------|-------|\n")

            for col in structure['columns']:
                null_yn = 'YES' if col['Null'] == 'YES' else 'NO'
                default = col['Default'] if col['Default'] else '-'
                extra = col['Extra'] if col['Extra'] else '-'
                f.write(f"| {col['Field']} | {col['Type']} | {null_yn} | {col['Key']} | {default} | {extra} |\n")

            # 인덱스 정보
            if structure['indexes']:
                f.write("\n### 인덱스\n\n")
                unique_indexes = {}
                for idx in structure['indexes']:
                    key_name = idx['Key_name']
                    if key_name not in unique_indexes:
                        unique_indexes[key_name] = {
                            'unique': idx['Non_unique'] == 0,
                            'columns': []
                        }
                    unique_indexes[key_name]['columns'].append(idx['Column_name'])

                for key_name, info in unique_indexes.items():
                    unique_text = 'UNIQUE' if info['unique'] else 'INDEX'
                    columns_text = ', '.join(info['columns'])
                    f.write(f"- **{key_name}** ({unique_text}): {columns_text}\n")

            f.write("\n---\n\n")

    print(f"✓ 리포트 생성: {output_file}")

if __name__ == "__main__":
    analyze_database()
