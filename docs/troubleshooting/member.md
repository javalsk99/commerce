# Member에서 해결한 문제
## 중복 쿼리, N+1 문제
- AuthController와 같이 컨트롤러에서 검증 제거
- GET /members N+1 문제가 발생했다.  
  원인: 주문을 하기 전, /members로 조회하면 Member에 Order가 추가로 조회되는 문제  
  시도한 방법: Fetch Join 사용 - Member와 Order가 한 번에 조회되지만, Order가 없는 경우 조회되지 않는 문제 발생

      return em.createQuery(
                      "select m from Member m " +
                            " join fetch m.orders o", Member.class)
              .getResultList();

  Fetch Join은 가져오는 대상이 null이면 조회가 안되는 문제가 있다. - left join fetch를 사용하면 가져오는 대상이 null이여도 조회가 된다.  
  추가 문제: 주문이 있으면 Member, Order, Payment, Delivery, OrderProduct, Product가 조회돼서 6번의 쿼리가 나온다.  
  해결: 인강에서 들었던 컬렉션 조회 최적화의 JPA에서 DTO 직접 조회 참고 - 위의 메서드들 조립해서 Member -> Order, Payment, Delivery -> OrderProduct, Product로 합쳐서 3번의 쿼리로 줄였다.

      protected List<MemberQueryDto> findMembers()
      protected static List<String> toMemberLoginIds(List<MemberQueryDto> result)
      protected List<OrderQueryDto> findOrdersByLoginIds(List<String> loginIds)
      protected static List<String> toOrderNumbers(List<OrderQueryDto> result)
      protected Map<String, List<OrderProductQueryDto>> findOrderProductMap(List<String> orderNumbers)

- GET /members/{memberLoginId} GET /members와 같은 문제가 발생했다.  
  해결: GET /members에서 아래의 두 메서드들만 바꿔서 해결

      protected Optional<MemberQueryDto> findMember(String loginId)
      protected List<OrderQueryDto> findOrdersByLoginId(String loginId)

- POST /members/{memberLoginId}/password, /members/{memberLoginId}/address에서 select가 중복돼서 나온다.  
  원인: member를 찾고, changePassword, changeAddress에서도 member를 찾아서 생긴 문제

      Member member = memberService.findMemberByLoginId(memberLoginId);
      memberService.changePassword(member.getLoginId(), form.getPassword());
      return memberService.getMemberDto(member);

  해결: changePassword, changeAddress에서 member를 받고 반환해서 select 중복 쿼리 제거

      Member member = memberService.changePassword(memberLoginId, form.getPassword());
      return memberService.getMemberDto(member);
