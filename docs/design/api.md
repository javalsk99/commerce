# API 명세
![api](images/API_명세.PNG)
- Category

  GET /categories 카테고리 목록 조회

  POST /categories 카테고리 등록

  GET /categories/{categoryId} 카테고리 조회

  POST /categories/{categoryId} 카테고리 수정

  DELETE /categories/{categoryId} 카테고리 삭제

  GET /categories/{categoryId}/products 카테고리에 소속된 상품 조회


- Auth

  POST /login 로그인


- Member

  GET /members 회원 목록 조회

  POST /members 회원 가입

  GET /members/{memberId} 회원 조회

  POST /members/{memberId} 회원 수정

  DELETE /members/{memberId} 회원 삭제


- Order

  POST /orders 주문 생성

  GET /orders/{orderId} 주문 조회

  POST /orders/{orderId}/delivery 배송 시작

  POST /orders/{orderId}/payment 결제 요청


- Product

  GET /products 상품 목록 조회

  POST /products 상품 등록

  GET /products/{productId} 상품 조회

  POST /products/{productId} 상품 수정

  DELETE /products/{productId} 상품 삭제