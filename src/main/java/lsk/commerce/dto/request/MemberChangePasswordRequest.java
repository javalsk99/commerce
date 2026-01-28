package lsk.commerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberChangePasswordRequest {

    @NotNull
    private String password;

    public MemberChangePasswordRequest(String password) {
        this.password = password;
    }
}
