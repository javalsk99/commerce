package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MemberLoginRequest {

    @NotBlank @Size(min = 4, max = 20)
    private String loginId;

    @NotBlank @Size(min = 8, max = 20)
    private String password;

    public MemberLoginRequest(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }
}
