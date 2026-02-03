# Category에서 해결한 문제
## 중복 쿼리, N+1 문제
- 컨트롤러에서 검증 제거
- POST /categories 같은 이름의 카테고리 등록을 막지 않았다.  
  해결: Member처럼 같은 이름의 카테고리 검증 메서드 추가

      private void validateCategory(Category category) {
          if (CategoryRepository.existsByName(category.getName())) {
              throw new IllegalArgumentException("이미 존재하는 카테고리입니다. name: " + categoryName);
          }
      }

  추가 문제: 부모 카테고리 이름이 없을 땐 select 쿼리가 한 번만 나오지만, 부모 카테고리 이름이 있을 때 select 쿼리가 중복으로 나온다.  
  원인: 도메인에서는 Category parent로 넣어야 되지만, 컨트롤러에서는 Category로 넣을 수 없고 String으로 넣어야 돼서 조회를 추가로 해야 하는 문제

      if (request.getParentName() != null) {
          Category parentCategory = categoryService.findCategoryByName(request.getParentName());
          Category childCategory = Category.createChildCategory(parentCategory, request.getName());
          categoryService.create(childCategory);
          return childCategory.getName() + " created";
      }

      Category parentCategory = Category.createParentCategory(request.getName());
      categoryService.create(parentCategory);
      return parentCategory.getName() + " created";

  해결: 같은 이름의 카테고리가 있는지 검증하면서, 부모 카테고리의 이름이 있으면 같이 조회

      CategoryRepository
      public List<Category> existsByCategoryName(String categoryName, String parentCategoryName) {
          return em.createQuery(
                  "select c from Category c" +
                          " where c.name = :name" +
                          " or (:parentName is not null and c.name = :parentName)", Category.class)
                  .setParameter("name", categoryName)
                  .setParameter("parentName", parentCategoryName)
                  .getResultList();
      }

      CategoryService
      private Category validateCategory(String categoryName, String parentCategoryName) {
          List<Category> categories = categoryRepository.existsByCategoryName(categoryName, parentCategoryName);
          if (categories.stream().anyMatch(c -> c.getName().equals(categoryName))) {
              throw new IllegalArgumentException("이미 존재하는 카테고리입니다. name: " + categoryName);
          }

          Category parentCategory = null;
          if (parentCategoryName != null) {
              parentCategory = categories.stream()
                      .filter(c -> c.getName().equals(parentCategoryName))
                      .findFirst()
                      .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + parentCategoryName));
          }

          return parentCategory;
      }

- GET /categories 부모 카테고리가 있는 자식 카테고리들 중복으로 조회된다.  
  원인: categoryRepository.findAll()로 모든 카테고리 조회하고, 부모 카테고리들은 자식 카테고리까지 가져오는 문제 발생  
  해결: categoryRepository.findAll()로 모든 카테고리 조회하고, 부모 카테고리가 없는 최상위 카테고리만 반환한다.

      public List<Category> findCategories() {
          List<Category> categories = categoryRepository.findAll();
          return categories.stream()
                  .filter(c -> c.getParent() == null)
                  .collect(toList());
      }

- GET /categories/{categoryName} (조회하는 카테고리의 제일 안쪽의 자식 카테고리의 계층 수) - (조회하는 카테고리 계층 수) + 1번의 select 쿼리가 나온다.  
  원인: categoryRepository.findAll()에서는 모든 카테고리를 한 번에 조회해서 자식 카테고리를 한 번만 조회하면 되지만,  
  categoryRepository.findByCategoryName에서는 모든 카테고리를 조회하지 않아서 자식 카테고리를 모르기 때문에 자식 카테고리를 조회하고 자식 카테고리가 있는지 다시 조회한다.

      return categoryRepository.findByName(categoryName)
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));

  해결: categoryRepository.findAll()로 모든 카테고리를 한 번에 조회하고, 조회하는 카테고리를 꺼내서 자식 카테고리를 조회해서 2번의 select 쿼리로 줄였다.

      List<Category> categories = categoryRepository.findAll();

      return categories.stream()
              .filter(c -> c.getName().equals(categoryName))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName"));

- POST /categories/{categoryName} /members/{memberLoginId}/password와 비슷한 문제가 발생했다.  
  원인: 카테고리를 두 번 조회해서 생긴 문제

      CategoryController
      Category category = categoryService.findCategoryByName(categoryName);
      Category newParentCategory = categoryService.findCategoryByName(newParentCategoryName);
      categoryService.changeParentCategory(category, newParentCategory);

  해결: categoryRepository.findAll()로 모든 카테고리를 조회하고, 카테고리들을 꺼내서 수정을 해서 2번의 select 쿼리를 1번으로 줄였다.

      CategoryService
      List<Category> categories = categoryRepository.findAll();
      Category category = categories.stream()
              .filter(c -> c.getName().equals(categoryName))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));

      Category newParentCategory = categories.stream()
              .filter(c -> c.getName().equals(newParentCategoryName))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + newParentCategoryName));

      return category.changeParentCategory(newParentCategory);

      CategoryController
      Category category = categoryService.changeParentCategory(categoryName, newParentCategoryName);

- DELETE /categories/{categoryName} 2번의 카테고리 select, 1번의 카테고리 상품 select 쿼리가 나오고 카테고리 delete 쿼리가 나온다.  
  원인: 자식이 있는지, 카테고리에 등록된 상품이 있는지 검증해서 3번의 select 쿼리가 나온다.

      CategoryController
      Category category = categoryService.findCategoryByName(categoryName);
      categoryService.deleteCategory(category);

      CategoryService
      public void deleteCategory(Category category) {
          if (!category.getChild().isEmpty()) {
              throw new IllegalStateException("자식 카테고리가 있어서 삭제할 수 없습니다.");
          } else if (!category.getCategoryProducts().isEmpty()) {
              throw new IllegalStateException("카테고리에 상품이 있어서 삭제할 수 없습니다.");
          }

          if (category.getParent() != null) {
              category.unConnectParent();
          }

          categoryRepository.delete(category);
      }

  시도한 방법: findCategoryByName()으로 조회해서 카테고리의 select 쿼리 줄이기 시도 - 카테고리와 category.getChild()로 select 쿼리가 2번, category.getCategoryProducts로 select 쿼리가 1번 나와서 총 세 번의 select 쿼리와 한 번의 delete 쿼리가 나온다.

      CategoryController
      categoryService.deleteCategory(categoryName);

      CategoryService
      Category category = findCategoryByName(categoryName);

  해결: child를 left join fetch로 같이 조회 - 카테고리와 category.getChild()의 select 쿼리가 하나로 합쳐져서 총 두 번의 select 쿼리와 한 번의 delete 쿼리가 나온다.

      CategoryRepository
      public Optional<Category> findWithChild(String categoryName) {
          return em.createQuery("select c from Category c" +
                          " left join fetch c.child" +
                          " where c.name = :name", Category.class)
                  .setParameter("name", categoryName)
                  .getResultStream()
                  .findFirst();
      }

  categoryProducts도 같이 조회하면 org.hibernate.loader.MultipleBagFetchException 예외가 발생하고 발생하지 않았어도 카테고리에 상품들이 많을 경우  
  default_batch_fetch_size로 조회하면 첫 번째의 select에 한 개의 카테고리와 두 번째의 select에 카테고리 상품들이 조회되지만, Fetch Join으로 조회하면 한 번의 select로 카테고리 상품 수만큼의 카테고리를 중복해서 조회한다.

- POST /categories/{categoryName}/{productName}, /categories/{categoryName}/products는 ProductController 중복 쿼리, N+1 문제 해결 후 진행
