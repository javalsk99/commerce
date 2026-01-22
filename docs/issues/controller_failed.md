# 컨트롤러에서 발생한 예외와 문제
- getSingleResult();를 사용해서 발생하는 NoResultException 예외

  em.createQuery().getResultStream().findFirst().orElse(null);로 해결

  https://stackoverflow.com/questions/2002993/jpa-getsingleresult-or-null

- org.springframework.http.converter.HttpMessageConversionException: Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]

  엔티티로 반환해서 생긴 문제

  DTO 반환으로 변경 -> 현재 생긴 추가 문제 : CategoryForm에서 계층 구조를 나타내지 못함