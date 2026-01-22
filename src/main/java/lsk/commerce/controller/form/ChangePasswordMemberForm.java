package lsk.commerce.controller.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChangePasswordMemberForm {

    @NotNull
    private String password;

    public ChangePasswordMemberForm(String password) {
        this.password = password;
    }
}
