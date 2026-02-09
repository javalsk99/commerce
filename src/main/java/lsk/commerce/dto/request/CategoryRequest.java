package lsk.commerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CategoryRequest {

    @NotBlank @Size(max = 20)
    private String name;

    private String parentName;

    public CategoryRequest(String name, String parentName) {
        this.name = name;
        this.parentName = parentName;
    }
}
