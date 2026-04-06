# Member 리팩토링
## MemberController
- 전체 메서드에 ResponseEntity로 상태 코드를 직접 설정했다.


- create  
  생성이므로 201 CREATED 코드로 설정하고, String을 Result 응답용 DTO에 넣어서 반환했다.


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
  비밀번호를 응답으로 보내면 보안에 문제가 생기므로 비밀번호 변경 완료 메시지를 Result 응답용 DTO에 넣어서 응답으로 반환했다.  
  빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.  
  보안 때문에 멱등성을 사용하지 않고 POST로 유지했다.


- changeAddress  
  MemberResponse를 Result 응답용 DTO에 넣어서 반환하고, 빠져있던 Argument Resolver를 POST 요청이므로 @RequestBody로 추가했다.  
  POST에서 멱등성에 더 적합한 PATCH로 변경했다.


## MemberQueryService
- findMember  
  Member가 없을 때, 404 에러를 반환하기 위해 커스텀 예외를 만들어 404 에러를 반환했다.


## MemberService
- changePassword, changeAddress  
  유지보수 효율을 위해 파라미터를 DTO의 필드에서 DTO로 변경했다.


- deleteMember  
  멱등성을 위해 조회 후 존재하면 삭제로 변경했다.


- adminJoin  
  보안을 위해 사용하지 않는 관리자 가입을 제거하고, DB에서 직접 변경하는 방식으로 관리자를 설정한다.


## Member
- changeAddress  
  멱등성을 위해 주소가 기존과 같으면 이후 로직을 실행하지 않고 반환했다.


- loginId, MemberCreateRequest loginId  
  @Pattern(regexp = "^[A-Za-z0-9]+$")를 추가해서 특수문자로 가입하는 것을 막았다.


- role  
  회원의 등급에 더 적합해서 grade에서 변경했다.


## MemberQueryRepository
- search  
  검색으로는 간략한 정보만 나오게 하기 위해 반환을 List<MemberResponse>로 변경했다