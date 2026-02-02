/*
package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired
    ProductService productService;
    @Autowired
    CategoryService categoryService;

    @Test
    void create() {
        //given
        Category parentCategory = Category.createParentCategory("컴퓨터/IT");
        Category childCategory1 = Category.createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory2 = Category.createChildCategory(childCategory1, "Java");

        //when
        Long parentCategoryId = categoryService.create(parentCategory);
        Long childCategoryId1 = categoryService.create(childCategory1);
        Long childCategoryId2 = categoryService.create(childCategory2);

        Category findParentCategory = categoryService.findCategory(parentCategoryId);
        Category findChildCategory1 = categoryService.findCategory(childCategoryId1);
        Category findChildCategory2 = categoryService.findCategory(childCategoryId2);
        List<Category> categories = categoryService.findCategories();

        //then
        assertThat(findParentCategory.getParent()).isNull();
        assertThat(findChildCategory1.getParent()).isEqualTo(findParentCategory);
        assertThat(findChildCategory2.getParent().getParent()).isEqualTo(findParentCategory);
        assertThat(findParentCategory.getChild()).contains(findChildCategory1);
        assertThat(categories)
                .extracting(Category::getName)
                .contains("컴퓨터/IT", "프로그래밍 언어", "Java");
    }

    @Test
    void delete() {
        //given
        Category parentCategory = Category.createParentCategory("컴퓨터/IT");
        Category childCategory1 = Category.createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory2 = Category.createChildCategory(childCategory1, "Java");

        Long parentCategoryId = categoryService.create(parentCategory);
        Long childCategoryId1 = categoryService.create(childCategory1);
        Long childCategoryId2 = categoryService.create(childCategory2);

        //when
        categoryService.deleteCategory(childCategory2);
        categoryService.deleteCategory(childCategory1);
        categoryService.deleteCategory(parentCategory);

        Category findParentCategory = categoryService.findCategory(parentCategoryId);
        Category findChildCategory1 = categoryService.findCategory(childCategoryId1);
        Category findChildCategory2 = categoryService.findCategory(childCategoryId2);

        //then
        assertThat(findParentCategory).isNull();
        assertThat(findChildCategory1).isNull();
        assertThat(findChildCategory2).isNull();
    }

    @Test
    void failed_delete() {
        //given
        Category parentCategory = createParentCategory("컴퓨터/IT");
        Category childCategory1 = createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory2 = createChildCategory(childCategory1, "Java");

        Book book = createBook1();
        productService.register(book, childCategory2);

        //when
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(parentCategory);
        } );
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(childCategory2);
        });
    }

    @Test
    void find_products_by_categoryId() {
        //given
        Category parentCategory = createParentCategory("컴퓨터/IT");
        Category childCategory1 = createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory2 = createChildCategory(childCategory1, "Java");
        Category childCategory3 = createChildCategory(childCategory1, "Python");
        Book book1 = createBook1();
        Book book2 = createBook2();

        productService.register(book1, childCategory2);
        productService.register(book2, childCategory3);

        //when
        List<Product> findProductParentCategoryId = categoryService.findProductsByCategoryName(parentCategory.getName());
        List<Product> findProductsByChildCategoryId2 = categoryService.findProductsByCategoryName(childCategory2.getName());
        List<Product> findProductsByChildCategoryId3 = categoryService.findProductsByCategoryName(childCategory3.getName());

        //then
        assertThat(findProductParentCategoryId)
                .extracting(Product::getName)
                .contains("자바 ORM 표준 JPA 프로그래밍", "Python");
        assertThat(findProductsByChildCategoryId2)
                .extracting(Product::getName)
                .contains("자바 ORM 표준 JPA 프로그래밍");
        assertThat(findProductsByChildCategoryId3)
                .extracting(Product::getName)
                .contains("Python");
    }

    @Test
    void change_parentCategory() {
        //given
        Category parentCategory = createParentCategory("컴퓨터/IT");
        Category childCategory1 = createChildCategory(parentCategory, "CS");
        Category childCategory2 = createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory3 = createChildCategory(childCategory1, "Java");

        //when
        Category changedChildCategory3 = childCategory3.changeParentCategory(childCategory2);

        //then
        assertThat(changedChildCategory3.getParent()).isEqualTo(childCategory2);
    }

    @Test
    void failed_change_parentCategory() {
        //given
        Category parentCategory1 = createParentCategory("컴퓨터/IT");
        Category parentCategory2 = createParentCategory("만화");
        Category childCategory1 = createChildCategory(parentCategory1, "CS");
        Category childCategory2 = createChildCategory(childCategory1, "Java");

        //when
        assertThrows(IllegalStateException.class, () -> {
            childCategory1.changeParentCategory(parentCategory2);
        });
    }

    private Book createBook1() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private Book createBook2() {
        return new Book("Python", 10000, 10, "author", "1111111111111");
    }

    private Category createParentCategory(String name) {
        Category category = Category.createParentCategory(name);
        return categoryService.findCategory(categoryService.create(category));
    }

    private Category createChildCategory(Category parentCategory, String name) {
        Category category = Category.createChildCategory(parentCategory, name);
        return categoryService.findCategory(categoryService.create(category));
    }
}*/
