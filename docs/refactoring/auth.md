# Auth 리팩토링
## AuthController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.
  String을 Result 응답용 DTO에 감싸서 반환했다.


- login: Argument Resolver가 빠져있었다.  
  POST 요청이므로 @RequestBody를 추가했다.


- webLogin: Argument Resolver가 빠져있었다.  
  GET 요청이므로 @ModelAttribute를 추가했다.