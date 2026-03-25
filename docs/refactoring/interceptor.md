# Interceptor 리팩토링
## LoginCheckInterceptor
- 토큰이 없을 때, JwtException을 401 에러로 예외 처리했다.
- 본인이 아닐 때, NotResourceOwnerException으로 예외를 두고 403 에러로 예외 처리했다.
- 관리자가 아닐 때, NotAdminException으로 예외를 두고 403 에러로 예외 처리했다.


- Member의 URI에 @PathVariable이 있는 경우 토큰의 loginId와 일치하지 않는 경우 NotResourceOwnerException 예외를 설정했다.