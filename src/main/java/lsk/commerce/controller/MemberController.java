package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.service.MemberService;
import lsk.commerce.swagger.ApiAdminForbiddenResponse;
import lsk.commerce.swagger.ApiMemberOwnerForbiddenResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "02. 회원", description = "회원 아이디는 대소문자를 구분합니다.")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @Operation(
            summary = "회원 가입",
            description = "회원을 생성합니다. \n\n" +
                    "**이름**: (필수) 한글, 영문, 숫자, _만 사용하여 2~50자 사이로 입력해 주세요. \n\n" +
                    "**아이디**: (필수, 중복 불가) 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요. \n\n" +
                    "**비밀번호**: (필수) 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요. \n\n" +
                    "**우편번호**: (필수) 숫자 5자로 입력해 주세요. \n\n" +
                    "**기본 주소**: (필수) 한글, 영문, 숫자, -, 공백만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**상세 주소**: (필수) 한글, 영문, 숫자, 특수문자(().,-), 공백만 사용하여 1~100자 사이로 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "비밀번호 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}]}"),
                                    @ExampleObject(name = "비밀번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "비밀번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "아이디 빈 문자열, 우편번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"우편번호는 숫자 5자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "이름 패턴 불일치, 비밀번호 빈 문자열, 기본 주소 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"name\", \"message\": \"이름은 한글, 영문, 숫자, _만 사용하여 2~50자 사이로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"baseAddress\", \"message\": \"기본 주소는 필수입니다\"}]}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "아이디 중복", value = "{\"code\": \"DUPLICATE_RESOURCE\", \"message\": \"이미 사용 중인 아이디입니다. loginId: test_id_001\", \"errors\": null}")
                    )
            )
    })
    @PostMapping("/members")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid MemberCreateRequest request) {
        String loginId = memberService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(loginId, 1));
    }

    @Operation(
            summary = "회원 검색",
            description = "**관리자**만 검색할 수 있습니다. \n\n" +
                    "검색 조건에 맞춰 조회합니다. \n\n" +
                    "원하지 않는 검색 조건은 비워주세요. \n\n" +
                    "초성과 한글이 섞이면 검색 결과가 나오지 않습니다. \n\n" +
                    "ex) ㅇ저 (X), ㅇㅈa (O)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "이름 패턴 불일치 (빈 문자열, 공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"name\", \"message\": \"이름은 한글, 초성, 영문, 숫자, _만 사용하여 1~50자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "아이디 패턴 불일치 (빈 문자열, 공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 1~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "이름, 아이디 패턴 불일치 (빈 문자열, 공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"name\", \"message\": \"이름은 한글, 초성, 영문, 숫자, _만 사용하여 1~50자 사이로 입력해 주세요\"}, {\"location\": \"FORM\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 1~20자 사이로 입력해 주세요\"}]}")
                            }
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @GetMapping("/members")
    public ResponseEntity<Result<List<MemberResponse>>> memberList(@ParameterObject @ModelAttribute @Valid MemberSearchCond cond) {
        List<MemberResponse> memberResponseList = memberQueryService.searchMembers(cond);
        return ResponseEntity.ok(new Result<>(memberResponseList, memberResponseList.size()));
    }

    @Operation(
            summary = "회원 상세 조회",
            description = "**본인**만 조회할 수 있습니다. \n\n" +
                    "회원의 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiMemberOwnerForbiddenResponse
    @GetMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<MemberQueryDto>> findMember(
            @Parameter(example = "testId")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
            @PathVariable("memberLoginId") String memberLoginId
    ) {
        MemberQueryDto memberQueryDto = memberQueryService.findMember(memberLoginId);
        return ResponseEntity.ok(new Result<>(memberQueryDto, 1));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "**본인**만 변경할 수 있습니다. \n\n" +
                    "**관리자 계정**은 변경되지 않고 성공합니다. \n\n" +
                    "**비밀번호**: (필수) 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 기존 비밀번호와 다르게 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "비밀번호 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}]}"),
                                    @ExampleObject(name = "비밀번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "비밀번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"password\", \"message\": \"영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "비밀번호 일치", value = "{\"code\": \"BAD_ARGUMENT\", \"message\": \"비밀번호가 기존과 달라야 합니다\", \"errors\": null}")

                            }
                    )
            )
    })
    @ApiMemberOwnerForbiddenResponse
    @PostMapping("/members/{memberLoginId}/password")
    public ResponseEntity<Result<String>> changePassword(
            @Parameter(example = "testId")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangePasswordRequest request
    ) {
        memberService.changePassword(memberLoginId, request);
        return ResponseEntity.ok(new Result<>("비밀번호가 변경되었습니다", 1));
    }

    @Operation(
            summary = "주소 변경",
            description = "**본인**만 변경할 수 있습니다. \n\n" +
                    "**우편번호**: (필수) 숫자 5자로 입력해 주세요. \n\n" +
                    "**기본 주소**: (필수) 한글, 영문, 숫자, -, 공백만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**상세 주소**: (필수) 한글, 영문, 숫자, 특수문자(().,-), 공백만 사용하여 1~100자 사이로 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "우편번호 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"우편번호는 필수입니다\"}]}"),
                                    @ExampleObject(name = "우편번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"우편번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"우편번호는 숫자 5자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "우편번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"우편번호는 숫자 5자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "우편번호, 기본 주소, 상세 주소 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"zipcode\", \"message\": \"공백일 수 없습니다\"}, {\"location\": \"BODY\", \"field\": \"baseAddress\", \"message\": \"공백일 수 없습니다\"}, {\"location\": \"BODY\", \"field\": \"detailAddress\", \"message\": \"공백일 수 없습니다\"}]}")
                            }
                    )
            )
    })
    @ApiMemberOwnerForbiddenResponse
    @PatchMapping("/members/{memberLoginId}/address")
    public ResponseEntity<Result<MemberResponse>> changeAddress(
            @Parameter(example = "testId")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangeAddressRequest request
    ) {
        Member member = memberService.changeAddress(memberLoginId, request);
        MemberResponse memberResponse = memberService.getMemberResponse(member);
        return ResponseEntity.ok(new Result<>(memberResponse, 1));
    }

    @Operation(
            summary = "회원 삭제",
            description = "**본인**만 삭제할 수 있습니다. \n\n" +
                    "**관리자 계정**은 삭제되지 않고 성공합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiMemberOwnerForbiddenResponse
    @DeleteMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(example = "testId")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
            @PathVariable("memberLoginId") String memberLoginId
    ) {
        memberService.deleteMember(memberLoginId);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }
}
