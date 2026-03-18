package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.request.ProductUpdateRequest;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.ProductService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    CategoryProductService categoryProductService;

    @MockitoBean
    ProductQueryService productQueryService;

    String productNumber = "9sj2nks876xm";

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(productService.register(any(ProductCreateRequest.class), anyList())).willReturn("BANG BANG");

                //when & then
                mvc.perform(post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .param("categoryNames", "가요,댄스"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("BANG BANG"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(productService).should().register(request, List.of("가요", "댄스"));
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidCreateRequestCategoryNamesProvider")
            void invalidInput(ProductCreateRequest request, String categoryName) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .param("categoryNames", categoryName))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(productService).should(never()).register(any(), any());
            }

            @Test
            void register_Failed_DuplicateProduct() throws Exception {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(productService.register(any(ProductCreateRequest.class), anyList())).willThrow(new IllegalArgumentException("이미 존재하는 상품입니다"));

                //when & then
                mvc.perform(post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .param("categoryNames", "가요"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_ARGUMENT"))
                        .andExpect(jsonPath("$.message").value("이미 존재하는 상품입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(productService).should().register(request, List.of("가요"));
            }

            @Test
            void otherDtypeFields() throws Exception {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .actor("actor")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .param("categoryNames", "가요"))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(productService).should(never()).register(any(), any());
            }

            static Stream<Arguments> invalidCreateRequestCategoryNamesProvider() {
                return Stream.of(
                        argumentSet("productName null",
                                ProductCreateRequest.builder()
                                        .price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("price null",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("stockQuantity null",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("dtype null",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("dtype 빈 문자열",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("dtype 공백",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype(" ").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("dtype 1자 초과",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("a".repeat(2)).artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요"
                        ),
                        argumentSet("categoryName null",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                null
                        ),
                        argumentSet("categoryName 빈 문자열",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                ""
                        ),
                        argumentSet("categoryName 공백",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                " "
                        ),
                        argumentSet("categoryName 20자 초과",
                                ProductCreateRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "a".repeat(21)
                        )
                );
            }
        }
    }

    @Nested
    class ProductList {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album1 = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                Album album2 = Album.builder()
                        .name("BLACKHOLE")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                ProductResponse productResponse1 = ProductResponse.from(album1);
                ProductResponse productResponse2 = ProductResponse.from(album2);

                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("productName", "b");
                cond.add("artist", "i");

                given(productQueryService.searchProducts(any(ProductSearchCond.class))).willReturn(List.of(productResponse1, productResponse2));

                //when & then
                mvc.perform(get("/products")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data[1].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.count").value(2))
                        .andDo(print());

                //then
                BDDMockito.then(productQueryService).should().searchProducts(any(ProductSearchCond.class));
            }

            @Test
            void notFound() throws Exception {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("productName", "b");
                cond.add("artist", "ㄱ");

                given(productQueryService.searchProducts(any(ProductSearchCond.class))).willReturn(Collections.emptyList());

                //when & then
                mvc.perform(get("/products")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isEmpty())
                        .andExpect(jsonPath("$.count").value(0))
                        .andDo(print());

                //then
                BDDMockito.then(productQueryService).should().searchProducts(any(ProductSearchCond.class));
            }
        }
    }

    @Nested
    class FindProduct {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                ProductResponse productResponse = ProductResponse.from(album);

                given(productService.findProduct(anyString())).willReturn(album);
                given(productService.getProductDto(any(Product.class))).willReturn(productResponse);

                //when & then
                mvc.perform(get("/products/{productNumber}", productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.dtype").value("A"))
                        .andExpect(jsonPath("$.data.artist").value("IVE"))
                        .andExpect(jsonPath("$.data.studio").value("STARSHIP"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should().findProduct(productNumber));
                    softly.check(() -> BDDMockito.then(productService).should().getProductDto(album));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void findProductByName_Failed_ProductNotFound() throws Exception {
                //given
                given(productService.findProduct(anyString())).willThrow(new DataNotFoundException("존재하지 않는 상품입니다."));

                //when & then
                mvc.perform(get("/products/{productNumber}", "lllIIllI00OO"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should().findProduct("lllIIllI00OO"));
                    softly.check(() -> BDDMockito.then(productService).should(never()).getProductDto(any(Product.class)));
                });
            }
        }
    }

    @Nested
    class UpdateProduct {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                ProductUpdateRequest request = new ProductUpdateRequest(20000, 8);
                String json = objectMapper.writeValueAsString(request);

                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(20000)
                        .stockQuantity(8)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                ProductResponse productResponse = ProductResponse.from(album);

                given(productService.updateProduct(anyString(), any(ProductUpdateRequest.class))).willReturn(album);
                given(productService.getProductDto(any(Product.class))).willReturn(productResponse);

                //when & then
                mvc.perform(patch("/products/{productNumber}", productNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.price").value(20000))
                        .andExpect(jsonPath("$.data.stockQuantity").value(8))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should().updateProduct(productNumber, request));
                    softly.check(() -> BDDMockito.then(productService).should().getProductDto(album));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                ProductUpdateRequest request = new ProductUpdateRequest(20000, 8);
                String json = objectMapper.writeValueAsString(request);

                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(20000)
                        .stockQuantity(8)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                ProductResponse productResponse = ProductResponse.from(album);

                given(productService.updateProduct(anyString(), any(ProductUpdateRequest.class))).willReturn(album);
                given(productService.getProductDto(any(Product.class))).willReturn(productResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/products/{productNumber}", productNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.price").value(20000))
                        .andExpect(jsonPath("$.data.stockQuantity").value(8))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should().updateProduct(productNumber, request));
                    softly.check(() -> BDDMockito.then(productService).should().getProductDto(album));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/products/{productNumber}", productNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.price").value(20000))
                        .andExpect(jsonPath("$.data.stockQuantity").value(8))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should(times(2)).updateProduct(productNumber, request));
                    softly.check(() -> BDDMockito.then(productService).should(times(2)).getProductDto(album));
                });
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidUpdateRequestProvider")
            void invalidInput(ProductUpdateRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(patch("/products/{productNumber}", productNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productService).should(never()).updateProduct(any(), any()));
                    softly.check(() -> BDDMockito.then(productService).should(never()).getProductDto(any()));
                });
            }

            static Stream<Arguments> invalidUpdateRequestProvider() {
                return Stream.of(
                        argumentSet("price null", new ProductUpdateRequest(null, 8)),
                        argumentSet("price 100원 미만", new ProductUpdateRequest(99, 8)),
                        argumentSet("stockQuantity null", new ProductUpdateRequest(20000, null))
                );
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                //when & then
                mvc.perform(delete("/products/{productNumber}", productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(productService).should().deleteProduct(productNumber);
            }

            @Test
            void idempotency() throws Exception {
                //given
                Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                //when & then 첫 번째 요청
                mvc.perform(delete("/products/{productNumber}", productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(productService).should().deleteProduct(productNumber);

                //when & then 두 번째 요청
                mvc.perform(delete("/products/{productNumber}", productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(productService).should(times(2)).deleteProduct(productNumber);
            }
        }
    }

    @Nested
    class ConnectCategory {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                Category category1 = Category.createCategory(null, "가요");
                Category.createCategory(category1, "댄스");

                ReflectionTestUtils.setField(category1, "id", 1L);

                album.connectCategory(category1);

                ProductNameWithCategoryNameResponse productNameWithCategoryNameResponse = getProductNameWithCategoryNameResponse();

                given(categoryProductService.connect(anyString(), anyString())).willReturn(album);
                given(productService.getProductWithCategoryDto(any(Product.class))).willReturn(productNameWithCategoryNameResponse);

                //when & then
                mvc.perform(patch("/products/{productNumber}/{categoryName}", productNumber, "댄스"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[0].categoryName").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[1].categoryName").value("댄스"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().connect(productNumber, "댄스"));
                    softly.check(() -> BDDMockito.then(productService).should().getProductWithCategoryDto(album));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                Category category1 = Category.createCategory(null, "가요");
                Category.createCategory(category1, "댄스");

                ReflectionTestUtils.setField(category1, "id", 1L);

                album.connectCategory(category1);

                ProductNameWithCategoryNameResponse productNameWithCategoryNameResponse = getProductNameWithCategoryNameResponse();

                given(categoryProductService.connect(anyString(), anyString())).willReturn(album);
                given(productService.getProductWithCategoryDto(any(Product.class))).willReturn(productNameWithCategoryNameResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/products/{productNumber}/{categoryName}", productNumber, "댄스"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[0].categoryName").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[1].categoryName").value("댄스"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().connect(productNumber, "댄스"));
                    softly.check(() -> BDDMockito.then(productService).should().getProductWithCategoryDto(album));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/products/{productNumber}/{categoryName}", productNumber, "댄스"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[0].categoryName").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNameResponseList[1].categoryName").value("댄스"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should(times(2)).connect(productNumber, "댄스"));
                    softly.check(() -> BDDMockito.then(productService).should(times(2)).getProductWithCategoryDto(album));
                });
            }

            private static ProductNameWithCategoryNameResponse getProductNameWithCategoryNameResponse() {
                ProductNameWithCategoryNameResponse.CategoryNameResponse categoryNameResponse1 = new ProductNameWithCategoryNameResponse.CategoryNameResponse("가요");
                ProductNameWithCategoryNameResponse.CategoryNameResponse categoryNameResponse2 = new ProductNameWithCategoryNameResponse.CategoryNameResponse("댄스");
                return new ProductNameWithCategoryNameResponse("BANG BANG", List.of(categoryNameResponse1, categoryNameResponse2));
            }
        }

        @Nested
        class FailureCase {

            @Test
            void categoryNotFound() throws Exception {
                //given
                Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(categoryProductService.connect(anyString(), anyString())).willThrow(new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + "록"));

                //when & then
                mvc.perform(patch("/products/{productNumber}/{categoryName}", productNumber, "록"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다. name: " + "록"))
                        .andDo(print());

                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().connect(productNumber, "록"));
                    softly.check(() -> BDDMockito.then(productService).should(never()).getProductWithCategoryDto(any()));
                });
            }
        }
    }
}