# 🌸 LovelyDream Backend

AI 기반 데이트 코스 추천 플랫폼 **러블리 드림**의 백엔드 레포지토리입니다.  
사용자 인증, 추천 엔진 연동, Redis 기반 인증 로직, 결제 및 구독 기능 등을 제공합니다.

---

## 📌 백엔드 기술 스택
일부 기술은 추후 적용 예정입니다. 
- Kotlin
- Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA
- MySQL
- Redis
- OpenAI GPT API (LLM 또는 RAG 및 추천 알고리즘 연동 예정)

---

## 📌 인프라 기술 스택
추후 배포 시 사용 예정인 기술 스택입니다.
- AWS Route53, S3, CloudFront, ELB, EC2, RDS
- Git Actions CI/CD

---

## 📦 주요 기능 개발 현황 (2025.08.06)

- [x] Swagger API 문서
- [x] 회원가입 (이메일 인증 / 약관동의)
- [ ] 프로필 설정
- [ ] 로그인 / JWT
- [ ] 추천 기능 (RAG / GPT 활용 예정)
- [ ] 프리미엄 구독 및 결제 기능
- [ ] AI 추천 코스 기반 캘린더 연동

---

## 🧪 테스트

- Kotlin + MockK 기반 유닛 테스트
- 테스트 클래스는 JUnit5 환경에서 실행
- 주요 테스트: 회원가입, 인증 코드 TTL, 예외 처리 등

---

## 👤 개발자

| 이름 | 역할 |
|------|------|
| 김재섭 ([KoreaNirsa](https://github.com/Nirsa-Dev)) | 백엔드 기획/설계/개발, 서버 구축 및 CI/CD |

📧 관심 있는 분은 islandtim@naver.com 으로 연락 주세요!

---

## 📎 관련 링크
- 개발 블로그 정리글: https://nirsa.tistory.com/467
