# Category 리팩토링
## CategoryController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  생성이므로 201 CREATED 코드로 설정하고 String을 Result 응답용 DTO에 넣어서 반환했다.
  빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.


- categoryList  
  기존 CategoryList를 DTO로 변환하는 로직을 Stream으로 변경했다.


- changeParentCategory  
  파라미터로 받는 필드를 DTO에 담아서 검증을 추가했다.
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


- disconnectProduct, disconnectProducts  
  중간 엔티티인 카테고리 상품을 삭제하므로 POST에서 DELETE로 변경했다.


- disconnectProduct  
  productName이 유니크 키가 아니어서 나노 아이디를 사용하는 productNumber로 변경했다.


- findCategory  
  상품과 연결 해제하기 전에 상품과 연결되어 있는지 확인하기 위해 반환을 상품 목록으로 변경했다.


## CategoryService
- 카테고리를 찾지 못해서 발생하는 예외에 커스텀 예외를 적용시켜 404 에러가 발생하게 했다.


- create  
  유지보수 효율을 위해 파라미터를 DTO의 필드에서 DTO로 변경했다.


- changeParentCategory  
  부모 카테고리를 변경한 것을 응답으로 확인하기 위해 부모 카테고리를 반환하게 변경했다.


- deleteCategory  
  멱등성을 위해 조회 후 존재하지 않으면 이후의 로직을 실행하지 않고 끝낸다.


- validateAndGetCategories  
  ProductController의 create에서 리스트를 @Size(min = 1)으로 검증해서 null과 empty 체크를 제거했다.