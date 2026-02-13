package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public String create(String categoryName, String parentCategoryName) {
        Category parentCategory = validateCategory(categoryName, parentCategoryName);
        Category category = Category.createCategory(parentCategory, categoryName);
        categoryRepository.save(category);
        return category.getName();
    }

    public Category findCategoryByName(String categoryName) {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));
    }

    public List<Category> findCategoryByNames(String... categoryNames) {
        List<Category> categories = categoryRepository.findAll();
        Set<String> uniqueNames = new HashSet<>(Arrays.asList(categoryNames));

        List<Category> categoryList = new ArrayList<>();
        for (String categoryName : uniqueNames) {
            categoryList.add(categories.stream()
                    .filter(c -> c.getName().equals(categoryName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName)));
        }

        return categoryList;
    }

    public List<Category> findCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getParent() == null)
                .collect(toList());
    }

    @Transactional
    public Category changeParentCategory(String categoryName, String newParentCategoryName) {
        List<Category> categories = categoryRepository.findAll();
        Category category = categories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));

        Category newParentCategory = categories.stream()
                .filter(c -> c.getName().equals(newParentCategoryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + newParentCategoryName));

        return category.changeParentCategory(newParentCategory);
    }

    @Transactional
    public void deleteCategory(String categoryName) {
        Category category = categoryRepository.findWithChild(categoryName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName));

        if (!category.getChild().isEmpty()) {
            throw new IllegalStateException("자식 카테고리가 있어서 삭제할 수 없습니다.");
        } else if (!category.getCategoryProducts().isEmpty()) {
            throw new IllegalStateException("카테고리에 상품이 있어서 삭제할 수 없습니다.");
        }

        if (category.getParent() != null) {
            category.unConnectParent();
        }

        categoryRepository.delete(category);
    }

    public CategoryResponse getCategoryDto(Category category) {
        return CategoryResponse.categoryChangeDto(category);
    }

    public CategoryDisconnectResponse getCategoryDisconnectResponse(Category category) {
        return CategoryDisconnectResponse.categoryChangeDisconnectResponse(category);
    }

    private Category validateCategory(String categoryName, String parentCategoryName) {
        List<Category> categories = categoryRepository.existsByCategoryName(categoryName, parentCategoryName);
        if (categories.stream().anyMatch(c -> c.getName().equals(categoryName))) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다. name: " + categoryName);
        }

        Category parentCategory = null;
        if (parentCategoryName != null) {
            parentCategory = categories.stream()
                    .filter(c -> c.getName().equals(parentCategoryName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + parentCategoryName));
        }

        return parentCategory;
    }

    protected void validateCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("카테고리가 존재하지 않습니다.");
        }

        Set<Long> categoriesIds = categories.stream()
                .map(c -> c.getId())
                .collect(toSet());

        Long findCategoryCount = categoryRepository.countCategories(categoriesIds);
        if (categoriesIds.size() != findCategoryCount) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 있습니다.");
        }
    }
}
