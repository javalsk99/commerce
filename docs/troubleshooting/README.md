# 문제 해결 과정
- [auth](auth.md)
- [member](member.md)
- [category](category.md)
- [product](product.md)
- [order](order.md)
- [payment](payment.md)
- [delivery](delivery.md)
- 테스트 코드 작성 중 검색 기능이 빠진 것을 발견  
  검색 기능에 QueryDSL을 사용하는 것을 배워서 com.querydsl:querydsl-jpa:5.0.0:jakarta 의존성 추가  
  [GHSA-6q3q-6v5j-h6vg](https://osv.dev/vulnerability/GHSA-6q3q-6v5j-h6vg) 8.2 Querydsl vulnerable to HQL injection trough orderBy 보안 취약성 메시지 발견  
  [maven central repository](https://central.sonatype.com/artifact/io.github.openfeign.querydsl/querydsl-jpa/7.1/overview)를 참고하여 io.github.openfeign.querydsl:querydsl-jpa:7.1 의존성 추가  
  기본적인 검색은 배웠던 내용에서 변한게 없으므로 초성으로 검색하는 기능을 추가했다.
- 통합 테스트가 실제 서버를 사용해서 오래걸린다.  
  가짜 객체를 사용해 단위 테스트로 테스트 시간을 단축시킨다.