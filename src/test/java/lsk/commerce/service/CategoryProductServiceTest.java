package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.product.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryProductServiceTest {

    @Autowired
    ProductService productService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    CategoryProductService categoryProductService;

    @Test
    void remove_connect() {
        //given
        Category parentCategory = createParentCategory("컴퓨터/IT");
        Category childCategory = createChildCategory(parentCategory, "프로그래밍 언어");

        Book book = createBook();
        productService.register(book, childCategory);

        //when
        categoryProductService.disconnect(childCategory, book);

        //then
        assertThat(book).isNotNull();
        assertThat(book.getCategoryProducts().size()).isEqualTo(1);
        assertThat(childCategory.getCategoryProducts().size()).isEqualTo(0);
        assertThat(parentCategory.getCategoryProducts())
                .extracting(CategoryProduct::getCategory, CategoryProduct::getProduct)
                .contains(tuple(parentCategory, book));
    }

    private Book createBook() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private Category createParentCategory(String name) {
        Category category = Category.createParentCategory(name);
        return categoryService.findCategory(categoryService.create(category));
    }

    private Category createChildCategory(Category parentCategory, String name) {
        Category category = Category.createChildCategory(parentCategory, name);
        return categoryService.findCategory(categoryService.create(category));
    }
}