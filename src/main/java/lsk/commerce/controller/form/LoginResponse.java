package lsk.commerce.controller.form;

import lombok.Getter;
import lsk.commerce.domain.Grade;

@Getter
public class LoginResponse {

    private String loginId;

    private Grade grade;

    public LoginResponse(String loginId, Grade grade) {
        this.loginId = loginId;
        this.grade = grade;
    }
}
