# 잘 작동했었는데 도커 컨테이너의 MySQL을 재시작 후, 환경 변수 적용 안 되는 문제 발생
org.hibernate.exception.GenericJDBCException: unable to obtain isolated JDBC connection [Access denied for user '${USER_NAME}'@'172.17.0.1' (using password: YES)] [n/a]

- 시도한 방법

  환경 변수 다시 세팅, IntelliJ 캐시 삭제, 데이터베이스 삭제 후 재생성

- 해결 방법

  Gradle에도 환경 변수 추가 (JUnit에만 있었음)