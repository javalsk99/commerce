# Category 리팩토링
## CategoryController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  생성이므로 201 CREATED 코드로 설정하고, 빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.


- categoryList  
  기존 CategoryList를 DTO로 변환하는 로직을 Stream으로 변경했다.


- changeParentCategory
  파라미터로 받는 필드를 DTO에 담아서 검증을 추가했다.

## CategoryService
- 카테고리를 찾지 못해서 발생하는 예외에 커스텀 예외를 적용시켜 404 에러가 발생하게 했다.


- create  
  유지보수 효율을 위해 파라미터를 DTO의 필드에서 DTO로 변경했다.


- changeParentCategory
  부모 카테고리를 변경한 것을 응답으로 확인하기 위해 부모 카테고리를 반환하게 변경했다.