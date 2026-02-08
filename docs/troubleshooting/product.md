# Product에서 해결한 문제
## 중복 쿼리, N+1 문제
- POST /products PK 생성에 쿼리가 많이 나온다.  
  원인: PK 생성 전략을 설정하지 않아서 생긴 문제

  select next_val as id_val from product_seq for update  
  update product_seq set next_val= ? where next_val=?  
  select next_val as id_val from category_product_seq for update (카테고리 수만큼 반복)  
  update category_product_seq set next_val= ? where next_val=? (카테고리 수만큼 반복)  

  해결: MySQL의 전용 기능인 AUTO_INCREMENT를 활용하는 IDENTITY 전략을 선택했다.  
  추가 문제: 카테고리가 여러 개 들어오면 카테고리 수만큼의 select 쿼리가 나온다.  
  원인: for each문으로 카테고리 수만큼 카테고리 조회해서 생긴 문제

- DELETE /products/{productName} select 쿼리가 네 개가 나온다.  
  원인: 상품을 조회하고 -> 카테고리 상품 -> 카테고리 -> 카테고리 상품으로 접근해서 생긴 문제  
  해결: 상품에서 연결되는 카테고리의 수는 적으므로 카테고리 상품과 카테고리를 left join fetch해서 select 쿼리를 두 개로 줄였다.  
  추가 문제: 카테고리 상품 수 + 1만큼의 delete 쿼리가 나온다. (상품 저장에서는 카테고리 상품 수 + 1만큼의 select 쿼리가 나온다.)  
  유지: 상품 저장과 삭제는 일반 회원이 사용하는 기능이 아니고, 관리자가 사용하는 기능이므로 자주 발생하는 작업이 아니므로 성능에 큰 부담을 주지 않는다.

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

- POST /products/{productName}/{categoryName}에서 Response로 카테고리에서 상품을 조회하는 DTO를 반환해서 select 쿼리가 한 번 더 나오는 문제  
  해결: DTO를 상품의 카테고리 상품이 나오게 바꾼다. - 상품이 한 개이므로 조회할 때 left join fetch로 카테고리 상품을 가져온다.

      ProductWithCategoryResponse
      private String name;
      private List<CategoryNameResponse> categoryNames = new ArrayList<>();

      CategoryNameResponse
      private String categoryName;

## 테스트
- wrongCategory_register 저장되지 않은 카테고리를 상품에 넣을 때 InvalidDataAccessApiUsageException 예외가 발생한다.  
  @ParameterizedTest를 사용하려면 IllegalArgumentException으로 바꿔야 한다.  
  시도한 방법: findCategories.containsAll(categories) - Call to 'list. containsAll(collection)' may have poor performance 성능이 저하될 수 있다.  
  해결: set으로 바꿔서 찾은 카테고리 수가 일치하지 않으면 저장되지 않은 카테고리가 있으므로 IllegalArgumentException 예외가 발생하게 한다.

      CategoryService
      protected void validateCategories(List<Category> categories) {
          if (categories == null || categories.isEmpty()) {
              throw new IllegalArgumentException("카테고리가 존재하지 않습니다.");
          }

          Set<Long> categoriesIds = categories.stream()
                  .map(c -> c.getId())
                  .collect(toSet());

          Long findCategoryCount = categoryRepository.countCategories(categoriesIds);
          if (categoriesIds.size() != findCategoryCount) {
              throw new IllegalArgumentException("존재하지 않는 카테고리가 있습니다.");
          }
      }

      CategoryRepository
      public Long countCategories(Set<Long> categoryIds) {
          return em.createQuery(
                          "select count(c) from Category c" +
                                  " where c.id in :categoryIds", Long.class)
                  .setParameter("categoryIds", categoryIds)
                  .getSingleResult();
      }
