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
import lsk.commerce.swagger.ApiOwnerError;
import lsk.commerce.swagger.ApiRoleError;
import lsk.commerce.swagger.ApiValidationMember;
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
                    "**이름**: 필수, 2자 이상 50자 이하 \n\n" +
                    "**아이디**: 필수, 중복 불가, 4자 이상 20자 이하 \n\n" +
                    "**비밀번호**: 필수, 8자 이상 20자 이하 \n\n" +
                    "**도시명**: 필수, 50자 이하 \n\n" +
                    "**거리명**: 필수, 50자 이하 \n\n" +
                    "**우편번호**: 필수, 10자 이하"
    )
    @ApiResponse(responseCode = "201")
    @ApiValidationMember
    @PostMapping("/members")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid MemberCreateRequest request) {
        String loginId = memberService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(loginId, 1));
    }

    @Operation(
            summary = "회원 검색",
            description = "**관리자**만 검색할 수 있습니다. \n\n" +
                    "검색 조건에 맞춰 조회합니다. \n\n" +
                    "원하지 않는 검색 조건은 비워주세요."
    )
    @ApiResponse(responseCode = "200")
    @ApiRoleError
    @GetMapping("/members")
    public ResponseEntity<Result<List<MemberResponse>>> memberList(@ParameterObject @ModelAttribute MemberSearchCond cond) {
        List<MemberResponse> memberResponseList = memberQueryService.searchMembers(cond);
        return ResponseEntity.ok(new Result<>(memberResponseList, memberResponseList.size()));
    }

    @Operation(
            summary = "회원 상세 조회",
            description = "**본인**만 조회할 수 있습니다. \n\n" +
                    "회원의 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiOwnerError
    @GetMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<MemberQueryDto>> findMember(
            @Parameter(example = "testId")
            @PathVariable("memberLoginId") String memberLoginId
    ) {
        MemberQueryDto memberQueryDto = memberQueryService.findMember(memberLoginId);
        return ResponseEntity.ok(new Result<>(memberQueryDto, 1));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "**본인**만 변경할 수 있습니다. \n\n" +
                    "**관리자 계정**은 변경되지 않고 성공합니다. \n\n" +
                    "**비밀번호**: 필수, 8자 이하, 기존과 달라야 합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiOwnerError
    @PostMapping("/members/{memberLoginId}/password")
    public ResponseEntity<Result<String>> changePassword(
            @Parameter(example = "testId")
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangePasswordRequest request
    ) {
        memberService.changePassword(memberLoginId, request);
        return ResponseEntity.ok(new Result<>("비밀번호가 변경되었습니다", 1));
    }

    @Operation(
            summary = "주소 변경",
            description = "**본인**만 변경할 수 있습니다. \n\n" +
                    "**도시명**: 필수, 50자 이하 \n\n" +
                    "**거리명**: 필수, 50자 이하 \n\n" +
                    "**우편번호**: 필수, 10자 이하"
    )
    @ApiResponse(responseCode = "200")
    @ApiOwnerError
    @PatchMapping("/members/{memberLoginId}/address")
    public ResponseEntity<Result<MemberResponse>> changeAddress(
            @Parameter(example = "test_id_001")
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangeAddressRequest request
    ) {
        Member member = memberService.changeAddress(memberLoginId, request);
        MemberResponse memberResponse = memberService.getMemberDto(member);
        return ResponseEntity.ok(new Result<>(memberResponse, 1));
    }

    @Operation(
            summary = "회원 삭제",
            description = "**본인**만 삭제할 수 있습니다. \n\n" +
                    "**관리자 계정**은 삭제되지 않고 성공합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiOwnerError
    @DeleteMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(example = "testId")
            @PathVariable("memberLoginId") String memberLoginId
    ) {
        memberService.deleteMember(memberLoginId);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }
}
