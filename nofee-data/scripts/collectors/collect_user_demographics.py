#!/usr/bin/env python3
"""
노피 고객 인구통계 데이터 수집 스크립트
생년월일과 성별 데이터 분석
수집일: 2025-11-19
"""

import pymysql
import json
from datetime import datetime, date
from pathlib import Path
from collections import defaultdict

# DB 설정
DB_CONFIG = {
    'host': '43.203.125.223',
    'port': 3306,
    'user': 'nofee',
    'password': 'HBDyNLZBXZ41TkeZ',
    'database': 'db_nofee',
    'charset': 'utf8mb4'
}

def calculate_age(birthday):
    """생년월일로부터 나이 계산"""
    if not birthday:
        return None

    try:
        # birthday가 문자열인 경우
        if isinstance(birthday, str):
            birth_date = datetime.strptime(birthday, '%Y%m%d')
        # birthday가 bytes인 경우 (blob)
        elif isinstance(birthday, bytes):
            birth_date = datetime.strptime(birthday.decode('utf-8'), '%Y%m%d')
        # birthday가 datetime인 경우
        elif isinstance(birthday, (datetime, date)):
            birth_date = birthday
        else:
            return None

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
                    'version': '1.0'
                }
            }

            print("=" * 60)
            print("👥 노피 고객 인구통계 데이터 수집")
            print("=" * 60)

            # 1. 전체 사용자 데이터 조회
            print("\n📊 1. 사용자 데이터 수집 중...")
            cursor.execute("""
                SELECT
                    user_no,
                    birthday,
                    gender_code,
                    created_at
                FROM tb_user
                WHERE deleted_yn = 'N'
            """)
            users = cursor.fetchall()
            total_users = len(users)
            print(f"   ✓ 총 사용자: {total_users:,}명")

            # 2. 생년월일 및 연령대 분석
            print("\n🎂 2. 연령대 분석 중...")
            age_distribution = defaultdict(int)
            age_list = []
            birthday_valid_count = 0

            for user in users:
                birthday = user.get('birthday')
                if birthday:
                    age = calculate_age(birthday)
                    if age:
                        birthday_valid_count += 1
                        age_list.append(age)
                        age_group = get_age_group(age)
                        age_distribution[age_group] += 1

            # 연령대별 정렬 (10대 -> 60대 이상)
            age_order = ['10대', '20대', '30대', '40대', '50대', '60대 이상', 'Unknown']
            sorted_age_distribution = {k: age_distribution.get(k, 0) for k in age_order}

            data['age_analysis'] = {
                'total_users': total_users,
                'valid_birthday_count': birthday_valid_count,
                'missing_birthday_count': total_users - birthday_valid_count,
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

            for user in users:
                gender_code = user.get('gender_code')
                if gender_code:
                    gender_valid_count += 1
                    gender_distribution[gender_code] += 1
                else:
                    gender_distribution['Unknown'] += 1

            # 성별 코드 매핑
            gender_mapping = {
                'M': '남성',
                'F': '여성',
                'Unknown': '미입력'
            }

            data['gender_analysis'] = {
                'total_users': total_users,
                'valid_gender_count': gender_valid_count,
                'missing_gender_count': total_users - gender_valid_count,
                'data_completeness': round((gender_valid_count / total_users * 100), 2) if total_users > 0 else 0,
                'gender_distribution': {
                    gender_mapping.get(k, k): v for k, v in gender_distribution.items()
                },
                'gender_distribution_percentage': {}
            }

            # 퍼센티지 계산
            for gender_code, count in gender_distribution.items():
                gender_label = gender_mapping.get(gender_code, gender_code)
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
                birthday = user.get('birthday')
                gender_code = user.get('gender_code')

                age = calculate_age(birthday) if birthday else None
                age_group = get_age_group(age)
                gender_label = gender_mapping.get(gender_code, '미입력') if gender_code else '미입력'

                cross_analysis[age_group][gender_label] += 1

            data['cross_analysis'] = {
                age_group: dict(genders) for age_group, genders in cross_analysis.items()
            }

            print("   ✓ 교차 분석 완료")
            for age_group in age_order:
                if age_group in cross_analysis:
                    print(f"\n   [{age_group}]")
                    for gender in ['남성', '여성', '미입력']:
                        count = cross_analysis[age_group].get(gender, 0)
                        if count > 0:
                            print(f"      • {gender}: {count:,}명")

            # 5. 신청자 인구통계 (실제 서비스 이용자)
            print("\n📱 5. 신청자 인구통계 분석 중...")

            # 견적 신청자
            cursor.execute("""
                SELECT DISTINCT u.user_no, u.birthday, u.gender_code
                FROM tb_user u
                INNER JOIN tb_apply_phone ap ON u.user_no = ap.user_no
                WHERE u.deleted_yn = 'N' AND ap.deleted_yn = 'N'
            """)
            quote_applicants = cursor.fetchall()

            # 캠페인 신청자
            cursor.execute("""
                SELECT DISTINCT u.user_no, u.birthday, u.gender_code
                FROM tb_user u
                INNER JOIN tb_apply_campaign_phone acp ON u.user_no = acp.user_no
                WHERE u.deleted_yn = 'N' AND acp.deleted_yn = 'N'
            """)
            campaign_applicants = cursor.fetchall()

            # 전체 신청자 (중복 제거)
            all_applicants_dict = {}
            for applicant in quote_applicants + campaign_applicants:
                all_applicants_dict[applicant['user_no']] = applicant

            all_applicants = list(all_applicants_dict.values())

            # 신청자 연령대 분석
            applicant_age_dist = defaultdict(int)
            for applicant in all_applicants:
                birthday = applicant.get('birthday')
                if birthday:
                    age = calculate_age(birthday)
                    if age:
                        age_group = get_age_group(age)
                        applicant_age_dist[age_group] += 1

            # 신청자 성별 분석
            applicant_gender_dist = defaultdict(int)
            for applicant in all_applicants:
                gender_code = applicant.get('gender_code')
                gender_label = gender_mapping.get(gender_code, '미입력') if gender_code else '미입력'
                applicant_gender_dist[gender_label] += 1

            total_applicants = len(all_applicants)
            data['applicant_demographics'] = {
                'total_applicants': total_applicants,
                'age_distribution': dict(applicant_age_dist),
                'gender_distribution': dict(applicant_gender_dist)
            }

            print(f"   ✓ 총 신청자: {total_applicants:,}명")
            print(f"   ✓ 전체 가입자 대비: {round(total_applicants/total_users*100, 2)}%")

            # 6. 핵심 인사이트
            print("\n💡 6. 핵심 인사이트 생성 중...")

            # 주요 타겟층 (가장 많은 연령대)
            main_age_group = max(age_distribution.items(), key=lambda x: x[1])[0] if age_distribution else 'Unknown'

            # 주요 성별
            main_gender = max(
                [(k, v) for k, v in gender_distribution.items() if k != 'Unknown'],
                key=lambda x: x[1]
            )[0] if any(k != 'Unknown' for k in gender_distribution.keys()) else 'Unknown'
            main_gender_label = gender_mapping.get(main_gender, main_gender)

            data['insights'] = {
                'primary_target_age_group': main_age_group,
                'primary_target_gender': main_gender_label,
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

            if main_gender_label != '미입력':
                gender_pct = data['gender_analysis']['gender_distribution_percentage'][main_gender_label]
                data['insights']['key_findings'].append(
                    f"{main_gender_label} 사용자 비중 {gender_pct}%"
                )

            print("   ✓ 인사이트 생성 완료")

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
