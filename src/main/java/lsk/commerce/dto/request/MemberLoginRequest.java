package lsk.commerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberLoginRequest {

    @NotNull
    private String loginId;

    @NotNull
    private String password;

    public MemberLoginRequest(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }
}
