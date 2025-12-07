# Nofee 프로젝트 롤백 가이드

> 최종 업데이트: 2025-12-06
> 작성 기준: 안정 버전 스냅샷

---

## 프로젝트 개요

| 저장소 | 설명 | GitHub URL |
|--------|------|------------|
| nofee-front | Next.js 프론트엔드 | https://github.com/nofee-workspace/nofee-front.git |
| nofee-springboot | Spring Boot 백엔드 | https://github.com/nofee-workspace/nofee-springboot.git |

---

## 환경 정보

| 환경 | 프론트엔드 URL | API URL |
|------|---------------|---------|
| Development | https://dev.nofee.team | https://dev-api.nofee.team |
| Production | https://nofee.team | https://api.nofee.team |

---

## 안정 버전 스냅샷 (2025-12-06)

### nofee-front
| 항목 | 값 |
|------|-----|
| 브랜치 | `main` (안정) / `sunho.kim` (작업용) |
| 커밋 해시 (Full) | `c56bfa8a82b28714b37d32beba5c7ca6d19664d5` |
| 커밋 해시 (Short) | `c56bfa8` |
| 커밋 메시지 | fix: StoreItem 인터페이스에 pricetableNote 추가 및 PriceTableListSection 스타일 수정 |
| 커밋 일시 | 2025-11-26 10:42:07 |
| 작성자 | ahbin_cho |

**최근 main 브랜치 커밋 히스토리:**
```
c56bfa8 fix: StoreItem 인터페이스에 pricetableNote 추가 및 PriceTableListSection 스타일 수정
d7c809d fix: setProducts 호출에 setTimeout 추가 및 스타일 수정
c24cd27 fix: stepCode 조건에 '0201004' 추가
b97712a console 삭제
e77c4a2 fix: ratePlanCode를 ratePlanGroupNo로 변경 및 코드 정리
```

### nofee-springboot
| 항목 | 값 |
|------|-----|
| 브랜치 | `main` (안정) / `sunho.kim` (작업용) |
| 커밋 해시 (Full) | `607a5b2662251437b2a7cdb3e9f62679a6fdbc76` |
| 커밋 해시 (Short) | `607a5b2` |
| 커밋 메시지 | [AGENCY] 시세표 드래그 앤 드롭 정렬 기능 추가 및 UI 개선 |
| 커밋 일시 | 2025-12-06 12:23:38 |
| 작성자 | hobin.song |

**최근 main 브랜치 커밋 히스토리:**
```
607a5b2 [AGENCY] 시세표 드래그 앤 드롭 정렬 기능 추가 및 UI 개선
20420df 수정
9edc1f5 수정
61b40cf [API] 시세표 노출 조건 개선 및 pricetableNote 응답 추가
8bffcf4 [Agency] 시세표 삭제 확인 팝업 디자인 개선
```

---

## 롤백 방법

### 방법 1: GitHub Actions로 안정 버전 재배포 (권장)

1. GitHub 저장소 → **Actions** 탭
2. **"CD - Deploy Nofee"** 워크플로우 선택
3. **"Run workflow"** 클릭
4. 입력값:
   - branch: `main`
   - environment: `development` 또는 `production`

### 방법 2: 터미널에서 gh CLI 사용

```bash
# nofee-front를 main 브랜치로 dev 환경에 배포
gh workflow run cd.yml -R nofee-workspace/nofee-front -f branch=main -f environment=development

# nofee-front를 main 브랜치로 production 환경에 배포
gh workflow run cd.yml -R nofee-workspace/nofee-front -f branch=main -f environment=production
```

### 방법 3: 특정 커밋으로 롤백

```bash
# nofee-front 롤백
cd nofee-front
git checkout main
git reset --hard c56bfa8a82b28714b37d32beba5c7ca6d19664d5
git push -f origin main

# nofee-springboot 롤백
cd nofee-springboot
git checkout main
git reset --hard 607a5b2662251437b2a7cdb3e9f62679a6fdbc76
git push -f origin main
```

> ⚠️ **주의**: `git push -f`는 강제 푸시입니다. 다른 팀원의 작업이 덮어씌워질 수 있으니 신중하게 사용하세요.

### 방법 4: git revert로 안전하게 되돌리기

```bash
# 특정 커밋만 되돌리기 (새 커밋 생성)
git revert <문제가_된_커밋_해시>
git push origin main
```

---

## 배포 환경 조합

| 브랜치 | 환경 | 용도 |
|--------|------|------|
| `sunho.kim` | development | 개발/테스트 |
| `main` | development | dev 환경 롤백 |
| `main` | production | 실서버 배포 |

---

## 긴급 연락처

문제 발생 시 담당자에게 연락하세요.

---

## 체크리스트

배포 전 확인사항:
- [ ] 로컬에서 `npm run build:development` 성공 확인
- [ ] 변경사항 커밋 및 푸시 완료
- [ ] 올바른 브랜치와 환경 선택 확인

롤백 시 확인사항:
- [ ] 롤백할 커밋 해시 확인
- [ ] 다른 팀원에게 롤백 사실 공유
- [ ] 롤백 후 서비스 정상 동작 확인
