package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 인증", description = "관리자 계정 testId, abAB12!@")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호를 검증하고 토큰을 담은 쿠키를 생성합니다. \n\n" +
                    "로그인에서 회원 아이디는 대소문자를 구분하지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "아이디 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}]}"),
                                    @ExampleObject(name = "아이디 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "아이디 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "아이디 null, 비밀번호 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}]}"),
                                    @ExampleObject(name = "아이디 빈 문자열, 비밀번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"loginId\", \"message\": \"아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "로그인 실패", value = "{\"code\": \"BAD_ARGUMENT\", \"message\": \"아이디 또는 비밀번호가 틀렸습니다\", \"errors\": null}")
                            }
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<Result<String>> login(
            @RequestBody @Valid MemberLoginRequest loginRequest,
            HttpServletResponse response
    ) {
        String token = authService.login(loginRequest.loginId(), loginRequest.password());

        Cookie cookie = new Cookie("jjwt", token);
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("login", 1));
    }

    @Operation(summary = "로그아웃", description = "쿠키를 제거합니다.")
    @ApiResponse(responseCode = "200")
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jjwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new Result<>("logout", 1));
    }
}
