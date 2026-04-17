package lsk.commerce.query.dto;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record CategoryQueryDto(
        String categoryName,
        String categoryNumber,
        List<CategoryProductQueryDto> categoryProductQueryDtoList
) {
    public CategoryQueryDto(String categoryName, String categoryNumber) {
        this(categoryName, categoryNumber, new ArrayList<>());
    }
}
