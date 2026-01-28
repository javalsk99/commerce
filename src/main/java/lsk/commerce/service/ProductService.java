package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryProductService categoryProductService;

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
    public Product findProductByName(String name) {
        return productRepository.findByName(name);
    }

    public void deleteProduct(Product product) {
        for (CategoryProduct removeCategoryProduct : product.removeCategoryProducts()) {
            categoryProductService.disConnect(removeCategoryProduct);
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
}
