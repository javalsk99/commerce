# 결제 연동 중 발생한 예외
- java.lang.NumberFormatException  
  String의 paymentId를 Long 타입인 Id 값으로 변환하다 생긴 문제  
  임시 해결: String 타입의 paymentId를 추가했다.


- jakarta.persistence.NoResultException  
  String 타입의 paymentId로 Payment를 조회할 때, getSingleResult();로 해서 생긴 문제  
  임시 해결: getResultList().stream().findFirst();로 변경했다.


- lsk.commerce.api.portone.SyncPaymentException at lsk.commerce.controller.PaymentController.lambda$syncPayment$3(PaymentController.java:100) ~[main/:na]  
  자바스크립트에서 상품을 주문으로 바꾸고, 로직 검증에서 상품을 주문으로 바꾸지 않아서 생긴 문제  
  해결: 주문으로 바꾸고, PaymentCustomData의 product도 orderId로 변경했다.  


- OrderForm의 OrderProductForm 변환 중 발생한 LazyInitializationException  
  for문에서 지연 로딩된 데이터를 사용해서 생긴 문제  
  해결: 서비스에서 트랜잭션 속에서 Form으로 변환시키고 컨트롤러에 사용했다.


- paymentId가 null이여서 주문을 결제해도 주문과 결제가 연결되지 않는 상황  
  해결: CompletePaymentRequest에 생성자를 추가해서 OrderController의 requestPayment로 결제 요청을 하면 paymentId를 랜덤값으로 생성해 주문과 결제를 연결시켰다.


- 결제 완료 후 DB에 결제 저장하는 문제  
  해결: AI의 도움으로 비동기 처리 후 트랜잭션으로 결제를 DB에 저장하게 변경했다.

      return Mono.fromCallable(() -> paymentService.completePayment(paymentId, paymentDate))
              .subscribeOn(Schedulers.boundedElastic())
              .map(updatePayment -> PaymentForm.paymentChangeForm(updatePayment));

  1. Mono.fromCallable: 나중에 결과가 필요할 때 시작 (예약)
  2. subscribeOn(Schedulers.boundedElastic()): DB에 결제 정보를 저장하는 데 오래 걸려서 DB 전용 쓰레드 지정
  3. paymentService.completePayment(): DB에 결제 정보 저장
  4. map(): LazyInitializationException을 막기 위한 Form 변환