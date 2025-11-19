#!/usr/bin/env python3
"""
카카오맵 크롤링 결과 CSV 파일 병합 스크립트
output 폴더에 있는 모든 kakao_phone_stores_*_multi.csv 파일을 하나로 병합합니다.
"""

import os
import glob
import pandas as pd
from datetime import datetime
import re


def parse_address(address):
    """
    주소에서 시/도, 시/군/구 정보 추출

    Args:
        address: 주소 문자열

    Returns:
        (시/도, 시/군/구) 튜플
    """
    if pd.isna(address):
        return None, None

    # 주소 파싱 패턴
    # 서울 강남구, 경기 수원시 팔달구, 강원특별자치도 강릉시 등
    parts = address.split()

    if len(parts) < 2:
        return None, None

    sido = parts[0]  # 시/도
    sigungu = parts[1]  # 시/군/구

    return sido, sigungu


def transform_to_standard_format(df):
    """
    현재 형식(매장명,주소,전화번호,카테고리,카카오맵링크)을
    표준 형식(지역명_매장명,매장명,지역명,시,군구,전화번호,주소,링크)으로 변환

    Args:
        df: 원본 데이터프레임

    Returns:
        변환된 데이터프레임
    """
    # 주소에서 시/도, 시/군/구 파싱
    df[['시', '군구']] = df['주소'].apply(lambda x: pd.Series(parse_address(x)))

    # 지역명 = 시 + 군구
    df['지역명'] = df['시'].fillna('') + ' ' + df['군구'].fillna('')
    df['지역명'] = df['지역명'].str.strip()

    # 지역명_매장명
    df['지역명_매장명'] = df['지역명'] + '_' + df['매장명']

    # 링크 컬럼명 변경
    df['링크'] = df['카카오맵링크']

    # 표준 형식 컬럼 순서로 재정렬
    standard_df = df[['지역명_매장명', '매장명', '지역명', '시', '군구', '전화번호', '주소', '링크']]

    return standard_df


def merge_csv_files(output_dir='output', pattern='kakao_phone_stores_*_multi.csv'):
    """
    output 폴더의 모든 CSV 파일을 하나로 병합

    Args:
        output_dir: CSV 파일들이 있는 디렉토리
        pattern: 병합할 파일 패턴

    Returns:
        병합된 데이터프레임
    """
    # CSV 파일 경로 패턴
    csv_pattern = os.path.join(output_dir, pattern)
    csv_files = glob.glob(csv_pattern)

    if not csv_files:
        print(f"경고: {csv_pattern} 패턴에 맞는 파일을 찾을 수 없습니다.")
        return None

    print(f"총 {len(csv_files)}개의 CSV 파일을 발견했습니다.")

    # 모든 CSV 파일 읽어서 리스트에 저장
    dataframes = []

    for csv_file in csv_files:
        try:
            # CSV 파일 읽기 (BOM 처리 포함)
            df = pd.read_csv(csv_file, encoding='utf-8-sig')
            dataframes.append(df)
            print(f"✓ {os.path.basename(csv_file)}: {len(df)}개 행")
        except Exception as e:
            print(f"✗ {os.path.basename(csv_file)} 읽기 실패: {e}")

    if not dataframes:
        print("병합할 데이터가 없습니다.")
        return None

    # 모든 데이터프레임 병합
    merged_df = pd.concat(dataframes, ignore_index=True)
    print(f"\n병합 전 총 행 수: {len(merged_df)}")

    # 1단계: 모든 컬럼이 동일한 경우 중복 제거
    merged_df_deduplicated = merged_df.drop_duplicates()
    print(f"1단계 - 완전 중복 제거 후 행 수: {len(merged_df_deduplicated)}")
    print(f"  제거된 행 수: {len(merged_df) - len(merged_df_deduplicated)}")

    # 2단계: 전화번호 기준으로 중복 제거 (첫 번째 행만 유지)
    if '전화번호' in merged_df_deduplicated.columns:
        before_phone_dedup = len(merged_df_deduplicated)
        merged_df_deduplicated = merged_df_deduplicated.drop_duplicates(subset=['전화번호'], keep='first')
        print(f"2단계 - 전화번호 중복 제거 후 행 수: {len(merged_df_deduplicated)}")
        print(f"  제거된 행 수: {before_phone_dedup - len(merged_df_deduplicated)}")

    print(f"\n최종 행 수: {len(merged_df_deduplicated)}")
    print(f"총 제거된 행 수: {len(merged_df) - len(merged_df_deduplicated)}")

    # 3단계: 표준 형식으로 변환
    print("\n표준 형식으로 변환 중...")
    standard_df = transform_to_standard_format(merged_df_deduplicated)

    return standard_df


def save_merged_csv(df, output_dir='output'):
    """
    병합된 데이터프레임을 CSV 파일로 저장

    Args:
        df: 병합된 데이터프레임
        output_dir: 저장할 디렉토리

    Returns:
        저장된 파일 경로
    """
    # 타임스탬프 생성
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    filename = f'kakao_phone_stores_merged_{timestamp}.csv'
    filepath = os.path.join(output_dir, filename)

    # CSV로 저장 (BOM 포함, UTF-8 인코딩)
    df.to_csv(filepath, index=False, encoding='utf-8-sig')

    print(f"\n병합된 파일이 저장되었습니다: {filepath}")
    print(f"총 {len(df)}개의 매장 정보가 포함되어 있습니다.")

    return filepath


def print_summary(df):
    """
    병합된 데이터 요약 정보 출력

    Args:
        df: 데이터프레임
    """
    print("\n=== 병합 결과 요약 ===")
    print(f"총 매장 수: {len(df)}")

    if '지역명' in df.columns:
        print(f"\n지역별 매장 수:")
        region_counts = df['지역명'].value_counts()
        for region, count in region_counts.head(10).items():
            print(f"  - {region}: {count}개")
        if len(region_counts) > 10:
            print(f"  ... 외 {len(region_counts) - 10}개 지역")

    if '시' in df.columns:
        print(f"\n총 시/도 수: {df['시'].nunique()}개")

    if '군구' in df.columns:
        print(f"총 군/구 수: {df['군구'].nunique()}개")

    print("\n컬럼 정보:")
    for col in df.columns:
        print(f"  - {col}")


def main():
    """메인 함수"""
    print("=" * 60)
    print("카카오맵 크롤링 결과 CSV 병합 스크립트")
    print("=" * 60)

    # 현재 스크립트 위치 기준으로 output 디렉토리 설정
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, 'output')

    # output 디렉토리 확인
    if not os.path.exists(output_dir):
        print(f"오류: {output_dir} 디렉토리를 찾을 수 없습니다.")
        return

    # CSV 파일 병합
    merged_df = merge_csv_files(output_dir)

    if merged_df is None or len(merged_df) == 0:
        print("병합할 데이터가 없습니다.")
        return

    # 결과 요약 출력
    print_summary(merged_df)

    # 병합된 파일 저장
    saved_filepath = save_merged_csv(merged_df, output_dir)

    print("\n✅ 병합 완료!")


if __name__ == '__main__':
    main()
