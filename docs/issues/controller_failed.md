# 컨트롤러에서 발생한 예외와 문제
- getSingleResult();를 사용해서 발생하는 NoResultException 예외

  em.createQuery().getResultStream().findFirst().orElse(null);로 해결

  https://stackoverflow.com/questions/2002993/jpa-getsingleresult-or-null


- org.springframework.http.converter.HttpMessageConversionException: Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]

  엔티티로 반환해서 생긴 문제

  DTO 반환으로 변경 -> 현재 생긴 추가 문제 : CategoryForm에서 계층 구조를 나타내지 못함


- java.lang.IllegalArgumentException: 잘못된 상품입니다. product.name: 하얀 그리움 at lsk.commerce.controller.form.ProductForm.productChangeForm(ProductForm.java:60)

  CategoryProduct에서 Product로 꺼내서 Album, Book, Movie로 다운 캐스팅 못해서 생긴 문제

      Category category = categoryService.findCategoryByName(categoryName);
      List<CategoryProduct> categoryProducts = category.getCategoryProducts();
      List<ProductForm> productForms = new ArrayList<>();

      for (CategoryProduct categoryProduct : categoryProducts) {
          ProductForm productForm = ProductForm.productChangeForm(categoryProduct.getProduct());
          productForms.add(productForm);
      }

  만들어뒀던 categoryService.findProductByCategoryName() 사용

      List<Product> products = categoryService.findProductsByCategoryName(categoryName);
      List<ProductForm> productForms = new ArrayList<>();

      for (Product product : products) {
          ProductForm productForm = ProductForm.productChangeForm(product);
          productForms.add(productForm);
      }


- Uncaught (in promise) SyntaxError: Unexpected token ']', ..."Products":]}}]}}]}}]"... is not valid JSON

  상품에 카테고리 추가해서 발생한 무한 루프 - PaymentController에서 Dto가 아닌 도메인 엔티티 반환

  Dto 반환으로 변경


- java.lang.IllegalArgumentException: 잘못된 상품입니다. product.name: 자바 ORM 표준 JPA 프로그래밍 at lsk.commerce.dto.response.ProductResponse.productChangeDto(ProductResponse.java:54) ~[main/:na]

  상품이 instanceof로 구분되지 않는 문제

      if (product instanceof Album album) {
          return new ProductResponse(album.getName(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null);
      } else if (product instanceof Book book) {
          return new ProductResponse(book.getName(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null);
      } else if (product instanceof Movie movie) {
          return new ProductResponse(movie.getName(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getDirector(), movie.getActor());
      } else {
          throw new IllegalArgumentException("잘못된 상품입니다. product.name: " + product.getName());
      }

  product.getClass().getName();의 결과가 lsk.commerce.domain.Product$HibernateProxy 프록시로 나와서 생긴 문제

      Object unproxiedProduct = Hibernate.unproxy(product);
      if (unproxiedProduct instanceof Album album) {
          return new ProductResponse(album.getName(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null);
      } else if (unproxiedProduct instanceof Book book) {
          return new ProductResponse(book.getName(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null);
      } else if (unproxiedProduct instanceof Movie movie) {
          return new ProductResponse(movie.getName(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getDirector(), movie.getActor());
      } else {
          throw new IllegalArgumentException("잘못된 상품입니다. product: " + unproxiedProduct.getClass().getName());
      }

  Hibernate.unproxy로 실제 객체 가져와서 해결


- ERROR 16460 --- [commerce] [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: 로그인을 해야 접근할 수 있습니다.] with root cause

  Postman에서는 로그인을 해서 쿠키가 있지만, 웹에서는 로그인을 하지 않아서 쿠키가 없어서 생긴 문제

  임시 해결: web용 로그인 추가


- lsk.commerce.api.portone.SyncPaymentException at lsk.commerce.controller.PaymentController.lambda$syncPayment$6(PaymentController.java:100) ~[main/:na]

      System.out.println(payment.getOrderName());
      System.out.println(orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건");
      System.out.println(payment.getAmount().getTotal());
      System.out.println(orderRequest.getTotalAmount());

      //결제로 주문한 상품의 이름과 상품의 이름이 같은지, 결제한 총 금액과 상품의 가격이 같은지 (주문 금액으로 변경), 결제한 화폐와 상품의 화폐가 같은지
      return payment.getOrderName().equals(orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건") &&
              payment.getAmount().getTotal() == orderRequest.getTotalAmount();
      }

  로그를 찍어서 앞에 로직이 다 정상적으로 통과돼서 리턴값 확인 - 주문 상품 종류가 1개인 경우 이름이 달라져서 생긴 문제

  book
  book 외 0건
  2000
  2000

  해결: 주문 상품의 종류가 한 개일 때 조건 추가 / 주문 상품을 안 넣는 경우도 차단

      if (orderRequest.getOrderProducts().size() == 1) {
          return payment.getOrderName().equals(orderRequest.getOrderProducts().getFirst().getName()) &&
                  payment.getAmount().getTotal() == orderRequest.getTotalAmount();
      } else {
          return payment.getOrderName().equals(orderRequest.getOrderProducts().getFirst().getName() + " 외 " + (orderRequest.getOrderProducts().size() - 1) + "건") &&
                  payment.getAmount().getTotal() == orderRequest.getTotalAmount();
      }


- java.lang.IllegalArgumentException: 해당 상품의 카테고리가 아닙니다. at lsk.commerce.domain.Product.removeCategoryProduct(Product.java:118)

      public CategoryProduct removeCategoryProduct(Category category) {
          for (CategoryProduct categoryProduct : category.getCategoryProducts()) {
              if (this.equals(categoryProduct.getProduct())) {
                  this.categoryProducts.remove(categoryProduct);
                  category.getCategoryProducts().remove(categoryProduct);
                  return categoryProduct;
              }
          }

          throw new IllegalArgumentException("해당 상품의 카테고리가 아닙니다.");
      }

  this: lsk.commerce.domain.product.Book, getProduct: lsk.commerce.domain.Product$HibernateProxy 프록시로 나와서 return으로 못 빠지는 문제

  해결: Id값으로 비교 this.getId().equals(categoryProduct.getProduct().getId())


- Soft Delete 적용 중 발생한 문제

  @SoftDelete 사용 -> Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.metamodel.UnsupportedMappingException: To-one attribute (lsk.commerce.domain.OrderProduct.order) cannot be mapped as LAZY as its associated entity is defined with @SoftDelete

  @SQLRestriction, @SQLDelete 사용 -> java.sql.SQLIntegrityConstraintViolationException: Cannot delete or update a parent row: a foreign key constraint fails (`commerce`.`orders`, CONSTRAINT `FKtkrur7wg4d8ax0pwgo0vmy20c` FOREIGN KEY (`delivery_id`) REFERENCES `delivery` (`delivery_id`))

  delivery의 cascade를 PERSIST로 변경 -> org.hibernate.TransientObjectException: persistent instance references an unsaved transient instance of 'lsk.commerce.domain.Order' (save the transient instance before flushing)

  Member, Delivery Order 연관 메서드 생성 -> 그대로