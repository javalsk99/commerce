# DTO 리팩토링
- JSON 데이터로 보내기 쉽고, 간결한 Record로 변경


- MemberLoginRequest
- MemberCreateRequest
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
- CategoryCreateRequest


- CategoryResponse  
  정적 메서드에서 children을 생성하는 로직을 Stream으로 변경했다.


- CategoryChangeParentRequest  
  컨트롤러에서 필드 주입을 DTO 주입으로 변경했다.


- CategoryDisconnectResponse  
  정적 메서드에서 productResponseList를 생성하는 로직을 Stream으로 변경했다.


- ProductCreateRequest  
  다른 dtype의 필드가 있는지 검증을 추가했다.


- ProductResponse  
  DTO를 QClass로 생성해 주는 @QueryProjection를 사용하기 위해 컴팩트 생성자로 변경했다.


- ProductSearchCond


- ProductUpdateRequest  
  price와 stockQuantity 둘 다 null인 상황만 아니면 돼서 @NotNull을 제거하고 @AssertTrue를 추가했다.


- ProductNameWithCategoryNameResponse, CategoryNameResponse  
  정적 메서드에서 categoryNameResponseList를 생성하는 로직을 Stream으로 변경했다.


- OrderCreateRequest  
  컨트롤러에서 필드 주입을 DTO 주입으로 변경했다.  
  memberLoginId를 제거했다.


- OrderQueryDto  
  전체 필드 중 orderProductQueryDtoList 필드만 빠진 생성자를 QueryDSL의 Q파일 생성자와, DTO 직접 조회의 member.loginId로 조회에서 사용했다.  
  전체 필드 중 loginId, orderProductQueryDtoList 필드가 빠진 생성자를 DTO 직접 조회의 orderNumber로 조회에서 사용했다.


- OrderProductQueryDto
- OrderSearchCond


- OrderChangeRequest  
  컨트롤러에서 필드 주입을 DTO 주입으로 변경했다.


- OrderResponse
- OrderProductDto


- OrderPaymentResponse  
  주문 정보를 응답하는 DTO이므로 OrderRequest에서 이름을 변경했다.  
  포트원 서버로 보내는 요청 데이터라고 생각해서 Request라는 이름을 사용했었고, 내 서버에서 포트원 서버로 보내는 응답 데이터이므로 Response로 이름을 변경했다.


- CompletePaymentRequest


- PaymentCompleteResponse  
  요청을 검증 후 결제 완료 응답하는 DTO이므로 PaymentRequest에서 이름을 변경했다.

