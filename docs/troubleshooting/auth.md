# Auth에서 해결한 문제
## 중복 쿼리, N+1 문제
- POST /login - 로그인 시 select가 중복으로 나온다.  
  원인: 컨트롤러에서 검증하고 조회해서 생긴 문제

      AuthController
      if (memberService.findMemberByLoginId(loginRequest.getLoginId()) == null || loginMember == null) {
          throw new IllegalArgumentException("로그인에 실패했습니다.");
      }

  해결: MemberService, AuthService에서 검증하고 컨트롤러에서 조회만 하기

      MemberService
      return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));
      }

      AuthService
      if (!member.getPassword().equals(password)) {
          throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다.");
      }

  추가 문제: 로그인에서는 보안을 위해 아이디가 틀렸는지 비밀번호가 틀렸는지 구분을 하지 않는 데, MemberController에서는 아이디 틀린 상황만 있다.  
  해결: MemberService에 로그인 용도의 조회 메서드 추가

      public Member findMemberForLogin(String loginId) {
          return memberRepository.findByLoginId(loginId)
                  .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));
      }

  GET /web/login에도 똑같이 적용

## 로직
- 토큰 생성을 서비스에서 진행

      public String login(String loginId, String password) {
          Member loginMember = memberService.findMemberForLogin(loginId);
          if (!loginMember.getPassword().equals(password)) {
              throw new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다.");
          }

          return jwtProvider.createToken(loginMember);
      }

- JwtProvider의 토큰 검증 및 추출을 분리

      public boolean validateToken(String token) {
          try {
              parseToken(token);
              return true;
          } catch (JwtException e) {
              return false;
          }
      }

      public Claims extractClaims(String token) {
          return parseToken(token).getPayload();
      }

      private Jws<Claims> parseToken(String token) {
          return Jwts.parser()
                  .verifyWith(secretKey)
                  .build()
                  .parseSignedClaims(token);
      }