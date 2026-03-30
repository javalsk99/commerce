# 배포 중 발생한 문제
## CI/CD 에서 발생한 문제
- Docker build & push  
  Error: Process completed with exit code 1.

  시도한 방법: 도커 이미지에 버전 추가 - 메시지가 변하지 않았다.  
  해결: 환경 변수에 사용하던 Docker Access Token을 ReadOnly에서 Read, Write로 변경했다.


- Revoke GitHub IP from Security Group  
  Error: aws: [ERROR]: An error occurred (NoRegion): You must specify a region. You can also configure your region by running "aws configure".  
  Error: Process completed with exit code 253.

  해결: region을 추가했다.


- 비정상적인 접근 발견  
  2026-03-30T07:03:31.840Z  INFO 1 --- [commerce] [nio-8080-exec-6] l.c.interceptor.LoginCheckInterceptor    : 인증 체크  인터셉터 실행 [GET]/yii/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php
  2026-03-30T07:03:32.929Z  INFO 1 --- [commerce] [nio-8080-exec-8] l.c.interceptor.LoginCheckInterceptor    : 인증 체크  인터셉터 실행 [GET]/zend/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php
  2026-03-30T07:03:34.111Z  INFO 1 --- [commerce] [nio-8080-exec-1] l.c.interceptor.LoginCheckInterceptor    : 인증 체크  인터셉터 실행 [GET]/ws/ec/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php

  해결: 인터셉터가 잘 막아줬지만 관리자 아이디를 열어둘 계획이므로 관리자도 컨트롤러 이외의 요청을 막는다.

      if (!(handler instanceof HandlerMethod)) {
          if (requestURI.equals("/index.html")) {
              return true;
          }

          if (requestURI.startsWith("/payments/")) {
              String pathVariable = requestURI.substring("/payments/".length());

              if (NanoIdProvider.validateNanoId(pathVariable)) {
                  return true;
              }
          }

          log.warn("미등록 경로 접근 차단: [{}] {}", method, requestURI);
          return false;
      }
