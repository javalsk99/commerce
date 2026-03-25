# Order 리팩토링
## OrderController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  파라미터로 받는 변수들을 DTO에 담아서 검증을 추가했다.


- changeOrder  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.  
  Delivery와 Payment만 가져오는 Fetch Join에 OrderProducts도 Fetch Join으로 추가했다.


- cancelOrder  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


- create, changeOrder, delete, cancelOrder  
  @Login으로 loginId을 쿠키에서 꺼내서 OrderService로 보낸다.


## OrderService
- order, changeOrder  
  파라미터를 DTO로 변경했다.  
  중복 로직을 정적 메서드로 추출하고 반환 타입을 void에서 List<OrderProduct>로 변경했다.  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.


- changeOrder  
  주문 상품을 바뀐 부분을 수정하는 게 아니라 교체하는 로직이어서 메서드 명을 updateOrder에서 변경했다.  
  식별자가 없을 때, 발생하는 예외는 데이터 정합성이 깨져서 발생하는 예외이므로 500번 에러를 반환하기 위해 커스텀 예외를 만들어 500 에러를 반환했다.


- deleteOrder  
  멱등성을 위해 조회 후 존재하지 않으면 이후의 로직을 실행하지 않고 끝낸다.  
  두 번째 조회에서 Delivery만 Fetch Join에서 Payment도 같이 Fetch Join으로 변경했다.  
  OrderProduct를 Fetch Join하지 않은 이유는 JDBC는 조회를 하지 않아도 추가 쿼리가 나오지 않아서 Fetch Join하지 않았다.


- changeOrder, cancelOrder, deleteOrder  
  loginId를 파라미터에 추가해 주문의 주인이 맞는지 검증한다.


## Order
- getOrderProductsAsMap  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.  
  productNumber가 없을 때, 발생하는 예외는 데이터 정합성이 깨져서 발생하는 예외이므로 500번 에러를 반환하기 위해 커스텀 예외를 만들어 500 에러를 반환했다.


- changeOrder
  주문 상품만 변경해서 updateOrder를 changeOrder로 변경했다.


- isOwner  
  주문의 memberLoginId와 서비스에서 받은 loginId가 일치하는지 검증한다.

