# 토큰에서 발생한 예외
- io.jsonwebtoken.RequiredTypeException: Cannot convert existing claim value of type 'class java.lang.String' to desired type 'class lsk.commerce.domain.Grade'

  Enum 타입의 Grade를 String으로 받지 않아서 생긴 문제 .claim("grade", loginMember.getGrade())

  해결: .claim("grade", loginMember.getGrade().name())로 변경


- 인터셉터에서 비로그인으로도 접속 가능한 회원 가입 API가 안되는 문제

  /members로 시작해야 되는데 /member로 경로 오타

  해결: if (!(handler instanceof HandlerMethod)) 실제 있는 경로가 아니면 인터셉터에 적용되지 않게 변경