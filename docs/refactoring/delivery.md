# Delivery 리팩토링
## DeliveryEventListener
- delaySeconds를 추가해 테스트 코트에서는 5초로 짧게 테스트할 수 있게 변경했다.


## DeliveryService
- startDelivery, completeDelivery  
  Payment 추가 쿼리가 나와서 Payment도 같이 Fetch Join하는 것으로 변경했다.