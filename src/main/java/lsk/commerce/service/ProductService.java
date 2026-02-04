package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.ProductWithCategoryResponse;
import lsk.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Long register(Product product, Category... categories) {
        product.addCategoryProduct(categories);
        productRepository.save(product);
        return product.getId();
    }

    public Long register(Product product, List<Category> categories) {
        product.addCategoryProduct(categories);
        productRepository.save(product);
        return product.getId();
    }

    @Transactional(readOnly = true)
    public Product findProduct(Long productId) {
        return productRepository.findOne(productId);
    }

    @Transactional(readOnly = true)
    public List<Product> findProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findProductByName(String productName) {
        return productRepository.findByName(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
    }

    @Transactional(readOnly = true)
    public Product findProductWithCategoryProduct(String productName) {
        return productRepository.findWithCategoryProduct(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
    }

    public void deleteProduct(String productName) {
        Product product = productRepository.findWithCategoryProductCategory(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
        if (!product.getCategoryProducts().isEmpty()) {
            product.removeCategoryProducts();
        }

        productRepository.delete(product);
    }

    public void updateProduct(Long productId, int newPrice, int newStockQuantity) {
        Product product = productRepository.findOne(productId);
        product.updateProduct(newPrice, newStockQuantity);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDto(Product product) {
        return ProductResponse.productChangeDto(product);
    }

    @Transactional(readOnly = true)
    public ProductWithCategoryResponse getProductWithCategoryDto(Product product) {
        return ProductWithCategoryResponse.productChangeResponse(product);
    }
}
