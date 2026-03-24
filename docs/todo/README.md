# 리팩토링할 때, 할일
5원칙 지키기  
n + 1 문제 해결하기  


## 상품 도메인
- [addCategoryProduct](../../src/main/java/lsk/commerce/domain/Product.java)  
  해당 메서드 위치 고민, 여기서 카테고리도 카테고리 상품에 추가하는 게 맞는지?


## 주문
상품 가격이 변해도 주문 금액이 변하지 않는 주문 테스트를 진행하다 결제하지 않은 주문은 언제까지 수정된 상품의 가격으로 적용되지 않게 해야 하는지 고민이 생겼다.


## 카테고리 상품
- disconnectAll 카테고리에 연결된 상품 수 만큼 카테고리 상품 조회 쿼리가 추가로 나온다  
  원인: 상품의 개수가 많을 때, 카테고리에 카테고리 상품을 Fetch Join을 하면 데이터 낭비가 심할 수 있기 때문에 Fetch Join을 하지 않아서 생긴 문제  
  문제가 발생한 부분: Product의 removeCategoryProduct 메서드의 for 문의 category.getCategoryProducts().remove(categoryProduct)에서 발생했다.
