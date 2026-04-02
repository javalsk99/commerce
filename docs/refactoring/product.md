# Product 리팩토링
## ProductController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  String 타입의 가변 인자를 리스트로 변경했다.  
  빠졌던 Argument Resolver를 DTO인 request에 @RequestBody, 리스트인 categoryNames에 @RequestParam을 추가하고 Bean Validation을 추가했다.


- changeProduct  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.
  빠졌던 Argument Resolver인 @RequestBody와 DTO 검증인 @Valid를 추가했다.


- connectCategory  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


- findProduct, changeProduct, delete, connectCategory  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.


- changeProduct  
  일부 필드만 변경해서 update보다 change가 잘 어울려서 updateProduct에서 변경했다.


## ProductService
- register  
  유지보수의 효율을 위해 파라미터를 엔티티에서 DTO로 변경했다.


- validateAndGetProduct  
  검증하면서 DTO를 엔티티로 변환했다.


- deleteProduct  
  멱등성을 위해 조회 후 존재하지 않으면 이후의 로직을 실행하지 않고 끝낸다.


- findProduct, findProductWithCategoryProduct, changePriceAndStock, deleteProduct  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.


- changePriceAndStock  
  updateProduct에서 변경했다.


## Product
- changePriceAndStock  
  기존과 다를 때만 변경해서 여러 번 호출 시 UPDATE 쿼리가 한 번만 나오게 했다.


- productNumber 필드를 나노 아이디로 생성하면서 Order의 orderNumber와 겹치는 부분을 NanoIdProvider 클래스로 만들어 사용했다.


- changePriceAndStock, changeStock, stock  
  각각 updateProduct, updateStock, quantity에서 변경했다.


- removeCategoryProductsFormCategory  
  상품을 제거할 때 카테고리 상품에서 카테고리만 제거하고 상품을 제거하지 않음 (기존 todo에 있던 문제)  
  상품에서 카테고리 상품으로 cascade가 걸려있어서 카테고리 상품에서 상품을 제거하지 않아도 된다.


## ProductRepository
- findByNumber, findWithCategoryProduct, findWithCategoryProductCategory  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.


## ProductQueryRepository
- search
  검색 결과를 간략하게 변경했다.