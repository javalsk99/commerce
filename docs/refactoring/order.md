# Order 리팩토링
## OrderController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  파라미터로 받는 변수들을 DTO에 담아서 검증을 추가했다.


- changeOrder  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


- cancelOrder  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


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


## Order
- getOrderProductsAsMap  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.  
  productNumber가 없을 때, 발생하는 예외는 데이터 정합성이 깨져서 발생하는 예외이므로 500번 에러를 반환하기 위해 커스텀 예외를 만들어 500 에러를 반환했다.

