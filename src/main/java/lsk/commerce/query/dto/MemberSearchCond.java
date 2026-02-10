package lsk.commerce.query.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberSearchCond {

    private String name;
    private String loginId;

    public MemberSearchCond(String name, String loginId) {
        this.name = name;
        this.loginId = loginId;
    }
}
