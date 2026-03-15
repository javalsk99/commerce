package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public String create(CategoryRequest request) {
        Category parentCategory = validateCategory(request.name(), request.parentName());
        Category category = Category.createCategory(parentCategory, request.name());
        categoryRepository.save(category);
        return category.getName();
    }

    public Category findCategoryByName(String categoryName) {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + categoryName));
    }

    public List<Category> findCategoryByNames(String... categoryNames) {
        List<Category> categories = categoryRepository.findAll();
        Set<String> uniqueNames = new HashSet<>(Arrays.asList(categoryNames));

        List<Category> categoryList = new ArrayList<>();
        for (String categoryName : uniqueNames) {
            categoryList.add(categories.stream()
                    .filter(c -> c.getName().equals(categoryName))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + categoryName)));
        }

        return categoryList;
    }

    public List<Category> findCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getParent() == null)
                .toList();
    }

    @Transactional
    public Category changeParentCategory(String categoryName, CategoryChangeParentRequest request) {
        List<Category> categories = categoryRepository.findAll();
        Category category = categories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + categoryName));

        Category newParentCategory = categories.stream()
                .filter(c -> c.getName().equals(request.name()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + request.name()));

        category.changeParentCategory(newParentCategory);
        return newParentCategory;
    }

    @Transactional
    public void deleteCategory(String categoryName) {
        Category category = categoryRepository.findWithChild(categoryName)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + categoryName));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("자식 카테고리가 있어서 삭제할 수 없습니다");
        } else if (!category.getCategoryProducts().isEmpty()) {
            throw new IllegalStateException("카테고리에 상품이 있어서 삭제할 수 없습니다");
        }

        if (category.getParent() != null) {
            category.unConnectParent();
        }

        categoryRepository.delete(category);
    }

    public CategoryResponse getCategoryDto(Category category) {
        return CategoryResponse.from(category);
    }

    public CategoryDisconnectResponse getCategoryDisconnectResponse(Category category) {
        return CategoryDisconnectResponse.categoryChangeDisconnectResponse(category);
    }

    private Category validateCategory(String categoryName, String parentCategoryName) {
        List<Category> categories = categoryRepository.existsByCategoryNames(categoryName, parentCategoryName);
        if (categories.stream().anyMatch(c -> c.getName().equals(categoryName))) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다. name: " + categoryName);
        }

        Category parentCategory = null;
        if (parentCategoryName != null) {
            parentCategory = categories.stream()
                    .filter(c -> c.getName().equals(parentCategoryName))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + parentCategoryName));
        }

        return parentCategory;
    }

    protected List<Category> validateAndGetCategories(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            throw new DataNotFoundException("카테고리가 존재하지 않습니다");
        }

        Set<String> categoryNameSet = new HashSet<>(categoryNames);

        List<Category> categories = categoryRepository.findByNameSet(categoryNameSet);
        if (categoryNameSet.size() != categories.size()) {
            throw new DataNotFoundException("존재하지 않는 카테고리가 있습니다");
        }

        return categories;
    }
}
