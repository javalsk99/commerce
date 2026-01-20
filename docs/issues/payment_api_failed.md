# 결제 연동 중 발생한 예외
- java.lang.NumberFormatException

  String의 paymentId를 Long 타입인 Id 값으로 변환하다 생긴 문제

  임시 해결: String 타입의 paymentId를 추가했다.

- jakarta.persistence.NoResultException

  String 타입의 paymentId로 Payment를 조회할 때, getSingleResult();로 해서 생긴 문제

  임시 해결: getResultList().stream().findFirst();로 변경했다.