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
    private final CategoryService categoryService;

    @Transactional
    public String register(Product product, List<Category> categories) {
        validateProduct(product);
        categoryService.validateCategories(categories);

        product.addCategoryProduct(categories);
        productRepository.save(product);
        return product.getName();
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
    public Product updateProduct(String productName, int newPrice, int newStockQuantity) {
        Product product = findProductByName(productName);
        product.updateProduct(newPrice, newStockQuantity);
        return product;
    }

    public ProductResponse getProductDto(Product product) {
        return ProductResponse.productChangeDto(product);
    }

    public ProductWithCategoryResponse getProductWithCategoryDto(Product product) {
        return ProductWithCategoryResponse.productChangeResponse(product);
    }

    public void validateProduct(Product product) {
        boolean result;
        if (product instanceof Album a) {
            result = productRepository.existsAlbum(a.getName(), a.getArtist(), a.getStudio());
        } else if (product instanceof Book b) {
            result = productRepository.existsBook(b.getName(), b.getAuthor(), b.getIsbn());
        } else if (product instanceof Movie m) {
            result = productRepository.existsMovie(m.getName(), m.getActor(), m.getDirector());
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다.");
        }

        if (result) {
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }
    }
}
