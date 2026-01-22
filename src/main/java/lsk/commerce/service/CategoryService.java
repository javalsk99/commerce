package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Long create(Category category) {
        categoryRepository.save(category);
        return category.getId();
    }

    public Category findCategory(Long categoryId) {
        return categoryRepository.findOne(categoryId);
    }

    public Category findCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    public List<Category> findCategories() {
        return categoryRepository.findAll();
    }

    public List<Product> findProductsByCategoryId(String categoryName) {
        return categoryRepository.findProductsByCategoryId(categoryName);
    }

    @Transactional
    public Category changeParentCategory(Category category, Category newParentCategory) {
        return category.changeParentCategory(newParentCategory);
    }

    @Transactional
    public void deleteCategory(Category category) {
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
}
