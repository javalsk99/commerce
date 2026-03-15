# Member 리팩토링
## MemberController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  생성이므로 201 CREATED 코드로 설정


- memberList  
  List를 Result 응답용 DTO에 넣어서 반환했다.  
  확장하기에 좋고, 데이터가 깨지지 않는다.

      public record Result<T>(
              T data,
              int count
      ) {
      }


- findMember  
  MemberQueryDto를 Result 응답용 DTO에 넣어서 반환했다.


- changePassword  
  비밀번호를 응답으로 보내면 보안에 문제가 생기므로 비밀번호 변경 완료 메시지를 응답으로 반환하고, 빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.


- changeAddress  
  MemberResponse를 Result 응답용 DTO에 넣어서 반환하고, 빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.


## MemberQueryService
- findMember  
  Member가 없을 때 404 에러를 반환하기 위해 커스텀 예외를 만들어 404 에러를 반환한다.


## MemberService
- changePassword, changeAddress
  유지보수 효율을 위해 파라미터를 DTO의 필드에서 DTO로 변경했다.