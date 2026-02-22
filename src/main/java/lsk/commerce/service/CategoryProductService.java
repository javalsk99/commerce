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

    public Category disconnect(String categoryName, String productName) {
        Category category = categoryService.findCategoryByName(categoryName);
        Product product = productService.findProductWithCategoryProduct(productName);
        CategoryProduct categoryProduct = product.removeCategoryProduct(category);
        categoryProductRepository.delete(categoryProduct);
        return category;
    }

    public Category disconnectAll(String categoryName) {
        Category category = categoryService.findCategoryByName(categoryName);
        List<CategoryProduct> categoryProducts = new ArrayList<>(findCategoryProductsWithProductByCategory(category));
        if (categoryProducts.isEmpty()) {
            throw new IllegalArgumentException("카테고리에 상품이 없습니다.");
        }

        for (CategoryProduct categoryProduct : categoryProducts) {
            categoryProduct.getProduct().removeCategoryProduct(category);
            categoryProductRepository.delete(categoryProduct);
        }

        return category;
    }

    public Product connect(String productName, String categoryName) {
        Product product = productService.findProductWithCategoryProduct(productName);
        Category category = categoryService.findCategoryByName(categoryName);

        if (product.getCategoryProducts().stream().anyMatch(c -> category.equals(c.getCategory()))) {
            throw new IllegalArgumentException("이미 상품이 해당 카테고리에 연결되어 있습니다.");
        }

        product.connectCategory(category);
        return product;
    }
}
