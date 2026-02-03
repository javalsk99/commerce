# 5일차 – 기술 스택 최종 결정 (클라우드 포함)
## 목표 : 왜 이 기술을 썼는지 설명 가능 상태
## 체크리스트
Java / Spring / JPA 버전 결정  
DB 종류 결정 (MySQL, PostgreSQL)  
AWS 서비스 선택 (EC2, RDS)  
Docker 사용 여부 결정  
Nginx 사용 여부 결정  
CI/CD 사용 여부 결정  
산출물 : 선택한 기술 / 선택하지 않은 기술 / 이유

## 체크리스트 정리
- Java / Spring / JPA 버전 결정  
  Java는 처음 배웠을 때부터 21버전으로 사용했고, 장기 지원 버전이므로 21버전을 사용한다.  
  Spring Boot는 현재 4.x 버전이 나왔지만, 3.x 버전이 Java 21버전에서 검증된 안정적인 버전이다.  
  본 프로젝트는 학습 및 포트폴리오 목적이므로, 안정성과 자료가 많은 3.x 버전을 사용한다.  
  JPA는 Spring Boot 3.x 버전과 호환되는 Hibernate6 최신 버전을 사용한다.


- DB 종류 결정  
  MySQL은 설정과 운영이 쉽고, 대중적이어서 자료가 많다.  
  PostgreSQL은 복잡한 쿼리, 데이터 유형, 확장성, 동시성 및 데이터 무결성에 강점이 있어서 대규모 프로젝트에 어울린다.  
  본 프로젝트는 대규모 프로젝트가 아니므로 MySQL을 사용한다.


- AWS 서비스 선택  
  EC2는 사용자가 가상 컴퓨터를 임대받아 사용하는 것이다.  
  RDS는 애플리케이션 내에서 관계형 데이터베이스의 설정, 운영, 스케일링을 단순하게 하는 웹 서비스이다.  
  EC2에 DB를 설치해서 사용해도 되지만, EC2와 RDS를 같이 사용하면 서버에 문제가 생길 때, 서버를 교체해서 서비스를 중단하지 않을 수 있다.  
  그리고 서버와 DB를 분리해서 서버를 확장하거나 줄이는 과정에서도 DB에 손상이 가지 않는다. 그래서 EC2와 RDS를 같이 사용한다.


- Docker 사용 여부 결정  
  Docker는 애플리케이션을 구축, 테스트 및 배포할 수 있는 소프트웨어 플랫폼이다.  
  Docker를 사용하면 코드를 더 빨리 전달하고, 애플리케이션 운영을 표준화하고, 코드를 원활하게 이동하고, 리소스 사용률을 높여 비용을 절감할 수 있다.  
  내 개발 환경을 그대로 구축해서 환경의 차이로 발생하는 문제를 줄일 수 있는 장점도 있어서 Docker를 사용한다.


- Nginx 사용 여부 결정  
  Nginx는 웹 서비스, 리버스 프록시, 캐싱, 로드 밸런싱, 미디어 스트리밍 등을 위한 오픈 소스 소프트웨어다.  
  Docker와 Spring Boot 조합에 Nginx가 많이 사용되고, 설정이 단순해서 Nginx를 사용한다.


- CI/CD 사용 여부 결정  
  CI는 지속적인 통합으로, 프로덕션 환경으로 배포할 수 있도록 준비하는 프로세스다.  
  CD는 지속적인 제공 및 배포로, 저장소로 전달된 프로덕션 서비스를 실제 사용자들에게 배포하는 프로세스다.  
  GitHub Actions는 GitHub에서 제공하는 CI/CD 툴로, 코드 변경 후 빌드하고 저장소에 전달 후 배포하는 과정을 자동화한다. 그래서 CI/CD로 GitHub Actions를 사용한다.


- 개인적인 생각  
  AWS, Docker, Nginx, CI/CD를 처음 써보지만 다 사용하는 이유는 실무에서 사용하는 기술 스택들을 직접 적용해 실무 경험을 미리 쌓는데 도움이 될 것 같아서 다 사용하는 것을 선택했다.  
  사용해보지 않은 기술들이라 찾아보고 개념을 이해하고, 이 기술들을 선택할 이유를 정리하는 데 어려움이 있었지만, 이렇게 왜 필요한지와 사용하는 이유를 정리해 두면, 사용할 때 어디서 쓰는지와 기술들을 이해하는 데 도움이 된다고 생각해서 정리했다. 

### 참고 자료
- [MySQL과 PostgreSQL의 차이](https://aws.amazon.com/ko/compare/the-difference-between-mysql-vs-postgresql)
- [EC2 / RDS란?](https://devjjo.tistory.com/31)
- [RDS, EC2를 연동하는 이유](https://velog.io/@softwarerbfl/AWS-EC2-RDS-%EC%97%B0%EA%B2%B0%ED%95%98%EA%B8%B0)
- [Docker란?](https://aws.amazon.com/ko/docker)
- [Nginx란?](https://nginxstore.com/blog/nginx/nginx-%EB%9E%80)
- [GitHub Actions에 대한 소개와 사용법](https://velog.io/@ggong/Github-Action%EC%97%90-%EB%8C%80%ED%95%9C-%EC%86%8C%EA%B0%9C%EC%99%80-%EC%82%AC%EC%9A%A9%EB%B2%95)