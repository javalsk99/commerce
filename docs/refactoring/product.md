# Product 리팩토링
## ProductController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  String 타입의 가변 인자를 리스트로 변경했다.  
  빠졌던 Argument Resolver를 DTO인 request에 @RequestBody, 리스트인 categoryNames에 @RequestParam을 추가하고 Bean Validation을 추가했다.


## ProductService
- register  
  유지보수의 효율을 위해 파라미터를 엔티티에서 DTO로 변경했다.


- validateAndGetProduct  
  검증하면서 DTO를 엔티티로 변환했다.