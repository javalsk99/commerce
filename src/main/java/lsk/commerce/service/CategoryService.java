package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Long create(Category category) {
        categoryRepository.save(category);
        return category.getId();
    }

    @Transactional(readOnly = true)
    public Category findCategory(Long categoryId) {
        return categoryRepository.findOne(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByCategoryId(Long categoryId) {
        return categoryRepository.findProductsByCategoryId(categoryId);
    }

    public Category changeParentCategory(Category category, Category newParentCategory) {
        return category.changeParentCategory(newParentCategory);
    }

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
