package lsk.commerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class MemberChangePasswordRequest {

    @NotBlank @Size(min = 8, max = 20)
    private String password;

    public MemberChangePasswordRequest(String password) {
        this.password = password;
    }
}
