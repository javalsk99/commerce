# Delivery에서 해결한 문제
## N+1 문제
- DeliveryService의 startDelivery, completeDelivery에서 발생한 N+1 문제
  해결: 주문에서 배송을 Fetch Join으로 가져와서 select 쿼리를 줄였다.