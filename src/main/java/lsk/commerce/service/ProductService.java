package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.product.Product;
import lsk.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Long register(Product product) {
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

    public void deleteProduct(Product product) {
        productRepository.delete(product);
    }

    public void updateProduct(Long productId, String newName, int newPrice, int newStockQuantity) {
        Product product = productRepository.findOne(productId);
        product.updateProduct(newName, newPrice, newStockQuantity);
    }
}
