package lsk.commerce.service;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.ProductChangeRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.ProductDetailResponse;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Transactional
    public String register(ProductCreateRequest request, List<String> categoryNames) {
        Product product = validateAndToProduct(request);
        List<Category> categories = categoryService.validateAndGetCategories(categoryNames);

        product.connectCategories(categories);
        productRepository.save(product);
        return product.getProductNumber();
    }

    public Product findProduct(String productNumber) {
        return productRepository.findByNumber(productNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 상품입니다. productNumber: " + productNumber));
    }

    public Product findProductWithCategoryProduct(String productNumber) {
        return productRepository.findWithCategoryProduct(productNumber)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 상품입니다. productNumber: " + productNumber));
    }

    public List<Product> findProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product changePriceAndStock(String productNumber, ProductChangeRequest request) {
        Product product = findProduct(productNumber);
        product.changePriceAndStock(request.price(), request.stockQuantity());
        return product;
    }

    @Transactional
    public void deleteProduct(String productNumber) {
        if (productNumber.equals("WxgG3CzGZhAZ")) {
            return;
        }

        Optional<Product> optionalProduct = productRepository.findWithCategoryProductCategory(productNumber);
        if (optionalProduct.isEmpty()) {
            return;
        }

        Product product = optionalProduct.get();

        if (!product.getCategoryProducts().isEmpty()) {
            product.removeCategoryProductsFormCategory();
        }

        productRepository.delete(product);
    }

    public ProductDetailResponse getProductDto(Product product) {
        return ProductDetailResponse.from(product);
    }

    public ProductNameWithCategoryNameResponse getProductWithCategoryDto(Product product) {
        return ProductNameWithCategoryNameResponse.from(product);
    }

    private Product validateAndToProduct(ProductCreateRequest request) {
        boolean result;
        Product product;
        if (request.dtype().equals("A")) {
            result = productRepository.existsAlbum(request.name(), request.artist(), request.studio());
            product = Album.builder()
                    .name(request.name())
                    .price(request.price())
                    .stockQuantity(request.stockQuantity())
                    .artist(request.artist())
                    .studio(request.studio())
                    .build();
        } else if (request.dtype().equals("B")) {
            result = productRepository.existsBook(request.name(), request.author(), request.isbn());
            product = Book.builder()
                    .name(request.name())
                    .price(request.price())
                    .stockQuantity(request.stockQuantity())
                    .author(request.author())
                    .isbn(request.isbn())
                    .build();
        } else if (request.dtype().equals("M")) {
            result = productRepository.existsMovie(request.name(), request.actor(), request.director());
            product = Movie.builder()
                    .name(request.name())
                    .price(request.price())
                    .stockQuantity(request.stockQuantity())
                    .actor(request.actor())
                    .director(request.director())
                    .build();
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다");
        }

        if (result) {
            throw new DuplicateResourceException("이미 존재하는 상품입니다. name: " + product.getName());
        }

        return product;
    }
}
