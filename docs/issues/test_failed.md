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
          assertThat(findOrder.getOrderProducts().size()).isEqualTo(3);
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

  원인 - OrderProduct.deleteOrderProduct를 Order.updateOrder에서 사용하고, OrderService에서 Order.updateOrder 사용해서 발생한 것으로 예상

      public static void deleteOrderProduct(Order order, OrderProduct orderProduct) {
          orderProduct.product.addStock(orderProduct.count);
          order.getOrderProducts().remove(orderProduct);

      public static Order updateOrder(Order order, List<OrderProduct> newOrderProducts) {
          for (OrderProduct orderProduct : order.getOrderProducts()) {
              OrderProduct.deleteOrderProduct(order, orderProduct);
          }

      public void updateOrder(Order order, Map<Long, Integer> newProductIdsCount) {
          Order.updateOrder(order, newOrderProducts);

  해결 - OrderProduct.deleteOrderProduct를 Order.updateOrder를 거치지 않고 OrderService에서 사용

      public static void deleteOrderProduct(Order order) {
          for (OrderProduct orderProduct : order.getOrderProducts()) {
              orderProduct.product.addStock(orderProduct.count);
          }
          order.getOrderProducts().removeAll(order.getOrderProducts());
      }

      public void updateOrder(Order order, Map<Long, Integer> newProductIdsCount) {
          OrderProduct.deleteOrderProduct(order);
