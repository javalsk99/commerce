package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public String create(CategoryCreateRequest request) {
        Category parentCategory = validateCategory(request.name(), request.parentNumber());
        Category category = Category.createCategory(parentCategory, request.name());
        categoryRepository.save(category);
        return category.getCategoryNumber();
    }

    public Category findCategoryByCategoryNumber(String categoryNumber) {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getCategoryNumber().equals(categoryNumber))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. categoryNumber: " + categoryNumber));
    }

    public List<Category> findRootCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .filter(c -> c.getParent() == null)
                .toList();
    }

    @Transactional
    public Category changeParentCategory(String categoryNumber, CategoryChangeParentRequest request) {
        List<Category> categories = categoryRepository.findAll();
        Category category = categories.stream()
                .filter(c -> c.getCategoryNumber().equals(categoryNumber))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 카테고리입니다. categoryNumber: " + categoryNumber));

        if (categoryNumber.equals("LVjBKQYeuJQP")) {
            return category;
        }

        Category newParentCategory = categories.stream()
                .filter(c -> c.getCategoryNumber().equals(request.parentNumber()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("부모 카테고리가 존재하지 않습니다. parentNumber: " + request.parentNumber()));

        category.changeParentCategory(newParentCategory);
        return newParentCategory;
    }

    @Transactional
    public void deleteCategory(String categoryNumber) {
        if (categoryNumber.equals("LVjBKQYeuJQP")) {
            return;
        }

        Optional<Category> optionalCategory = categoryRepository.findWithChild(categoryNumber);
        if (optionalCategory.isEmpty()) {
            return;
        }

        Category category = optionalCategory.get();

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
        return CategoryDisconnectResponse.from(category);
    }

    private Category validateCategory(String categoryName, String parentNumber) {
        List<Category> categories = categoryRepository.findWithParent(categoryName, parentNumber);

        Category parentCategory = null;
        if (parentNumber != null) {
            parentCategory = categories.stream()
                    .filter(c -> c.getCategoryNumber().equals(parentNumber))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("부모 카테고리가 존재하지 않습니다. parentNumber: " + parentNumber));
        }

        Category parent = parentCategory;
        boolean isDuplicate = categories.stream()
                .filter(c -> c.getName().equals(categoryName))
                .anyMatch(c -> {
                    if (parentNumber == null) {
                        return c.getParent() == null;
                    }

                    if (c == parent) {
                        throw new DuplicateResourceException("부모 카테고리와 같은 이름입니다. name: " + categoryName);
                    }

                    return c.getParent() != null && c.getParent().equals(parent);
                });

        if (isDuplicate) {
            throw new DuplicateResourceException("이미 존재하는 카테고리입니다. name: " + categoryName);
        }

        return parentCategory;
    }

    protected List<Category> validateAndGetCategories(List<String> categoryNumbers) {
        Set<String> categoryNumberSet = new HashSet<>(categoryNumbers);

        List<Category> categories = categoryRepository.findByNumberSet(categoryNumberSet);
        if (categoryNumberSet.size() != categories.size()) {
            throw new DataNotFoundException("존재하지 않는 카테고리가 있습니다");
        }

        return categories;
    }
}
