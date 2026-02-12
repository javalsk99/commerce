# Order에서 해결한 문제
## 중복 쿼리, N+1 문제
- POST /orders 상품 수만큼 select 쿼리가 나오고, 상품이 두 개일때 update 쿼리가 insert 쿼리 전에 한 번 나오고 insert 쿼리 끝나고 한 번 나온다.  
  원인: for each 문에서 상품 조회와 수정해서 생긴 문제

      public String order(String memberLoginId, Map<String, Integer> productNamesCount) {
          Member member = memberService.findMemberByLoginId(memberLoginId);

          Delivery delivery = new Delivery(member);

          List<OrderProduct> orderProducts = new ArrayList<>();

          for (Map.Entry<String, Integer> productNameCountEntry : productNamesCount.entrySet()) {
              String productName = productNameCountEntry.getKey();
              int count = productNameCountEntry.getValue();

              Product product = productService.findProductByName(productName);
              orderProducts.add(OrderProduct.createOrderProduct(product, count));
          }

          Order order = Order.createOrder(member, delivery, orderProducts);

          orderRepository.save(order);

          return order.getOrderNumber();
      }

  해결: 상품을 한 번에 조회하고 stream으로 꺼내서 사용해서 상품의 select 쿼리를 한 개로 줄이고, update 쿼리가 마지막에 상품 수만큼 나온다.  
  추가 문제: 상품 수만큼 주문 상품의 insert 쿼리가 나오고, 상품의 update 쿼리가 나온다. (주문을 일반 회원이 하므로 자주 발생해서 줄이는 게 좋다.)  
  시도한 방법: application.properties에 spring.jpa.properties.hibernate.order_inserts, updates = true 추가 - IDENTITY 전략이여서 insert 쿼리가 줄어들지 않고, update 쿼리도 줄어들지 않는다.  
  / update for lsk.commerce.domain.product.Book /update product set name=?, price=?, stock_quantity=?, author=?, isbn=? where product_id=?  
  / update for lsk.commerce.domain.product.Movie /update product set name=?, price=?, stock_quantity=?, actor=?, director=? where product_id=?

  일부 해결: 주문에서의 cascade를 없애고 JdbcTemplate의 batchUpdate를 사용해서 주문 상품을 저장해서 주문 상품의 insert 쿼리를 한 개로 줄였다.  
  [INSERT INTO order_product (order_id, product_id, count, order_price, deleted) VALUES (?, ?, ?, ?, ?)] with a batch size of 2

  문제 확인: 상품의 자식 엔티티인 책의 인스턴스 3개를 만들어서 재고 감소를 해서 update 쿼리가 한 개로 합쳐지는지 확인을 했는데 update 쿼리가 세 개가 나왔다. (상품의 상속 관계가 원인인 것으로 추측)

  / update for lsk.commerce.domain.product.Book /update product set name=?, price=?, stock_quantity=?, author=?, isbn=? where product_id=?
  2026-02-05T12:31:30.932+09:00 TRACE 9616 --- [commerce] [nio-8080-exec-9] org.hibernate.orm.jdbc.bind              : binding parameter (1:VARCHAR) <- [book1]
  2026-02-05T12:31:30.933+09:00 DEBUG 9616 --- [commerce] [nio-8080-exec-9] org.hibernate.SQL                        :
  / update for lsk.commerce.domain.product.Book /update product set name=?, price=?, stock_quantity=?, author=?, isbn=? where product_id=?
  2026-02-05T12:31:30.933+09:00 TRACE 9616 --- [commerce] [nio-8080-exec-9] org.hibernate.orm.jdbc.bind              : binding parameter (1:VARCHAR) <- [book2]
  2026-02-05T12:31:30.934+09:00 DEBUG 9616 --- [commerce] [nio-8080-exec-9] org.hibernate.SQL                        :
  / update for lsk.commerce.domain.product.Book /update product set name=?, price=?, stock_quantity=?, author=?, isbn=? where product_id=?
  2026-02-05T12:31:30.934+09:00 TRACE 9616 --- [commerce] [nio-8080-exec-9] org.hibernate.orm.jdbc.bind              : binding parameter (1:VARCHAR) <- [book3]

  유지: 재고 감소에서 중요한 부분이 변경 감지이고, 락을 사용할 계획이므로 JdbcTemplate을 써서 변경 감지의 기능을 없애지 않는다. (update 쿼리가 많이 나올 수는 있지만, 변경 감지와 락을 사용해 데이터의 무결성을 지키기 위해 유지한다.)


- GET /orders/{orderNumber} select 쿼리가 다섯 번 나온다.  
  해결: 회원에서 주문을 조회한 것처럼 DTO를 직접 조회해서 select 쿼리를 두 번으로 줄였다. (MemberQueryRepository 분리)  
  시도 중 발생한 문제: 주문을 생성하고 결제를 하지 않으면 주문을 가져오지 못한다.  
  원인: 결제를 하지 않으면 결제가 null인 상태인데 join을 해서 가져오지 못해서 생긴 문제  
  해결: left join을 사용해서 결제가 null이여도 조회가 가능해졌다.


- POST /orders/{orderNumber} select 쿼리가 여러 번 나온다.  
  원인: 컨트롤러에서 조회하고, 서비스에서 다시 조회하고, for each문에서도 조회해서 생긴 문제  
  해결: 컨트롤러에서 조회를 없애고, 먼저 주문에서 필요한 것들을 전부 Fetch Join으로 가져와서 기존의 주문 상품과의 연관을 끊고 영속성 컨텍스트를 비우고, 주문 DTO에 필요한 배송과 결제를 Fetch Join으로 가져오고, 상품 전체를 조회해서 for each문에서 꺼내서 사용해 select 쿼리를 줄였다.

      Order order = findOrderWithAll(orderNumber);

      em.flush();
      em.clear();

      Order currentOrder = findOrderWithDeliveryPayment(orderNumber);
      List<Product> currentProducts = productService.findProducts();

      for (Map.Entry<String, Integer> newProductNameCountEntry : newProductNamesCount.entrySet()) {
          String newProductName = newProductNameCountEntry.getKey();
          int newCount = newProductNameCountEntry.getValue();

          Product newProduct = currentProducts.stream()
                  .filter(p -> newProductName.equals(p.getName()))
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + newProductName));
          newOrderProducts.add(OrderProduct.createOrderProduct(newProduct, newCount));
      }

      orderProductJdbcRepository.deleteOrderProductsByOrderId(currentOrder.getId());
      orderProductJdbcRepository.saveAll(newOrderProducts);


- DELETE /orders/{orderNumber} 주문에서 배송과 결제를 Fetch Join으로 가져오게 변경
- POST /orders/{orderNumber}/cancel 주문에서 연관된 모든 엔티티들을 Fetch Join으로 가져오게 변경 (회원 제외)
- POST /orders/{orderNumber}/payments 주문에서 연관된 모든 엔티티들을 Fetch Join으로 가져오게 변경 (회원 제외)


- GET /orders 상품 수만큼 주문이 검색 결과로 나온다.  
  원인: 상품 이름을 조건으로 넣기 위해 OrderProduct와 Product를 .join으로 넣어서 생긴 문제

      .join(order.orderProducts, orderProduct)
      .join(orderProduct.product, product)

  시도한 방법: OrderProductQueryRepository에도 검색을 추가해 상품 검색 조건 추가 - 주문에서 해당 상품만 나오고, 주문에 해당 상품이 없는 경우 상품 없이 주문만 나온다.

      OrderProductQueryRepository
      protected Map<String, List<OrderProductQueryDto>> search(List<String> orderNumbers, String productName) {
          List<OrderProductQueryDto> orderProducts = query.select(Projections.constructor(OrderProductQueryDto.class,
                          order.orderNumber,
                          product.name,
                          product.price,
                          orderProduct.count,
                          orderProduct.orderPrice))
                  .from(orderProduct)
                  .join(orderProduct.order, order)
                  .join(orderProduct.product, product)
                  .where(
                          inOrderNumbers(orderNumbers),
                          eqProductName(productName)
                  )
                  .fetch();

          return orderProducts.stream()
                  .collect(groupingBy(orderProductQueryDto -> orderProductQueryDto.getOrderNumber()));
      }

  해결: OrderProductQueryRepository의 검색을 제거하고, OrderQueryRepository에서 서브쿼리를 이용해 상품 검색 조건 추가

      OrderQueryRepository
      private BooleanExpression containsProductName(String productName) {
          if (!StringUtils.hasText(productName)) {
              return null;
          }

          QOrderProduct subOrderProduct = orderProduct;
          return JPAExpressions.select(Projections.constructor(OrderProductQueryDto.class,
                          order.orderNumber,
                          product.name,
                          product.price,
                          orderProduct.count,
                          orderProduct.orderPrice))
                  .from(subOrderProduct)
                  .where(
                          subOrderProduct.order.eq(order),
                          containsProductNameAndInitial(productName, subOrderProduct)
                  )
                  .exists();
      }
