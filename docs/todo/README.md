# 리팩토링할 때, 할일
5원칙 지키기  
n + 1 문제 해결하기  
상품을 제거할 때 카테고리 상품에서 카테고리만 제거하고 상품을 제거하지 않음 (작동은 됨) - 상품 제거도 추가하기

## 상품 도메인
- [addCategoryProduct](../../src/main/java/lsk/commerce/domain/Product.java)  
  해당 메서드 위치 고민, 여기서 카테고리도 카테고리 상품에 추가하는 게 맞는지?