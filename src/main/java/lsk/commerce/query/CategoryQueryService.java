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

    public CategoryQueryDto findCategory(String categoryNumber) {
        CategoryQueryDto categoryQueryDto = categoryQueryRepository.findCategory(categoryNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. categoryNumber: " + categoryNumber));

        List<CategoryProductQueryDto> categoryProductQueryDtoList = categoryProductQueryRepository.findCategoryProductsByCategoryName(categoryNumber);

        return categoryQueryDto.toBuilder()
                .categoryProductQueryDtoList(categoryProductQueryDtoList)
                .build();
    }
}
