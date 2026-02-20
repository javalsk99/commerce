package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    void disconnect() {
        //given
        Category parentCategory = createCategory1();
        Category childCategory = createCategory4();

        Album album = createAlbum1();
        productService.register(album, List.of(childCategory.getName()));

        //when
        categoryProductService.disconnect(childCategory.getName(), album.getName());

        //then
        assertThat(album).isNotNull();
        assertThat(album.getCategoryProducts().size()).isEqualTo(1);
        assertThat(childCategory.getCategoryProducts().size()).isEqualTo(0);
        assertThat(parentCategory.getCategoryProducts())
                .extracting("category", "product")
                .contains(tuple(parentCategory, album));
    }

    @Test
    void failed_disconnect_notConnected() {
        //given
        Category category1 = createCategory1();
        Category category2 = createCategory2();

        Album album = createAlbum1();
        productService.register(album, List.of(category1.getName()));

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.disconnect(category2.getName(), album.getName()));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("namesProvider")
    void failed_disconnect_wrongName(String categoryName, String productName, String reason) {
        //given
        Category category = createCategory1();
        Album album = createAlbum1();
        productService.register(album, List.of(category.getName()));

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.disconnect(categoryName, productName));
    }

    @Test
    void failed_disconnect_alreadyDisconnect() {
        //given
        Category category = createCategory1();
        Album album = createAlbum1();
        productService.register(album, List.of(category.getName()));

        categoryProductService.disconnect(category.getName(), album.getName());

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.disconnect(category.getName(), album.getName()));
    }

    @Test
    void disconnectAll() {
        //given
        Category category = createCategory1();

        Album album1 = createAlbum1();
        Album album2 = createAlbum2();
        Album album3 = createAlbum3();
        productService.register(album1, List.of(category.getName()));
        productService.register(album2, List.of(category.getName()));
        productService.register(album3, List.of(category.getName()));

        //when
        categoryProductService.disconnectAll(category.getName());

        //then
        assertThat(category.getCategoryProducts()).isEmpty();
        assertThat(album1.getCategoryProducts()).isEmpty();
        assertThat(album2.getCategoryProducts()).isEmpty();
        assertThat(album3.getCategoryProducts()).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nameProvider")
    void failed_disconnectAll_wrongName(String name, String reason) {
        //given
        Category category = createCategory1();

        Album album1 = createAlbum1();
        Album album2 = createAlbum2();
        Album album3 = createAlbum3();
        productService.register(album1, List.of(category.getName()));
        productService.register(album2, List.of(category.getName()));
        productService.register(album3, List.of(category.getName()));

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.disconnectAll(name));
    }

    @Test
    void failed_disconnectAll_alreadyDisconnectAll() {
        //given
        Category category = createCategory1();

        Album album1 = createAlbum1();
        Album album2 = createAlbum2();
        Album album3 = createAlbum3();
        productService.register(album1, List.of(category.getName()));
        productService.register(album2, List.of(category.getName()));
        productService.register(album3, List.of(category.getName()));

        categoryProductService.disconnectAll(category.getName());

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.disconnectAll(category.getName()));
    }

    @Test
    void connect() {
        //given
        Category parentCategory = createCategory1();
        Category childCategory = createCategory4();

        Album album = createAlbum1();
        productService.register(album, List.of(parentCategory.getName()));

        //when
        categoryProductService.connect(album.getName(), childCategory.getName());

        //then
        assertThat(album.getCategoryProducts())
                .extracting("category")
                .containsExactlyInAnyOrder(parentCategory, childCategory);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("namesProvider")
    void failed_connect_wrongName(String categoryName, String productName, String reason) {
        //given
        Category category = createCategory1();
        Album album = createAlbum1();
        productService.register(album, List.of(category.getName()));

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.connect(productName, categoryName));
    }

    @Test
    void failed_connect_alreadyConnect() {
        //given
        Category category = createCategory1();
        Album album = createAlbum1();
        productService.register(album, List.of(category.getName()));

        //when
        assertThrows(IllegalArgumentException.class, () ->
                categoryProductService.connect(album.getName(), category.getName()));
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

    private Album createAlbum1() {
        return new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
    }

    private Album createAlbum2() {
        return new Album("BANG BANG", 15000, 10, "IVE", "STARSHIP");
    }

    private Album createAlbum3() {
        return new Album("타임 캡슐", 15000, 15, "다비치", "씨에이엠위더스");
    }

    static Stream<Arguments> namesProvider() {
        return Stream.of(
                arguments(null, "하얀 그리움", "카테고리 이름 null"),
                arguments("", "하얀 그리움", "카테고리 이름 빈 문자열"),
                arguments(" ", "하얀 그리움", "카테고리 이름 공백"),
                arguments("발라드", "하얀 그리움", "존재하지 않는 카테고리"),
                arguments("가요", null, "앨범 이름 null"),
                arguments("가요", "", "앨범 이름 빈 문자열"),
                arguments("가요", " ", "앨범 이름 공백"),
                arguments("가요", "BANG BANG", "존재하지 않는 앨범")
        );
    }

    static Stream<Arguments> nameProvider() {
        return Stream.of(
                arguments(null, "카테고리 이름 null"),
                arguments("", "카테고리 이름 빈 문자열"),
                arguments(" ", "카테고리 이름 공백"),
                arguments("발라드", "존재하지 않는 카테고리")
        );
    }
}
