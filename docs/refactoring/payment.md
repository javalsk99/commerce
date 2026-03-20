# Payment 리팩토링
## PaymentController
- completePayment  
  Mono 안에 ResponseEntity와 Result를 추가했다.


## SyncPaymentException
- 예외가 발생하는 곳 마다 어떤 이유로 발생하는 지 메시지를 추가했다.