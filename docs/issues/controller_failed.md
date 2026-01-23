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