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

- MemberResponse  
  필드에 Grade를 빼고, 주소를 추가했다.  
  static 메서드는 관례에 맞춰 파라미터가 1개이므로 from으로 이름 변경


- MemberChangePassword
- MemberChangeAddress
- CategoryRequest


- CategoryResponse  
  정적 메서드에서 children을 생성하는 로직을 Stream으로 변경했다.


- CategoryDisconnectResponse
  정적 메서드에서 productResponseList를 생성하는 로직을 Stream으로 변경했다.


- ProductRequest