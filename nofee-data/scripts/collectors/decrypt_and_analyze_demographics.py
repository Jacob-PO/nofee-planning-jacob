#!/usr/bin/env python3
"""
노피 고객 인구통계 데이터 수집 스크립트 (생년월일 복호화 포함)
생년월일과 성별 데이터 분석
수집일: 2025-11-19
"""

import pymysql
import json
import os
from datetime import datetime, date
from pathlib import Path
from collections import defaultdict
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import binascii
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

# AES 복호화 키
SECRET_KEY = os.getenv('AES_SECRET_KEY')

def decrypt_aes(encrypted_hex_data):
    """AES 복호화 (HEX 중첩 방식)"""
    try:
        if not encrypted_hex_data:
            return None

        # HEX 문자열을 bytes로 변환
        if isinstance(encrypted_hex_data, bytes):
            encrypted_hex_data = encrypted_hex_data.decode('utf-8')

        # 첫 번째 HEX 디코딩 (저장된 HEX 문자열)
        encrypted_bytes = binascii.unhexlify(encrypted_hex_data)

        # HEX 문자열을 다시 디코딩
        hex_str = encrypted_bytes.decode('utf-8')
        actual_encrypted = binascii.unhexlify(hex_str)

        # AES 키 생성 (16바이트)
        key = SECRET_KEY.encode('utf-8')[:16].ljust(16, b'\0')

        # ECB 모드 복호화
        cipher = AES.new(key, AES.MODE_ECB)
        decrypted_padded = cipher.decrypt(actual_encrypted)

        # PKCS7 패딩 제거
        decrypted = unpad(decrypted_padded, AES.block_size)

        return decrypted.decode('utf-8')
    except Exception as e:
        # 조용히 실패
        return None

def calculate_age(birthday_str):
    """생년월일로부터 나이 계산 (YYYYMMDD 형식)"""
    if not birthday_str or len(birthday_str) != 8:
        return None

    try:
        birth_date = datetime.strptime(birthday_str, '%Y%m%d')
        today = datetime.now()
        age = today.year - birth_date.year - ((today.month, today.day) < (birth_date.month, birth_date.day))
        return age
    except:
        return None

def get_age_group(age):
    """나이를 연령대로 변환"""
    if age is None:
        return 'Unknown'
    elif age < 20:
        return '10대'
    elif age < 30:
        return '20대'
    elif age < 40:
        return '30대'
    elif age < 50:
        return '40대'
    elif age < 60:
        return '50대'
    else:
        return '60대 이상'

def collect_user_demographics():
    """고객 인구통계 데이터 수집 및 분석"""
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor(pymysql.cursors.DictCursor) as cursor:
            data = {
                'metadata': {
                    'collected_at': datetime.now().isoformat(),
                    'purpose': '고객 인구통계 분석',
                    'version': '2.0',
                    'note': '생년월일 복호화 포함'
                }
            }

            print("=" * 60)
            print("👥 노피 고객 인구통계 데이터 수집 (복호화 포함)")
            print("=" * 60)

            # 1. 전체 사용자 데이터 조회
            print("\n📊 1. 사용자 데이터 수집 중...")
            cursor.execute("""
                SELECT
                    user_no,
                    HEX(birthday) as birthday_hex,
                    gender_code,
                    created_at
                FROM tb_user
                WHERE deleted_yn = 'N'
            """)
            users = cursor.fetchall()
            total_users = len(users)
            print(f"   ✓ 총 사용자: {total_users:,}명")

            # 2. 생년월일 복호화 및 연령대 분석
            print("\n🎂 2. 생년월일 복호화 및 연령대 분석 중...")
            age_distribution = defaultdict(int)
            age_list = []
            birthday_valid_count = 0
            decrypt_fail_count = 0

            for user in users:
                birthday_hex = user.get('birthday_hex')
                if birthday_hex:
                    # 복호화
                    birthday_str = decrypt_aes(birthday_hex)
                    if birthday_str:
                        age = calculate_age(birthday_str)
                        if age and 0 < age < 120:  # 유효한 나이 범위
                            birthday_valid_count += 1
                            age_list.append(age)
                            age_group = get_age_group(age)
                            age_distribution[age_group] += 1
                        else:
                            decrypt_fail_count += 1
                    else:
                        decrypt_fail_count += 1

            # 연령대별 정렬 (10대 -> 60대 이상)
            age_order = ['10대', '20대', '30대', '40대', '50대', '60대 이상', 'Unknown']
            sorted_age_distribution = {k: age_distribution.get(k, 0) for k in age_order if age_distribution.get(k, 0) > 0}

            data['age_analysis'] = {
                'total_users': total_users,
                'valid_birthday_count': birthday_valid_count,
                'decrypt_fail_count': decrypt_fail_count,
                'missing_birthday_count': total_users - birthday_valid_count - decrypt_fail_count,
                'data_completeness': round((birthday_valid_count / total_users * 100), 2) if total_users > 0 else 0,
                'age_distribution': sorted_age_distribution,
                'age_distribution_percentage': {
                    k: round((v / birthday_valid_count * 100), 2) if birthday_valid_count > 0 else 0
                    for k, v in sorted_age_distribution.items()
                },
                'statistics': {}
            }

            if age_list:
                data['age_analysis']['statistics'] = {
                    'average_age': round(sum(age_list) / len(age_list), 1),
                    'min_age': min(age_list),
                    'max_age': max(age_list),
                    'median_age': sorted(age_list)[len(age_list) // 2]
                }
                print(f"   ✓ 평균 연령: {data['age_analysis']['statistics']['average_age']}세")
                print(f"   ✓ 생년월일 데이터 완성도: {data['age_analysis']['data_completeness']}%")
                print(f"   ✓ 복호화 실패: {decrypt_fail_count}건")

            # 연령대별 분포 출력
            print("\n   📊 연령대별 분포:")
            for age_group in age_order:
                if age_group in sorted_age_distribution and sorted_age_distribution[age_group] > 0:
                    count = sorted_age_distribution[age_group]
                    percentage = data['age_analysis']['age_distribution_percentage'][age_group]
                    print(f"      • {age_group}: {count:,}명 ({percentage}%)")

            # 3. 성별 분석
            print("\n⚥ 3. 성별 분석 중...")
            gender_distribution = defaultdict(int)
            gender_valid_count = 0

            # 성별 코드 매핑
            gender_mapping = {
                '0104001': '남성',
                '0104002': '여성',
                'Unknown': '미입력'
            }

            for user in users:
                gender_code = user.get('gender_code')
                if gender_code:
                    gender_valid_count += 1
                    gender_label = gender_mapping.get(gender_code, '미입력')
                    gender_distribution[gender_label] += 1
                else:
                    gender_distribution['미입력'] += 1

            data['gender_analysis'] = {
                'total_users': total_users,
                'valid_gender_count': gender_valid_count,
                'missing_gender_count': total_users - gender_valid_count,
                'data_completeness': round((gender_valid_count / total_users * 100), 2) if total_users > 0 else 0,
                'gender_distribution': dict(gender_distribution),
                'gender_distribution_percentage': {},
                'gender_code_mapping': gender_mapping
            }

            # 퍼센티지 계산
            for gender_label, count in gender_distribution.items():
                percentage = round((count / total_users * 100), 2) if total_users > 0 else 0
                data['gender_analysis']['gender_distribution_percentage'][gender_label] = percentage

            print(f"   ✓ 성별 데이터 완성도: {data['gender_analysis']['data_completeness']}%")
            print("\n   📊 성별 분포:")
            for gender_label, count in data['gender_analysis']['gender_distribution'].items():
                percentage = data['gender_analysis']['gender_distribution_percentage'][gender_label]
                print(f"      • {gender_label}: {count:,}명 ({percentage}%)")

            # 4. 연령대별 성별 교차 분석
            print("\n🔍 4. 연령대×성별 교차 분석 중...")
            cross_analysis = defaultdict(lambda: defaultdict(int))

            for user in users:
                birthday_hex = user.get('birthday_hex')
                gender_code = user.get('gender_code')

                # 나이 계산
                age = None
                if birthday_hex:
                    birthday_str = decrypt_aes(birthday_hex)
                    if birthday_str:
                        age = calculate_age(birthday_str)

                age_group = get_age_group(age)
                gender_label = gender_mapping.get(gender_code, '미입력') if gender_code else '미입력'

                cross_analysis[age_group][gender_label] += 1

            data['cross_analysis'] = {
                age_group: dict(genders) for age_group, genders in cross_analysis.items()
            }

            print("   ✓ 교차 분석 완료")
            for age_group in age_order:
                if age_group in cross_analysis:
                    total_in_group = sum(cross_analysis[age_group].values())
                    if total_in_group > 0:
                        print(f"\n   [{age_group}] (총 {total_in_group:,}명)")
                        for gender in ['남성', '여성', '미입력']:
                            count = cross_analysis[age_group].get(gender, 0)
                            if count > 0:
                                pct = round(count / total_in_group * 100, 1)
                                print(f"      • {gender}: {count:,}명 ({pct}%)")

            # 5. 핵심 인사이트
            print("\n💡 5. 핵심 인사이트 생성 중...")

            # 주요 타겟층 (가장 많은 연령대)
            main_age_group = max(sorted_age_distribution.items(), key=lambda x: x[1])[0] if sorted_age_distribution else 'Unknown'

            # 주요 성별
            main_gender = max(
                [(k, v) for k, v in gender_distribution.items() if k != '미입력'],
                key=lambda x: x[1]
            )[0] if any(k != '미입력' for k in gender_distribution.keys()) else '미입력'

            data['insights'] = {
                'primary_target_age_group': main_age_group,
                'primary_target_gender': main_gender,
                'data_quality': {
                    'birthday_completeness': data['age_analysis']['data_completeness'],
                    'gender_completeness': data['gender_analysis']['data_completeness']
                },
                'key_findings': []
            }

            # 인사이트 생성
            if data['age_analysis']['statistics']:
                avg_age = data['age_analysis']['statistics']['average_age']
                data['insights']['key_findings'].append(
                    f"평균 연령 {avg_age}세로 {main_age_group} 중심의 서비스"
                )

            if main_gender != '미입력':
                gender_pct = data['gender_analysis']['gender_distribution_percentage'][main_gender]
                data['insights']['key_findings'].append(
                    f"{main_gender} 사용자 비중 {gender_pct}%"
                )

            # 주요 타겟층 조합
            if main_age_group != 'Unknown' and main_gender != '미입력':
                data['insights']['key_findings'].append(
                    f"핵심 타겟: {main_age_group} {main_gender}"
                )

            print("   ✓ 인사이트 생성 완료")
            for finding in data['insights']['key_findings']:
                print(f"      • {finding}")

            return data

    finally:
        connection.close()

def save_data(data, output_dir):
    """데이터를 JSON 파일로 저장"""
    # latest 버전
    latest_path = Path(output_dir) / "user_demographics_latest.json"

    # 타임스탬프 버전
    timestamp_path = Path(output_dir) / f"user_demographics_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    # JSON 직렬화를 위해 datetime 객체 변환
    def json_serial(obj):
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        raise TypeError(f"Type {type(obj)} not serializable")

    # latest 버전 저장
    with open(latest_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2, default=json_serial)

    # 타임스탬프 버전 저장
    with open(timestamp_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2, default=json_serial)

    print(f"\n💾 데이터 저장 완료:")
    print(f"   • Latest: {latest_path}")
    print(f"   • Archive: {timestamp_path}")

    return latest_path

def main():
    """메인 실행 함수"""
    try:
        # 데이터 수집
        data = collect_user_demographics()

        # 데이터 저장
        script_dir = Path(__file__).parent.parent
        data_dir = script_dir / 'data' / 'database'
        data_dir.mkdir(parents=True, exist_ok=True)

        output_path = save_data(data, data_dir)

        print("\n" + "=" * 60)
        print("✅ 고객 인구통계 데이터 수집 완료!")
        print("=" * 60)

        return output_path

    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
