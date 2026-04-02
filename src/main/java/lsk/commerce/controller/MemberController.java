package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberChangeAddressRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.MemberQueryService;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import lsk.commerce.service.MemberService;
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

@Tag(name = "02. 회원", description = "가입, 검색, 수정, 삭제")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberQueryService memberQueryService;

    @Operation(
            summary = "회원 가입",
            description = "회원을 생성합니다. \n\n" +
                    "**아이디**는 중복될 수 없습니다."
    )
    @PostMapping("/members")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid MemberCreateRequest request) {
        String loginId = memberService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(loginId, 1));
    }

    @Operation(summary = "회원 검색", description = "**관리자**만 검색할 수 있습니다. \n\n")
    @GetMapping("/members")
    public ResponseEntity<Result<List<MemberQueryDto>>> memberList(@ParameterObject @ModelAttribute MemberSearchCond cond) {
        List<MemberQueryDto> memberQueryDtoList = memberQueryService.searchMembers(cond);
        return ResponseEntity.ok(new Result<>(memberQueryDtoList, memberQueryDtoList.size()));
    }

    @Operation(summary = "회원 상세 조회", description = "**본인**만 조회할 수 있습니다.")
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
                    "**관리자 계정**은 변경할 수 없습니다."
    )
    @PostMapping("/members/{memberLoginId}/password")
    public ResponseEntity<Result<String>> changePassword(
            @Parameter(example = "test_id_001")
            @PathVariable("memberLoginId") String memberLoginId,
            @RequestBody @Valid MemberChangePasswordRequest request
    ) {
        memberService.changePassword(memberLoginId, request);
        return ResponseEntity.ok(new Result<>("비밀번호가 변경되었습니다", 1));
    }

    @Operation(summary = "주소 변경", description = "**본인**만 변경할 수 있습니다.")
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
                    "**관리자 계정**은 삭제할 수 없습니다."
    )
    @DeleteMapping("/members/{memberLoginId}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(example = "test_id_001")
            @PathVariable("memberLoginId") String memberLoginId
    ) {
        memberService.deleteMember(memberLoginId);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }
}
