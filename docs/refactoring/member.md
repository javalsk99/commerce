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
