package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CategoryRequest {

    @NotNull
    private String name;

    private String parentName;

    public CategoryRequest(String name, String parentName) {
        this.name = name;
        this.parentName = parentName;
    }
}
