# 리팩토링할 때, 할일
5원칙 지키기

## 상품 도메인
- [addCategoryProduct](../../src/main/java/lsk/commerce/domain/Product.java)

  해당 메서드 위치 고민, 여기서 카테고리도 카테고리 상품에 추가하는 게 맞는지?

## 결제 api (이번주 안에 할일)
- Controller 분리 (회원, 상품, 카테고리, 주문, 결제)
- Controller용 Dto 생성 (Product에서 currency, Payment에서 paymentId 처리)
- PaymentService, 결제 api용으로 추가한 코드 수정 및 이후 로직