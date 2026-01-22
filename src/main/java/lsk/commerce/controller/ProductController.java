package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.controller.form.ProductForm;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @PostMapping("/products")
    public String create(@Valid ProductForm form, String... categoryNames) {
        List<Category> categories = new ArrayList<>();
        for (String categoryName : categoryNames) {
            if (categoryService.findCategoryByName(categoryName) == null) {
                throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
            }

            categories.add(categoryService.findCategoryByName(categoryName));
        }

        if (form.getDtype().equals("A")) {
            Album album = new Album(form.getName(), form.getPrice(), form.getStockQuantity(), form.getArtist(), form.getStudio());
            productService.register(album, categories);
        } else if (form.getDtype().equals("B")) {
            Book book = new Book(form.getName(), form.getPrice(), form.getStockQuantity(), form.getAuthor(), form.getIsbn());
            productService.register(book, categories);
        } else {
            Movie movie = new Movie(form.getName(), form.getPrice(), form.getStockQuantity(), form.getDirector(), form.getActor());
            productService.register(movie, categories);
        }

        return form.getName() + " created";
    }

    @GetMapping("/products")
    public List<ProductForm> productList() {
        List<Product> products = productService.findProducts();
        List<ProductForm> productForms = new ArrayList<>();

        for (Product product : products) {
            ProductForm productForm = ProductForm.ProductChangeForm(product);
            productForms.add(productForm);
        }

        return productForms;
    }
}
