package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;
import lsk.commerce.repository.CategoryProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryProductService {

    private final CategoryProductRepository categoryProductRepository;
    private final CategoryService categoryService;
    private final ProductService productService;

    public List<CategoryProduct> findCategoryProductsWithProductByCategory(Category category) {
        return categoryProductRepository.findAllWithProductByCategory(category);
    }

    public Category disconnect(String categoryNumber, String productNumber) {
        Category category = categoryService.findCategoryByCategoryNumber(categoryNumber);
        Product product = productService.findProductWithCategoryProduct(productNumber);
        if (category.getCategoryProducts().stream().noneMatch(cp -> cp.getProduct().equals(product))) {
            return category;
        }

        CategoryProduct categoryProduct = product.removeCategoryProduct(category);
        categoryProductRepository.delete(categoryProduct);
        return category;
    }

    public Category disconnectAll(String categoryNumber) {
        Category category = categoryService.findCategoryByCategoryNumber(categoryNumber);
        List<CategoryProduct> categoryProducts = new ArrayList<>(findCategoryProductsWithProductByCategory(category));
        if (categoryProducts.isEmpty()) {
            return category;
        }

        for (CategoryProduct categoryProduct : categoryProducts) {
            categoryProduct.getProduct().removeCategoryProduct(category);
            categoryProductRepository.delete(categoryProduct);
        }

        return category;
    }

    public Product connect(String productNumber, String categoryNumber) {
        Product product = productService.findProductWithCategoryProduct(productNumber);
        Category category = categoryService.findCategoryByCategoryNumber(categoryNumber);

        if (product.getCategoryProducts().stream().anyMatch(c -> category.equals(c.getCategory()))) {
            return product;
        }

        product.connectCategory(category);
        return product;
    }
}
