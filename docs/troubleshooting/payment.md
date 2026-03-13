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


- PaymentRepositoryTest - byPaymentId 결제만 조회해도 주문도 같이 조회된다.  
  원인: @OneToOne의 mappedBy는 LAZY로 설정해도 EAGER로 작동한다.

================= WHEN START =================
2026-03-08T22:16:25.069+09:00 DEBUG 16808 --- [    Test worker] org.hibernate.SQL                        :
    select
        p1_0.payment_id,
        p1_0.deleted,
        p1_0.payment_amount,
        p1_0.paid_at,
        p1_0.payment_number,
        p1_0.payment_status
    from
        payment p1_0
    where
        (
            p1_0.deleted = 0
        )
        and p1_0.payment_number=?
2026-03-08T22:16:25.085+09:00 DEBUG 16808 --- [    Test worker] org.hibernate.SQL                        :
    select
        o1_0.order_id,
        o1_0.deleted,
        o1_0.delivery_id,
        o1_0.member_id,
        o1_0.ordered_at,
        o1_0.order_number,
        o1_0.order_status,
        o1_0.payment_id,
        o1_0.total_amount
    from
        orders o1_0
    where
        o1_0.payment_id=?
        and (
            o1_0.deleted = 0
        )
================= WHEN END ===================

  해결: 일반 조회를 주문을 Fetch Join으로 가져오는 것으로 변경한다.

## 결제 연동 중 발생한 예외
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