package lsk.commerce.query.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryQueryDto {

    private String name;
    private List<CategoryQueryDto> child = new ArrayList<>();

    public CategoryQueryDto(String name) {
        this.name = name;
    }
}
