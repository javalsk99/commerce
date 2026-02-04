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

- GET /categories/{categoryName} (조회하는 카테고리의 제일 안쪽의 자식 카테고리의 계층 수) - (조회하는 카테고리 계층 수) + 1만큼의 select 쿼리가 나온다.  
  원인: categoryRepository.findAll()에서는 모든 카테고리를 한 번에 조회해서 자식 카테고리를 한 번만 조회하면 되지만,  
  categoryRepository.findByCategoryName에서는 모든 카테고리를 조회하지 않아서 자식 카테고리를 모르기 때문에 자식 카테고리를 조회하고 자식 카테고리가 있는지 다시 조회한다.

      return categoryRepository.findByName(categoryName)
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));

  해결: categoryRepository.findAll()로 모든 카테고리를 한 번에 조회하고, 조회하는 카테고리를 꺼내서 자식 카테고리를 조회해서 두 번의 select 쿼리로 줄였다.

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

  해결: categoryRepository.findAll()로 모든 카테고리를 조회하고, 카테고리들을 꺼내서 수정을 해서 두 번의 select 쿼리를 한 번으로 줄였다.

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

- DELETE /categories/{categoryName} 두 번의 카테고리 select, 한 번의 카테고리 상품 select 쿼리가 나오고 카테고리 delete 쿼리가 나온다.  
  원인: 자식이 있는지, 카테고리에 등록된 상품이 있는지 검증해서 세 번의 select 쿼리가 나온다.

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

  시도한 방법: findCategoryByName()으로 조회해서 카테고리의 select 쿼리 줄이기 시도 - 카테고리와 category.getChild()로 select 쿼리가 2번, category.getCategoryProducts로 select 쿼리가 한 번 나와서 총 세 번의 select 쿼리와 한 번의 delete 쿼리가 나온다.

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


- POST /categories/{categoryName}/{productName} 카테고리에서 한 번, 상품에서 한 번, 카테고리 상품에서 두 번의 select 쿼리가 나온다.  
  원인: 카테고리와 상품을 조회하고 카테고리에서 카테고리 상품을 접근하고, 카테고리 상품에서 상품을 접근해서 생긴 문제  
  해결: 카테고리를 조회하고 카테고리 상품을 접근하고, 상품에 카테고리 상품을 left join fetch 해서 select 쿼리를 세 번으로 줄였다.  
  이유: 본 프로젝트에서의 카테고리에 들어가는 상품 수가 적지만 커머스 특성 상 대부분의 경우 카테고리에 들어가는 상품의 수는 많으므로 카테고리에서 카테고리 상품을 접근할 때 Fetch Join 하지 않는다.


- POST /categories/{categoryName}/products 카테고리에서 select 쿼리가 한 번, 카테고리 상품에서 select 쿼리가 한 번, 상품에서 select 쿼리가 한 번, 카테고리에서 select 쿼리가 상품 수만큼, 상품에 left join fetch한 카테고리 상품의 select 쿼리가 상품 수만큼으로 총 2N+3 번의 select 쿼리가 나온다.  
  원인: 컨트롤러에서 조회하고, for each문으로 서비스에서 다시 조회해서 생긴 문제

      CategoryController
      Category category = categoryService.findCategoryByName(categoryName);
          for (CategoryProduct categoryProduct : new ArrayList<>(category.getCategoryProducts())) {
              categoryProductService.disconnect(categoryName, categoryProduct.getProduct().getName());
          }

  시도한 방법: 컨트롤러에서 서비스로 categoryName을 넘겨 서비스에서 조회하고 삭제 - POST /categories/{categoryName}/{productName}의 문제처럼 select 쿼리를 네 번으로 줄였다.

      CategoryProductService
      public Category disconnectAll(String categoryName) {
          Category category = categoryService.findCategoryByName(categoryName);
          for (CategoryProduct categoryProduct : new ArrayList<>(category.getCategoryProducts())) {
              categoryProduct.getProduct().removeCategoryProduct(category);
              categoryProductRepository.delete(categoryProduct);
          }

          return category;
      }

  유지: POST /categories/{categoryName}/{productName}은 상품이 한 개여서 Fetch Join으로 카테고리 상품을 가져오면 이점이 있지만,  
  POST /categories/{categoryName}/products는 상품 전체를 조회하기 때문에 Fetch Join으로 카테고리 상품을 가져오면 쿼리가 줄어들지만 데이터가 과도하게 늘어날 수 있다.

## 로직
- 카테고리에 등록된 상품이 많으면 카테고리에서 카테고리 상품을 for each문으로 반복하면 성능에 좋지 않다.

      public CategoryProduct removeCategoryProduct(Category category) {
          for (CategoryProduct categoryProduct : category.getCategoryProducts()) {
              if (categoryProduct.getProduct() == null) {
                  throw new IllegalArgumentException("카테고리에 상품이 없습니다.");
              }

              if (this.getId().equals(categoryProduct.getProduct().getId())) {
                  this.categoryProducts.remove(categoryProduct);
                  category.getCategoryProducts().remove(categoryProduct);
                  return categoryProduct;
              }
          }

          throw new IllegalArgumentException("해당 상품의 카테고리가 아닙니다.");
      }

  해결: 상품은 많아도 들어가는 카테고리 수가 적으므로 상품에서 카테고리 상품을 for each문으로 반복한다.

      Product
      public CategoryProduct removeCategoryProduct(Category category) {
          for (CategoryProduct categoryProduct : categoryProducts) {
              if (categoryProduct.getCategory() == null) {
                  throw new IllegalArgumentException("상품에 카테고리가 제대로 들어가지 않았습니다.");
              }

              if (category.getId().equals(categoryProduct.getCategory().getId())) {
                  this.categoryProducts.remove(categoryProduct);
                  category.getCategoryProducts().remove(categoryProduct);
                  return categoryProduct;
              }
          }

          throw new IllegalArgumentException("카테고리에 상품이 없습니다.");
      }
