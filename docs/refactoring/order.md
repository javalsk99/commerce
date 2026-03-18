# Order 리팩토링
## OrderController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  파라미터로 받는 변수들을 DTO에 담아서 검증을 추가했다.


## OrderService
- order  
  파라미터를 DTO로 변경했다.
  updateOrder와의 중복 로직을 정적 메서드로 추출했다.


- order, updateOrder  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.
