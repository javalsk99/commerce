package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.ProductWithCategoryResponse;
import lsk.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Long register(Product product, List<Category> categories) {
        validateProduct(product);
        product.addCategoryProduct(categories);
        productRepository.save(product);
        return product.getId();
    }

    public Product findProduct(Long productId) {
        return productRepository.findOne(productId);
    }

    public List<Product> findProducts() {
        return productRepository.findAll();
    }

    public Product findProductByName(String productName) {
        return productRepository.findByName(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
    }

    public Product findProductWithCategoryProduct(String productName) {
        return productRepository.findWithCategoryProduct(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
    }

    @Transactional
    public void deleteProduct(String productName) {
        Product product = productRepository.findWithCategoryProductCategory(productName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName));
        if (!product.getCategoryProducts().isEmpty()) {
            product.removeCategoryProducts();
        }

        productRepository.delete(product);
    }

    @Transactional
    public void updateProduct(Long productId, int newPrice, int newStockQuantity) {
        Product product = productRepository.findOne(productId);
        product.updateProduct(newPrice, newStockQuantity);
    }

    public ProductResponse getProductDto(Product product) {
        return ProductResponse.productChangeDto(product);
    }

    public ProductWithCategoryResponse getProductWithCategoryDto(Product product) {
        return ProductWithCategoryResponse.productChangeResponse(product);
    }

    public boolean validateProduct(Product product) {
        if (product instanceof Album a) {
            return productRepository.existsAlbum(a.getName(), a.getArtist(), a.getStudio());
        } else if (product instanceof Book b) {
            return productRepository.existsBook(b.getName(), b.getAuthor(), b.getIsbn());
        } else if (product instanceof Movie m) {
            return productRepository.existsMovie(m.getName(), m.getActor(), m.getDirector());
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다.");
        }
    }
}
