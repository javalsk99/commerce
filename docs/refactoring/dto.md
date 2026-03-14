# DTO 리팩토링
JSON 데이터로 보내기 쉽고, 간결한 Record로 변경

- MemberLoginRequest
- MemberRequest
- MemberSearchCond
- MemberQueryDto  
  toBuilder()를 이용해 새로운 인스턴스를 만들면서 orderQueryDtoList 필드를 담는다.

      public MemberQueryDto findMember(String loginId) {
          return member.toBuilder()
                  .orderQueryDtoList(orderMap.get(member.loginId()))
                  .build();

      public List<MemberQueryDto> searchMembers(MemberSearchCond cond) {
          return members.stream()
                  .map(m -> m.toBuilder()
                          .orderQueryDtoList(orderMap.get(m.loginId()))
                          .build())
                  .collect(toList());
