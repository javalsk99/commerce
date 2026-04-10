# 테스트 실행 중 발생한 예외 및 실패
- org.springframework.dao.InvalidDataAccessApiUsageException: id to load is required for loading  
  회원, 상품 생성 후 저장하지 않아서 id 값이 load되지 않아서 발생한 문제


- order 테스트 중 주문 상품 리스트가 예상대로 들어가지 않는 문제 발생  
  테스트 내용

      @Test
      void order() {
          //given
          Long memberId = createMember1();
          Long albumId = createAlbum();
          Long bookId = createBook();
          Long movieId = createMovie();

          //when
          Long orderId = orderService.order(memberId, Map.of(albumId, 3, bookId, 5, movieId, 2));

          //then
          Order findOrder = orderService.findOrder(orderId);
          then(findOrder.getOrderProducts().size()).isEqualTo(3);
      }

  결과 - expected: 3 but was: 6

      Map.of(albumId, 3, bookId, 5) //3
      Map.of(albumId, 3) //1

  원인 - 수정한 OrderService.order

        for (Map.Entry<Long, Integer> countProductIdEntry : countProductIds.entrySet()) {
            Long productId = countProductIdEntry.getKey();
            int count = countProductIdEntry.getValue();

            //주문 상품 생성
            products.add(productService.findProduct(productId));
            for (Product product : products) {
                orderProducts.add(OrderProduct.createOrderProduct(product, count));
            }
        }

  해결 - 1, 3, 6으로 증가하는 방식이 for 문에서 발생했을 것이라 예상

        for (Map.Entry<Long, Integer> countProductIdEntry : countProductIds.entrySet()) {
            Long productId = countProductIdEntry.getKey();
            int count = countProductIdEntry.getValue();

            //주문 상품 생성
            Product product = productService.findProduct(productId);
            orderProducts.add(OrderProduct.createOrderProduct(product, count));
        }


- java.util.ConcurrentModificationException 동시 수정이 일어날 때 발생하는 예외  
  원인 - OrderProduct.deleteOrderProduct를 Order.changeOrder에서 사용하고, OrderService에서 Order.changeOrder 사용해서 발생한 것으로 예상

      public static void deleteOrderProduct(Order order, OrderProduct orderProduct) {
          orderProduct.product.addStock(orderProduct.count);
          order.getOrderProducts().remove(orderProduct);

      public static Order changeOrder(Order order, List<OrderProduct> newOrderProducts) {
          for (OrderProduct orderProduct : order.getOrderProducts()) {
              OrderProduct.deleteOrderProduct(order, orderProduct);
          }

      public void changeOrder(Order order, Map<Long, Integer> newProductIdsCount) {
          Order.changeOrder(order, newOrderProducts);

  해결 - OrderProduct.deleteOrderProduct를 Order.changeOrder를 거치지 않고 OrderService에서 사용

      public static void deleteOrderProduct(Order order) {
          for (OrderProduct orderProduct : order.getOrderProducts()) {
              orderProduct.product.addStock(orderProduct.count);
          }
          order.getOrderProducts().removeAll(order.getOrderProducts());
      }

      public void changeOrder(Order order, Map<Long, Integer> newProductIdsCount) {
          OrderProduct.deleteOrderProduct(order);


- completeDelivery가 실행되지 않는 문제 - this로 트랜잭션이 동작하지 않음

      public Payment completePayment(String paymentId, LocalDateTime paymentDate) {
          TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                  scheduler.schedule(() -> deliveryService.startDelivery(orderId), Instant.now().plusSeconds(60));
              }
          });

      public void startDelivery(Long orderId) {
          Order order = orderService.findOrder(orderId);
          order.getDelivery().startDelivery();

              @Override
              public void afterCommit() {
                  DeliveryService deliveryService = applicationContext.getBean(DeliveryService.class);
                  scheduler.schedule(() -> completeDelivery(orderId), Instant.now().plusSeconds(60));
              }
          });
      }

      public void completeDelivery(Long orderId) {
          Order order = orderService.findOrder(orderId);
          order.getDelivery().completeDelivery();
      }

  시도한 방법: DeliveryService 직접 주입 - 무한 루프 발생

  The dependencies of some of the beans in the application context form a cycle:  
  orderController defined in file [commerce\build\classes\java\main\lsk\commerce\controller\OrderController.class]  
  ↓  
  paymentService defined in file [commerce\build\classes\java\main\lsk\commerce\service\PaymentService.class]  
  ┌─────┐  
  |  deliveryService defined in file [commerce\build\classes\java\main\lsk\commerce\service\DeliveryService.class]  
  └─────┘

  해결: @TransactionalEventListener로 서비스와 이벤트 분리

      public Payment completePayment(String paymentId, LocalDateTime paymentDate) {
          eventPublisher.publishEvent(new PaymentCompletedEvent(orderId));

      public void startDelivery(Long orderId) {
          Order order = orderService.findOrder(orderId);
          order.getDelivery().startDelivery();

          eventPublisher.publishEvent(new DeliveryStartedEvent(orderId));
      }

      public void completeDelivery(Long orderId) {
          Order order = orderService.findOrder(orderId);
          order.getDelivery().completeDelivery();
      }

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void startDelivery(PaymentCompletedEvent event) {
          scheduler.schedule(() -> deliveryService.startDelivery(event.orderId()), Instant.now().plusSeconds(60));
      }

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void completeDelivery(DeliveryStartedEvent event) {
          scheduler.schedule(() -> deliveryService.completeDelivery(event.orderId()), Instant.now().plusSeconds(60));
      }


- 주문을 soft delete하고 주문을 찾을 때 예외가 발생한다.

      @Test
      void delete() {
          //given
          orderService.cancelOrder(orderNumber);

          //when
          orderService.deleteOrder(orderNumber);

          //then
          assertThrows(IllegalArgumentException.class, () ->
                  orderService.findOrderWithAll(orderNumber));
      }

  결과: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <org.springframework.dao.InvalidDataAccessApiUsageException>  
  persistent instance references an unsaved transient instance of 'lsk.commerce.domain.Order' (save the transient instance before flushing)

  원인 추측: 주문을 soft delete해서 주문을 찾을 수 없는데 Fetch Join을 해서 문제인 것으로 추측  
  시도한 방법: Fetch Join없이 주문을 찾는 비즈니스 로직이 없어서 직접 쿼리를 짜서 주문만 조회

      assertThrows(IllegalArgumentException.class, () ->
              em.createQuery("select o from Order o where o.orderNumber = :orderNumber", Order.class)
                      .setParameter("orderNumber", orderNumber)
                      .getResultStream()
                      .findFirst());

  결과: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <org.hibernate.TransientObjectException>  
  persistent instance references an unsaved transient instance of 'lsk.commerce.domain.Order' (save the transient instance before flushing)  
  예외는 변했지만 발생한 원인은 변하지 않았다.

  원인 추측: soft delete는 update로 작동하므로 em.flush를 사용해야 하는 것으로 추측

      orderService.deleteOrder(orderNumber);
      em.flush();
      em.clear();

  결과: em.flush에서 org.hibernate.TransientObjectException: persistent instance references an unsaved transient instance of 'lsk.commerce.domain.Order' (save the transient instance before flushing) 예외 발생

  시도한 방법: em.flush, em.clear를 지우고 deleted = true인 주문을 검색

      assertThrows(IllegalArgumentException.class, () ->
              em.createQuery("select o from Order o where o.orderNumber = :orderNumber and o.deleted = 'true'", Order.class)
                      .setParameter("orderNumber", orderNumber)
                      .setParameter("true", true)
                      .getResultStream()
                      .findFirst());

  결과: 성공했지만 주문을 조회할 때 deleted = true로 하지 않으므로 deleted = true는 지워야 된다.

  ai의 추천 해결책: em.flush를 사용하지 않고 em.clear만 사용하기

      orderService.deleteOrder(orderNumber);
      em.clear();

  ai의 도움을 받아 이해한 내용: 주문 상품을 저장하는 JDBC는 영속성 컨텍스트를 사용하지 않고 DB에 즉시 반영되고, 주문은 영속성 컨텍스트를 사용해 em.flush를 사용하면 영속성 컨텍스트에 주문 상품이 존재하지 않아서 TransientObjectException 예외가 발생한다.  
  그래서 em.clear로 영속성 컨텍스트를 비우고 조회 시 원하는 예외가 발생했다.

  주문 생성에서는 em.flush가 예외를 발생시키지 않았는데 soft delete에서 예외를 발생시킨 이유  
  주문 생성: JPA가 주문 저장만 하고 끝내서 문제를 일으키지 않는다.  
  soft delete: JDBC로 주문 상품은 soft delete 되었지만, JPA 영속성 컨텍스트에는 주문 엔티티에 주문 상품 리스트가 남아있어서 예외가 발생한다.

  헷갈리는 부분: 주문 상품의 수정 사항을 JDBC로 즉시 DB에 반영하고, 주문을 수정했는데 주문도 같이 반영이 된다. (em.clear를 사용했으므로 주문의 수정 사항은 지워져야 한다.)  
  JDBC와 JPA를 같이 사용해서 JPA의 쓰기 지연을 해제시켰다. (JDBC로 인해 발생한 flush는 한 트랜잭션안에서 작동해 DB에 수정 사항이 있을거라 생각하지 않아서 예외가 발생하지 않는다. 그래서 트랜잭션이 끝나면 em.clear로 영속성 컨텍스트를 비워야 한다.)

  결론: JDBC를 사용하면 JPA 영속 컨텍스트와 DB가 달라지므로 em.clear를 사용해야 한다. / JDBC를 사용하기 전에 수정 사항이 있으면 em.flush를 사용하고 JDBC를 사용해야 한다.


- 주문을 찾지 못하는 문제 발생

      @Test
      void delete_deliveredOrder() {
          //given
          Order order = paymentService.request(orderNumber);
          paymentService.completePayment(order.getPayment().getPaymentId(), LocalDateTime.now());

          deliveryService.startDelivery(orderNumber);
          deliveryService.completeDelivery(orderNumber);

          //when
          orderService.deleteOrder(orderNumber);

          //then
          assertThrows(IllegalArgumentException.class, () ->
                  orderService.findOrderWithDeliveryPayment(orderNumber));
      }

  deliveryService.startDelivery(orderNumber)에 있는 orderService.findOrderWithDelivery(orderNumber)에서 주문을 찾지 못한다.  
  java.lang.IllegalArgumentException: 존재하지 않는 주문입니다.

  원인 추측: DeliveryService 트랜잭션에 있는 REQUIRES_NEW 때문으로 추측 (deliveryService.startDelivery(orderNumber) 위에 orderService.findOrderWithDelivery(orderNumber)를 돌려봐도 잘 작동한다.)  
  의문: 애플리케이션에서는 이 부분에서 문제가 생기지 않는다.  
  윈인: 테스트에서의 @Transaction은 테스트 완료 후 롤백시키기 위해 커밋을 바로 하지 않아서 REQUIRES_NEW에서 데이터를 찾을 수 없다.  
  해결: TestTransaction.flagForCommit으로 강제로 커밋시킨다.

      paymentService.completePayment(order.getPayment().getPaymentId(), LocalDateTime.now());

      TestTransaction.flagForCommit();
      TestTransaction.end();

      deliveryService.startDelivery(orderNumber);

  추가 문제: 강제로 커밋시켜서 데이터가 롤백되지 않아서 이 후의 테스트가 실패한다.  
  해결: OrderServiceTest에는 테스트가 10개가 넘는다. 이 테스트 때문에 afterEach로 모든 테스트에 데이터를 지우는 것이 좋지 않으므로 새 테스트 클래스를 만들어서 테스트한다.


- 비동기 테스트 중 검증에 실패하는 문제 발생

      //when
      paymentSyncService.syncPayment(order.getPayment().getPaymentId());

      //then
      thenSoftly(softly -> {
              () -> then(paymentClient).should().getPayment(anyString()),
              () -> then(paymentService).should().verifyAndComplete(any(PaidPayment.class)),
              () -> then(paymentService).should().getPaymentRequest(any(Payment.class))
      );

  원인: 비동기 방식의 Mono는 구독하지 않으면 작동을 하지 않는다.  
  해결: 구독해서 작동을 시킨다.

      Mono<PaymentRequest> paymentRequestMono = paymentSyncService.syncPayment(order.getPayment().getPaymentId());
      paymentRequestMono.subscribe();

  테스트는 성공하지만 검증이 가능한 StepVerifier로 변경한다.

      StepVerifier.create(paymentSyncService.syncPayment(order.getPayment().getPaymentId()).log())
              .assertNext(paymentRequest ->
                      then(paymentRequest)
                              .extracting("paymentId", "paymentStatus")
                              .containsExactly(order.getPayment().getPaymentId(), PaymentStatus.COMPLETED)
              )
              .verifyComplete();

  추가 문제: PaymentStatus가 변경되지 않는다.  
  원인: PaymentStatus 변경은 PaymentService에 있는 verifyAndComplete 메서드 내부에 있어서 변경되지 않는다.  
  해결: willAnswer를 사용해서 PaymentStatus를 변경한다.

      given(paymentService.verifyAndComplete(any(PaidPayment.class))).willAnswer(invocation -> {
          Payment payment = order.getPayment();
          payment.complete(LocalDateTime.now());
          return payment;
      });
      given(paymentService.getPaymentRequest(any(Payment.class))).willAnswer(invocation -> new PaymentRequest(order.getPayment().getPaymentId(), order.getPayment().getPaymentStatus()));


- AuthControllerTest의 wrongLoginIdOrPassword 테스트에서 실패하는 문제 발생  
  원인 추측: 500번대 에러는 서버의 문제이고 400번대 에러는 클라이언트의 잘못된 요청이어서 500번대 에러는 테스트에 실패하는 것으로 추측

  jakarta.servlet.ServletException: Request processing failed: java.lang.IllegalArgumentException: 아이디 또는 비밀번호가 틀렸습니다

  해결: 서버의 문제가 아닌 예외가 서버의 문제로 발생하는 500번대 에러로 발생하는 것을 @RestControllerAdvice를 통해 클라이언트의 잘못된 요청으로 발생하는 400번대 에러로 변경하면서 보안과 서버의 안정성을 확보했다.

      @RestControllerAdvice
      public class GlobalExceptionHandler {

          @ExceptionHandler(IllegalArgumentException.class)
          public ResponseEntity<ErrorResult> illegalArgumentExHandle(IllegalArgumentException e) {
              return ResponseEntity.badRequest().body(new ErrorResult("BAD_ARGUMENT", e.getMessage()));
          }

      public record ErrorResult(
              String code,
              String message
      ) {
      }


## 통합 테스트
- org.springframework.orm.jpa.JpaSystemException: A problem occurred in the SQL executor : Error advancing (next) ResultSet position [Operation not allowed after ResultSet closed]  
  원인 추측: 비동기, REQUIRES_NEW 환경 때문에 @Transactional을 사용하지 않아서 발생한 것으로 추측  
  해결: 리포지토리에서 getResultStream()을 getResultList().stream()으로 변경했다.

      public Optional<Order> findWithAll(String orderNumber) {
          return em.createQuery(
                          "select o from Order o" +
                                  " join fetch o.delivery" +
                                  " left join fetch o.payment" +
                                  " left join fetch o.orderProducts op" +
                                  " join fetch op.product" +
                                  " join fetch o.member" +
                                  " where o.orderNumber = :orderNumber", Order.class)
                  .setParameter("orderNumber", orderNumber)
                  .getResultList()
                  .stream()
                  .findFirst();
      }

  getResultList().stream()과 getResultStream()의 차이  
  getResultList().stream()은 DB에서 데이터를 List로 가져와서 Stream으로 변환해서 커넥션을 바로 반납한다.  
  getResultStream()은 DB에서 데이터를 Stream으로 반환해서 메모리는 적게 차지하지만 커넥션을 오래 가지고 있는다.
  그래서 @Transactional을 사용하지 않는 해당 테스트와 적은 메모리와 많은 인원이 사용하는 커머스 프로젝트에서는 커넥션을 길게 가지고 있는 getResultStream()이 적합하지 않는다.


- 결제 완료 테스트 중 예외 발생 lsk.commerce.api.portone.SyncPaymentException: 결제 정보 조회 중 오류 발생  
  원인: 자바스크립트를 통해 포트원 서버에 주문과 결제 정보를 등록해야 한다.  
  결론: 결제 완료 테스트를 진행하려면 자바스크립트를 통해 포트원 서버에 결제 정보를 등록해야 하는데, 통합 테스트에는 자바스크립트가 작동하지 않아서 통합 테스트에서 진행할 수 없다.  
  해결: E2E 테스트에서 Playwright를 통해 자바스크립트를 작동시켰다.

      try (Playwright playwright = Playwright.create()) {
          Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
          BrowserContext context = browser.newContext();
          context.addCookies(Arrays.asList(new Cookie("jjwt", token)
                  .setDomain("localhost")
                  .setPath("/")
          ));
          Page page = context.newPage();

          page.navigate("http://localhost:" + port + "/payments/" + orderNumber);

  추가 문제: 결제를 진행하지 않고, PaymentController의 completePayment를 요청해서 결제가 실패된다.  
  2026-03-27T23:27:25.366+09:00 ERROR 7692 --- [-1 @coroutine#6] l.commerce.service.PaymentSyncService    : 결제 실패 ReadyPayment(id=e9fca02d-1572-4bc1-acfe-219c69d8bf62,

  시도한 방법: 자바스크립트로 결제 완료 시 작동하는 로직을 실행 - 이 방법도 위의 방법과 같이 결제를 진행하지 않고 completePayment를 요청하는 것이므로 실패했다.  
  시도한 방법: Playwright의 테스트 자동화 기능인 page.pause();로 잠시 멈추고 결제를 진행 - 테스트는 성공했지만, 테스트를 진행할 때마다 직접 결제를 진행해야 한다.  
  시도한 방법: 결제대행사를 카카오페이에서 토스페이먼츠로 변경 - 둘 다 테스트 모드지만 실제로 결제하는 과정이 필요해서 실패했다.  
  임시 해결: E2E 테스트에서 Mocking하면 실제 API가 잘 작동하는지 확인할 수 없지만, 결제 완료에서는 필요한 상황이라 생각해서 PaidPayment를 직접 생성하고, PaymentClient를 Mocking해서 진행한다.

      PaidPayment paidPayment = new PaidPayment(paymentId, "transactionId", "merchantId", "storeId", null, new SelectedChannel(SelectedChannelType.Test.INSTANCE, null, null, null, PgProvider.Tosspayments.INSTANCE, "iamporttest_3"), null, PortOneVersion.V2.INSTANCE, null, null, null, Instant.now(), Instant.now(), Instant.now(), orderName, new PaymentAmount(139000L, 0L, null, null, 0L, 139000L, 0L, 0L), Currency.Krw.INSTANCE, new Customer(null, null, null, null, null, null, null, null), null, null, null, null, null, "{\"orderNumber\":\"" + orderNumber + "\"}", null, Instant.now(), null, null, null, null);
      given(portone.getPayment(anyString())).willReturn(CompletableFuture.completedFuture(paidPayment));
