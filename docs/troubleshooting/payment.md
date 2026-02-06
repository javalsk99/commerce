# Payment에서 해결한 문제
## 중복 쿼리, N+1 문제
- GET /api/payments/{orderNumber} select 쿼리가 여섯 번 나온다. (주문, 주문 상품, 상품, 회원, 결제, 배송)  
  해결: 주문에서 전부 Fetch Join으로 가져온다.


- POST /api/payments/complete 결제 -> 주문 -> 주문 Fetch Join -> 상품 수만큼 -> 결제 -> 주문 -> 배송 순서로 select 쿼리가 나온다.  
  원인: 결제 api에서 제공하는 테스트 코드에서 수정해서 코드가 복잡해져서 생긴 문제  
  일부 해결: 결제 조회할 때 Fetch Join으로 주문을 가져온다. (결제 Fetch Join -> 주문 Fetch Join으로 중간에 주문 select를 없앴다.)

  select 쿼리 흐름: 결제와 주문을 조회 -> 주문과 연관된 엔티티들 조회 (GET /api/payments/{orderNumber}) -> 상품 들 반복 조회 -> 결제 조회 -> 주문 조회 -> 배송 조회  
  일부 해결: 상품을 전체 조회로 변경, 결제를 다시 조회하지 않고 이전에 조회했던 결제 사용 - (결제 Fetch Join -> 주문 Fetch Join -> 상품 -> 배송 순서로 select 쿼리가 나온다.)  
  남은 문제 해결: 처음 결제에서 배송도 같이 Fetch Join 해온다.

  추가 문제: 결제를 다시 조회하지 않아서 update 쿼리가 나오지 않는다.  
  해결: 처음에 있던 불필요한 결제 조회를 없애고 마지막에 결제 조회한다.  
  최종 쿼리 흐름: 주문 Fetch Join select (결제할 주문 읽어 오기) -> 상품 select (주문에 있는 주문 상품이 존재하는 상품인지 검증) -> 결제 Fetch Join select (결제 상태 변경을 위한 조회) -> 배송 update -> 주문 update -> 결제 update (예상한 update 쿼리 순서는 결제 -> 주문 -> 배송이지만 쓰기 지연으로 순서가 바뀐 것으로 추측된다.)