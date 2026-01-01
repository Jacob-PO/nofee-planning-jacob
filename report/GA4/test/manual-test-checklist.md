# FB Pixel 이벤트 수동 테스트 체크리스트

## 테스트 환경 설정

1. 개발 서버 실행: `npm run dev` (http://localhost:3001)
2. 브라우저 개발자 도구 열기 (F12)
3. Network 탭 → 필터에 `facebook.com/tr` 입력

---

## 테스트 시나리오

### 1. PageView - SPA 라우트 변경
- [ ] 홈 페이지 접속 (`/home-v2`)
- [ ] 비교 페이지로 이동 (`/compare`)
- [ ] Network 탭에서 `PageView` 이벤트 확인

**예상 결과:**
- 라우트 변경 시 `PageView` 이벤트 발생
- `page_path` 파라미터에 URL 경로 포함

---

### 2. Search - 필터 사용
- [ ] 홈 페이지에서 브랜드 필터 클릭
- [ ] "아이폰" 또는 "갤럭시" 선택
- [ ] Network 탭에서 `Search` 이벤트 확인

**예상 결과:**
- `Search` 이벤트 발생
- `search_string: "brand:아이폰"` 형태

---

### 3. Contact - 로그인 의도 (Welcome 페이지)
- [ ] Welcome 페이지 접속 (`/welcome`)
- [ ] "카카오로 3초 만에 시작하기" 버튼 클릭
- [ ] Network 탭에서 `Contact` 이벤트 확인 (리다이렉트 전)

**예상 결과:**
- `Contact` 이벤트 발생
- `content_name: "welcome"` 또는 상품명
- `source: "welcome"`

---

### 4. ViewContent - 딜 상세 조회
- [ ] 홈에서 딜 카드 클릭
- [ ] 딜 상세 페이지로 이동
- [ ] Network 탭에서 `ViewContent` 이벤트 확인

**예상 결과:**
- `ViewContent` 이벤트 발생
- `content_name`: 상품명
- `content_ids`: [상품코드]
- `content_category`: 통신사
- `value`: 월 납부금
- `currency`: "KRW"

---

### 5. InitiateCheckout - 견적 신청 클릭
- [ ] 딜 상세 페이지에서 "최저가 알림 채팅받기" 버튼 클릭
- [ ] Network 탭에서 `InitiateCheckout` 이벤트 확인

**예상 결과:**
- `InitiateCheckout` 이벤트 발생
- 동일 상품 파라미터 포함

---

### 6. Login (커스텀) - 로그인 완료
- [ ] 카카오 로그인 진행
- [ ] 로그인 완료 후 콜백 페이지
- [ ] Network 탭에서 `Login` 커스텀 이벤트 확인

**예상 결과:**
- `Login` 커스텀 이벤트 발생
- `method: "kakao"`
- `user_id`: 사용자 ID

---

### 7. CompleteRegistration - 회원가입 완료
- [ ] 신규 사용자로 카카오 로그인 (첫 가입)
- [ ] Network 탭에서 `CompleteRegistration` 이벤트 확인

**예상 결과:**
- `CompleteRegistration` 이벤트 발생
- `content_name: "signup"`
- `status: true`

---

### 8. Lead - 견적 완료
- [ ] 로그인 상태에서 견적 신청 완료
- [ ] Network 탭에서 `Lead` 이벤트 확인

**예상 결과:**
- `Lead` 이벤트 발생
- 상품 정보 파라미터 포함

---

## 콘솔 로그 확인

Console 탭에서 다음 로그 확인:
- `[FB Pixel]` - Pixel 이벤트 관련
- `[FB CAPI]` - Conversion API 관련

---

## 중복 이벤트 확인

동일 세션에서:
- [ ] 같은 딜 페이지 재방문 시 ViewContent 중복 발생하지 않음
- [ ] 같은 견적 신청 시 InitiateCheckout 중복 발생하지 않음
- [ ] 같은 회원가입 시 CompleteRegistration 중복 발생하지 않음

---

## 테스트 결과

| 이벤트 | 발생 여부 | 파라미터 정상 | 비고 |
|--------|----------|--------------|------|
| PageView | | | |
| Search | | | |
| Contact | | | |
| ViewContent | | | |
| InitiateCheckout | | | |
| Login | | | |
| CompleteRegistration | | | |
| Lead | | | |

테스트 일시: ____________________
테스트 담당: ____________________
