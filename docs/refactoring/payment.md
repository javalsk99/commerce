# Payment 리팩토링
## PaymentController
- completePayment  
  Mono 안에 ResponseEntity와 Result를 추가했다.


- requestPayment  
  결제를 생성하는 것이므로 OrderController에서 이동하고, 리소스 경로를 "/payments/orders/{orderNumber}"로 변경했다.  
  응답용 DTO PaymentResponse를 추가했다.


- requestPayment, getOrder, completePayment, handleWebhook  
  @Login으로 loginId을 쿠키에서 꺼내서 PaymentSyncService와 PaymentService로 보낸다.


## SyncPaymentException
- 예외가 발생하는 곳 마다 어떤 이유로 발생하는 지 메시지를 추가했다.


- syncPayment  
  loginId를 파라미터에 추가해 PaymentService로 보낸다.


## PaymentService
- request  
  Delivery와 Payment만 필요해서 Delivery와 Payment만 가져오는 Fetch Join으로 변경하고 반환을 Order에서 Payment로 변경했다.


- request, verifyAndComplete, verifyPayment, verifyOrderProducts  
  loginId를 파라미터에 추가해 주문의 주인이 맞는지 검증한다.

  