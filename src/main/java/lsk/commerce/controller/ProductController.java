package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.dto.request.ProductUpdateRequest;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.ProductWithCategoryResponse;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CategoryProductService categoryProductService;
    private final ProductQueryService productQueryService;

    @PostMapping("/products")
    public String create(@Valid ProductRequest request, String... categoryNames) {
        List<Category> categories = categoryService.findCategoryByNames(categoryNames);

        if (request.getDtype().equals("A")) {
            Album album = new Album(request.getName(), request.getPrice(), request.getStockQuantity(), request.getArtist(), request.getStudio());
            productService.register(album, categories);
        } else if (request.getDtype().equals("B")) {
            Book book = new Book(request.getName(), request.getPrice(), request.getStockQuantity(), request.getAuthor(), request.getIsbn());
            productService.register(book, categories);
        } else if (request.getDtype().equals("M")) {
            Movie movie = new Movie(request.getName(), request.getPrice(), request.getStockQuantity(), request.getActor(), request.getDirector());
            productService.register(movie, categories);
        } else {
            throw new IllegalArgumentException("잘못된 양식입니다. dtype: " + request.getDtype());
        }

        return request.getName() + " created";
    }

    @GetMapping("/products")
    public List<ProductResponse> productList(@ModelAttribute ProductSearchCond cond) {
        return productQueryService.searchProducts(cond);
    }

    @GetMapping("/products/{productName}")
    public ProductResponse findProduct(@PathVariable("productName") String productName) {
        Product product = productService.findProductByName(productName);
        return productService.getProductDto(product);
    }

    @PostMapping("/products/{productName}")
    public ProductResponse updateProduct(@PathVariable("productName") String productName, ProductUpdateRequest request) {
        Product product = productService.updateProduct(productName, request.getPrice(), request.getStockQuantity());
        return productService.getProductDto(product);
    }

    @DeleteMapping("/products/{productName}")
    public String delete(@PathVariable("productName") String productName) {
        productService.deleteProduct(productName);
        return "delete";
    }

    @PostMapping("/products/{productName}/{categoryName}")
    public ProductWithCategoryResponse connectCategory(@PathVariable("productName") String productName, @PathVariable("categoryName") String categoryName) {
        Product product = categoryProductService.connect(productName, categoryName);
        return productService.getProductWithCategoryDto(product);
    }
}
