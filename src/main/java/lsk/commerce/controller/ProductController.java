package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.ProductUpdateRequest;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CategoryProductService categoryProductService;

    @PostMapping("/products")
    public String create(@Valid ProductRequest request, String... categoryNames) {
        List<Category> categories = new ArrayList<>();
        for (String categoryName : categoryNames) {
            if (categoryService.findCategoryByName(categoryName) == null) {
                throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
            }

            categories.add(categoryService.findCategoryByName(categoryName));
        }

        if (request.getDtype().equals("A")) {
            Album album = new Album(request.getName(), request.getPrice(), request.getStockQuantity(), request.getArtist(), request.getStudio());
            productService.register(album, categories);
        } else if (request.getDtype().equals("B")) {
            Book book = new Book(request.getName(), request.getPrice(), request.getStockQuantity(), request.getAuthor(), request.getIsbn());
            productService.register(book, categories);
        } else if (request.getDtype().equals("M")) {
            Movie movie = new Movie(request.getName(), request.getPrice(), request.getStockQuantity(), request.getDirector(), request.getActor());
            productService.register(movie, categories);
        } else {
            throw new IllegalArgumentException("잘못된 양식입니다. dtype: " + request.getDtype());
        }

        return request.getName() + " created";
    }

    @GetMapping("/products")
    public List<ProductResponse> productList() {
        List<Product> products = productService.findProducts();
        List<ProductResponse> productResponses = new ArrayList<>();

        for (Product product : products) {
            ProductResponse productDto = productService.getProductDto(product);
            productResponses.add(productDto);
        }

        return productResponses;
    }

    @GetMapping("/products/{productName}")
    public ProductResponse findProduct(@PathVariable("productName") String productName) {
        if (productService.findProductByName(productName) == null) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName);
        }

        Product product = productService.findProductByName(productName);
        return productService.getProductDto(product);
    }

    @PostMapping("/products/{productName}")
    public ProductResponse updateProduct(@PathVariable("productName") String productName, ProductUpdateRequest request) {
        if (productService.findProductByName(productName) == null) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName);
        }

        Product product = productService.findProductByName(productName);
        productService.updateProduct(product.getId(), request.getPrice(), request.getStockQuantity());
        return productService.getProductDto(product);
    }

    @DeleteMapping("/products/{productName}")
    public String delete(@PathVariable("productName") String productName) {
        if (productService.findProductByName(productName) == null) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName);
        }

        Product product = productService.findProductByName(productName);
        productService.deleteProduct(product);
        return "delete";
    }

    @PostMapping("/products/{productName}/{categoryName}")
    public List<ProductResponse> connectCategory(@PathVariable("productName") String productName, @PathVariable("categoryName") String categoryName) {
        if (productService.findProductByName(productName) == null) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다. name: " + productName);
        } else if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
        }

        Product product = productService.findProductByName(productName);
        Category category = categoryService.findCategoryByName(categoryName);
        categoryProductService.connect(product, category);

        List<Product> products = categoryService.findProductsByCategoryName(categoryName);
        List<ProductResponse> productResponses = new ArrayList<>();
        for (Product findProduct : products) {
            ProductResponse productDto = productService.getProductDto(findProduct);
            productResponses.add(productDto);
        }

        return productResponses;
    }
}
