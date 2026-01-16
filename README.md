# 소개
로그인한 회원이 상품을 주문하고, 재고가 차감되고, 결제가 처리되는 커머스 백엔드 프로젝트

# 기술 스택
- Java, Spring Boot, JPA, MySQL
- EC2, Nginx, RDS, Docker, GitHub Actions

# 시스템 아키텍쳐
![architecture](docs/design/images/시스템_아키텍처.PNG)
본 프로젝트는 GitHub Actions 기반 CI/CD 파이프라인과 Docker를 사용하여 EC2에 배포되고,
Nginx를 리버스 프록시로 사용하는 API 서버 구조로 설계되었습니다.

- [도메인 설계](docs/design/domain.md)
- [ERD 설계](docs/design/erd.md)
- [시스템 아키텍처](docs/design/architecture.md)
- [API 명세](docs/design/api.md)


- 설계 한줄평

  API 명세 설계 과정에서 범위 설정에 어려움을 겪었지만, UI 구현을 제외하고 API 구현 서버를 목표로 방향을 정해 설계 범위를 정리하였습니다.

## 문제 해결 기록
개발 과정에서 발생한 문제와 해결 과정을 정리했습니다.

## 목록
- [환경 변수 문제](docs/issues/environment_variable.md)

### 참고
- 프로젝트 기획 단계에서 주제 선정과 준비 체크리스트 작성, 정리 내용의 수정 및 다듬기에 AI 도구를 참고하였습니다.
- 프로젝트 설계 단계에서 설계 내용의 검증 및 수정, 다듬기에 AI 도구를 참고하였습니다.