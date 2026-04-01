package lsk.commerce.query;

import lombok.RequiredArgsConstructor;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.dto.CategoryProductQueryDto;
import lsk.commerce.query.dto.CategoryQueryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryQueryRepository categoryQueryRepository;
    private final CategoryProductQueryRepository categoryProductQueryRepository;

    public CategoryQueryDto findCategory(String categoryName) {
        CategoryQueryDto categoryQueryDto = categoryQueryRepository.findCategory(categoryName)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + categoryName));

        List<CategoryProductQueryDto> categoryProductQueryDtoList = categoryProductQueryRepository.findCategoryProductsByCategoryName(categoryName);

        return categoryQueryDto.toBuilder()
                .categoryProductQueryDtoList(categoryProductQueryDtoList)
                .build();
    }
}
