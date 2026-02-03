# Product에서 해결한 문제
## 중복 쿼리, N+1 문제
- POST /products PK 생성에 쿼리가 많이 나온다.  
  원인: PK 생성 전략을 설정하지 않아서 생긴 문제

  select next_val as id_val from product_seq for update    
  update product_seq set next_val= ? where next_val=?  
  select next_val as id_val from category_product_seq for update (카테고리 수만큼 반복)  
  update category_product_seq set next_val= ? where next_val=? (카테고리 수만큼 반복)  

  해결: MySQL의 전용 기능인 AUTO_INCREMENT를 활용하는 IDENTITY 전략을 선택했다.  
  추가 문제: 카테고리가 여러 개 들어오면 카테고리 수만큼 select 쿼리가 나온다.  
  원인: for each문으로 카테고리 수만큼 카테고리 조회해서 생긴 문제

## 로직
- 상품을 만들 때, 카테고리의 부모가 겹치면 부모 카테고리가 중복해서 저장되던 문제  
  해결: stream().anyMatch()로 상품에 카테고리가 연결됐는지 확인하고 연결됐으면 루프를 벗어난다.

      public void addCategoryProduct(List<Category> categories) {
          for (Category category : categories) {
              while (category != null) {
                  Category finalCategory = category;
                  if (categoryProducts.stream().anyMatch(categoryProduct -> categoryProduct.getCategory() == finalCategory)) {
                      break;
                  }

                  connectCategory(category);
                  category = category.getParent();
              }
          }
      }

