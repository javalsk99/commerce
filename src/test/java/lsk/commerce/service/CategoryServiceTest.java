package lsk.commerce.service;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired
    ProductService productService;
    @Autowired
    CategoryService categoryService;

    @Test
    void create() {
        //when
        String parentCategoryName = categoryService.create("컴퓨터/IT", null);
        String childCategoryName1 = categoryService.create("프로그래밍 언어", "컴퓨터/IT");
        String childCategoryName2 = categoryService.create("Java", "프로그래밍 언어");

        //then
        Category findParentCategory = categoryService.findCategoryByName(parentCategoryName);
        Category findChildCategory1 = categoryService.findCategoryByName(childCategoryName1);
        Category findChildCategory2 = categoryService.findCategoryByName(childCategoryName2);

        assertThat(findParentCategory.getParent()).isNull();
        assertThat(findChildCategory1.getParent()).isEqualTo(findParentCategory);
        assertThat(findChildCategory2.getParent().getParent()).isEqualTo(findParentCategory);
        assertThat(findParentCategory.getChild()).contains(findChildCategory1);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("createNameProvider")
    void failed_create_byName(String name, String reason) {
        //when
        assertThrows(ConstraintViolationException.class, () ->
                categoryService.create(name, null));
    }

    @Test
    void failed_create_byParent() {
        //given
        createCategory1();

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.create("발라드", "가"));
    }

    @Test
    void duplicate_create() {
        //given
        createCategory1();
        Category category2 = createCategory2();

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.create("가요", category2.getName()));
    }

    @Test
    void find() {
        //given
        String parentCategory1 = categoryService.create("컴퓨터/IT", null);
        String childCategory1 = categoryService.create("프로그래밍 언어", "컴퓨터/IT");
        String childCategory2 = categoryService.create("Java", "프로그래밍 언어");
        categoryService.create("가요", null);
        String childCategory3 = categoryService.create("발라드", "가요");

        //when
        Category findParentCategory = categoryService.findCategoryByName(parentCategory1);
        List<Category> findCategories = categoryService.findCategoryByNames(childCategory1, childCategory2, childCategory3);
        List<Category> rootCategories = categoryService.findCategories();

        //then
        assertThat(findParentCategory.getName()).isEqualTo("컴퓨터/IT");
        assertThat(findCategories)
                .extracting("name")
                .containsExactlyInAnyOrder("프로그래밍 언어", "Java", "발라드");
        assertThat(rootCategories.size()).isEqualTo(2);
        assertThat(rootCategories)
                .extracting("name")
                .containsExactlyInAnyOrder("컴퓨터/IT", "가요");
    }

    @Test
    void find_ignoreDuplicate() {
        //given
        String categoryName = categoryService.create("컴퓨터/IT", null);

        //when
        List<Category> findCategories = categoryService.findCategoryByNames(categoryName, categoryName);

        //then
        assertThat(findCategories.size()).isEqualTo(1);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nameProvider")
    void failed_find(String name, String reason) {
        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.findCategoryByName(name));
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.findCategoryByNames(name));
    }

    @Test
    void delete() {
        //given
        Category category = createCategory1();

        //when
        categoryService.deleteCategory(category.getName());

        //then
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.findCategoryByName(category.getName()));
    }

    @Test
    void failed_delete_hasChild() {
        //given
        Category category = createCategory1();
        createCategory4();

        //when
        assertThrows(IllegalStateException.class, () ->
                categoryService.deleteCategory(category.getName()));
    }

    @Test
    void failed_delete_hasProduct() {
        //given
        Category category = createCategory1();
        Album album = createAlbum();
        productService.register(album, List.of(category.getName()));

        //when
        assertThrows(IllegalStateException.class, () ->
                categoryService.deleteCategory(category.getName()));
    }

    @Test
    void failed_delete_alreadyDeleted() {
        //given
        Category category = createCategory1();
        categoryService.deleteCategory(category.getName());

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.deleteCategory(category.getName()));
    }

    @Test
    void change_parentCategory() {
        //given
        Category parentCategory = createCategory1();
        Category parentCategory2 = createCategory2();
        String childCategoryName1 = categoryService.create("프로그래밍 언어", parentCategory2.getName());
        String childCategoryName2 = categoryService.create("Java", parentCategory.getName());

        //when
        Category changedCategory = categoryService.changeParentCategory(childCategoryName2, childCategoryName1);

        //then
        assertThat(changedCategory.getParent().getName()).isEqualTo("프로그래밍 언어");
    }

    @Test
    void change_parentCategory_hasChild() {
        //given
        Category parentCategory1 = createCategory1();
        Category parentCategory2 = createCategory2();
        String childCategoryName1 = categoryService.create("프로그래밍 언어", parentCategory1.getName());
        categoryService.create("Java", childCategoryName1);

        //when
        Category changedCategory = categoryService.changeParentCategory(childCategoryName1, parentCategory2.getName());

        //then
        assertThat(changedCategory.getParent().getName()).isEqualTo("컴퓨터/IT");
        assertThat(changedCategory.getChild().getFirst().getName()).isEqualTo("Java");
    }

    @Test
    void change_parentCategory_fromRoot() {
        //given
        Category parentCategory1 = createCategory2();
        String parentCategoryName2 = categoryService.create("프로그래밍 언어", null);
        categoryService.create("Java", parentCategoryName2);

        //when
        Category changedCategory = categoryService.changeParentCategory(parentCategoryName2, parentCategory1.getName());

        //then
        assertThat(changedCategory.getParent().getName()).isEqualTo("컴퓨터/IT");
        assertThat(changedCategory.getChild().getFirst().getName()).isEqualTo("Java");
    }

    @Test
    void failed_change_parentCategory_toSelfOrChild() {
        //given
        Category parentCategory = createCategory2();
        String childCategoryName1 = categoryService.create("프로그래밍 언어", parentCategory.getName());
        String childCategoryName2 = categoryService.create("Java", childCategoryName1);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.changeParentCategory(childCategoryName1, childCategoryName1));
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.changeParentCategory(childCategoryName1, childCategoryName2));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nameProvider")
    void failed_change_parentCategory(String name, String reason) {
        //given
        Category category = createCategory1();

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.changeParentCategory(name, category.getName()));
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.changeParentCategory(category.getName(), name));
    }

    @Test
    void change_dto() {
        //given
        Category parentCategory1 = createCategory1();
        Category parentCategory2 = createCategory2();
        Category childCategory = createCategory4();
        Book book = createBook();
        productService.register(book, List.of(parentCategory2.getName()));

        //when
        CategoryResponse categoryDto = categoryService.getCategoryDto(parentCategory1);
        CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(parentCategory2);

        //then
        assertThat(categoryDto.getName()).isEqualTo("가요");
        assertThat(categoryDto.getChild().getFirst()).isNotEqualTo(childCategory);
        assertThat(categoryDto.getChild().getFirst().getName()).isEqualTo("댄스");

        assertThat(categoryDisconnectResponse.getName()).isEqualTo("컴퓨터/IT");
        assertThat(categoryDisconnectResponse.getProducts().getFirst()).isNotEqualTo(book);
        assertThat(categoryDisconnectResponse.getProducts().getFirst().getName()).isEqualTo("자바 ORM 표준 JPA 프로그래밍");
    }

    private Category createCategory1() {
        return categoryService.findCategoryByName(categoryService.create("가요", null));
    }

    private Category createCategory2() {
        return categoryService.findCategoryByName(categoryService.create("컴퓨터/IT", null));
    }

    private Category createCategory3() {
        return categoryService.findCategoryByName(categoryService.create("Comedy", null));
    }

    private Category createCategory4() {
        return categoryService.findCategoryByName(categoryService.create("댄스", "가요"));
    }

    private Album createAlbum() {
        return new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
    }

    private Book createBook() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private Movie createMovie() {
        return new Movie("굿뉴스", 7000, 15, "설경구", "변성현");
    }

    static Stream<Arguments> createNameProvider() {
        return Stream.of(
                arguments(null, "카테고리 이름 null"),
                arguments("", "카테고리 이름 빈 문자열"),
                arguments(" ", "카테고리 이름 공백"),
                arguments("a".repeat(21), "카테고리 이름 20자 초과")
        );
    }

    static Stream<Arguments> nameProvider() {
        return Stream.of(
                arguments(null, "카테고리 이름 null"),
                arguments("", "카테고리 이름 빈 문자열"),
                arguments(" ", "카테고리 이름 공백"),
                arguments("발라드", "존재하지 않는 카테고리 이름")
        );
    }
}
