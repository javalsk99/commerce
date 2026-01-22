package lsk.commerce.controller.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lsk.commerce.domain.Category;

@Getter
public class CategoryForm {

    @NotNull
    private String name;

    private String parentName;

    public CategoryForm(String name, String parentName) {
        this.name = name;
        this.parentName = parentName;
    }

    public static CategoryForm categoryChangeForm(Category category) {
        if (category.getParent() == null) {
            return new CategoryForm(category.getName(), null);
        } else {
            return new CategoryForm(category.getName(), category.getParent().getName());
        }
    }
}
