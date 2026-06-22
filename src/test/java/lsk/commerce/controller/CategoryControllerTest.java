package lsk.commerce.controller;

import tools.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.dto.response.ProductDetailResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.CategoryQueryService;
import lsk.commerce.query.dto.CategoryProductQueryDto;
import lsk.commerce.query.dto.CategoryQueryDto;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class CategoryControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CategoryService categoryService;

    @MockitoBean
    CategoryProductService categoryProductService;

    @MockitoBean
    CategoryQueryService categoryQueryService;

    String productNumber = "ngf7x89dbbh3";
    String wrongCategoryNumber = "llII11OO00OO";

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("가요", null);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.create(any(CategoryCreateRequest.class))).willReturn("가요");

                //when & then
                mvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data").value("가요"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().create(request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidCreateRequestProvider")
            void invalidInput(CategoryCreateRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should(never()).create(any());
            }

            @Test
            void create_Failed_ParentNotFound() throws Exception {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("댄스", wrongCategoryNumber);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.create(any(CategoryCreateRequest.class))).willThrow(new DataNotFoundException("부모 카테고리가 존재하지 않습니다. parentNumber: " + wrongCategoryNumber));

                //when & then
                mvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("부모 카테고리가 존재하지 않습니다. parentNumber: " + wrongCategoryNumber))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().create(request);
            }

            static Stream<Arguments> invalidCreateRequestProvider() {
                return Stream.of(
                        argumentSet("categoryName null", new CategoryCreateRequest(null, null)),
                        argumentSet("categoryName 빈 문자열", new CategoryCreateRequest("", null)),
                        argumentSet("categoryName 공백", new CategoryCreateRequest(" ", null)),
                        argumentSet("categoryName 20자 초과", new CategoryCreateRequest("a".repeat(21), null))
                );
            }
        }
    }

    @Nested
    class CategoryList {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Category parentCategory = Category.createCategory(null, "가요");
                Category childCategory = Category.createCategory(parentCategory, "댄스");

                given(categoryService.findRootCategories()).willReturn(List.of(parentCategory, childCategory));
                given(categoryService.getCategoryDto(any(Category.class))).willAnswer(invocation -> {
                    Category category = invocation.getArgument(0, Category.class);
                    return CategoryResponse.from(category);
                });

                //when & then
                mvc.perform(get("/categories"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].name").value("가요"))
                        .andExpect(jsonPath("$.data[0].children[0].name").value("댄스"))
                        .andExpect(jsonPath("$.data[0].children[0].children").isEmpty())
                        .andExpect(jsonPath("$.data[1].name").value("댄스"))
                        .andExpect(jsonPath("$.data[1].children").isEmpty())
                        .andExpect(jsonPath("$.count").value(2))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().findRootCategories());
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).getCategoryDto(any(Category.class)));
                });
            }
        }
    }

    private abstract class Setup {

        String parentNumber;
        Category childCategory;

        @BeforeEach
        void beforeEach() {
            Category category = Category.createCategory(null, "가요");
            parentNumber = category.getCategoryNumber();
            childCategory = Category.createCategory(null, "댄스");
        }
    }

    @Nested
    class FindCategory {

        @Nested
        class SuccessCase extends Setup {

            @Test
            void basic() throws Exception {
                //given
                CategoryQueryDto categoryQueryDto = new CategoryQueryDto("가요", parentNumber, List.of(new CategoryProductQueryDto("가요", "BANG BANG", productNumber)));

                given(categoryQueryService.findCategory(anyString())).willReturn(categoryQueryDto);

                //when & then
                mvc.perform(get("/categories/{categoryNumber}", parentNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.categoryName").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNumber").value(parentNumber))
                        .andExpect(jsonPath("$.data.categoryProductQueryDtoList[0].productName").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.categoryProductQueryDtoList[0].productNumber").value(productNumber))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(categoryQueryService).should().findCategory(parentNumber);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void findCategoryName_Failed_categoryNotFound() throws Exception {
                //given
                given(categoryQueryService.findCategory(anyString())).willThrow(new DataNotFoundException("존재하지 않는 카테고리입니다. categoryNumber: " + wrongCategoryNumber));

                //when & then
                mvc.perform(get("/categories/{categoryNumber}", wrongCategoryNumber))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다. categoryNumber: " + wrongCategoryNumber))
                        .andDo(print());

                //then
                BDDMockito.then(categoryQueryService).should().findCategory(wrongCategoryNumber);
            }
        }
    }

    @Nested
    class ChangeParentCategory extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                CategoryResponse childCategoryResponse = new CategoryResponse("댄스", childCategory.getCategoryNumber(), Collections.emptyList());
                CategoryResponse categoryResponse = new CategoryResponse("가요", parentNumber, List.of(childCategoryResponse));
                CategoryChangeParentRequest request = new CategoryChangeParentRequest(parentNumber);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.changeParentCategory(anyString(), any(CategoryChangeParentRequest.class))).willReturn(childCategory);
                given(categoryService.getCategoryDto(any(Category.class))).willReturn(categoryResponse);

                //when & then
                mvc.perform(patch("/categories/{categoryNumber}", childCategory.getCategoryNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNumber").value(parentNumber))
                        .andExpect(jsonPath("$.data.children[0].name").value("댄스"))
                        .andExpect(jsonPath("$.data.children[0].categoryNumber").value(childCategory.getCategoryNumber()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().changeParentCategory(childCategory.getCategoryNumber(), request));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDto(any(Category.class)));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Category childCategory = Category.createCategory(null, "댄스");
                CategoryResponse childCategoryResponse = new CategoryResponse("댄스", childCategory.getCategoryNumber(), Collections.emptyList());
                CategoryResponse categoryResponse = new CategoryResponse("가요", parentNumber, List.of(childCategoryResponse));
                CategoryChangeParentRequest request = new CategoryChangeParentRequest(parentNumber);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.changeParentCategory(anyString(), any(CategoryChangeParentRequest.class))).willReturn(childCategory);
                given(categoryService.getCategoryDto(any(Category.class))).willReturn(categoryResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/categories/{categoryNumber}", childCategory.getCategoryNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNumber").value(parentNumber))
                        .andExpect(jsonPath("$.data.children[0].name").value("댄스"))
                        .andExpect(jsonPath("$.data.children[0].categoryNumber").value(childCategory.getCategoryNumber()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().changeParentCategory(childCategory.getCategoryNumber(), request));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDto(any(Category.class)));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/categories/{categoryNumber}", childCategory.getCategoryNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.categoryNumber").value(parentNumber))
                        .andExpect(jsonPath("$.data.children[0].name").value("댄스"))
                        .andExpect(jsonPath("$.data.children[0].categoryNumber").value(childCategory.getCategoryNumber()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).changeParentCategory(childCategory.getCategoryNumber(), request));
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).getCategoryDto(any(Category.class)));
                });
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidParentRequestProvider")
            void invalidInput(CategoryChangeParentRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(patch("/categories/{categoryNumber}", childCategory.getCategoryNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).changeParentCategory(any(), any()));
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).getCategoryDto(any()));
                });
            }

            @Test
            void changeParentCategory_Failed_ParentNotFound() throws Exception {
                //given
                CategoryChangeParentRequest request = new CategoryChangeParentRequest(wrongCategoryNumber);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.changeParentCategory(anyString(), any(CategoryChangeParentRequest.class))).willThrow(new DataNotFoundException("부모 카테고리가 존재하지 않습니다. parentNumber: " + wrongCategoryNumber));

                //when & then
                mvc.perform(patch("/categories/{categoryNumber}", childCategory.getCategoryNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("부모 카테고리가 존재하지 않습니다. parentNumber: " + wrongCategoryNumber))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().changeParentCategory(childCategory.getCategoryNumber(), request));
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).getCategoryDto(any()));
                });
            }

            static Stream<Arguments> invalidParentRequestProvider() {
                return Stream.of(
                        argumentSet("categoryNumber null", new CategoryChangeParentRequest(null)),
                        argumentSet("categoryNumber 빈 문자열", new CategoryChangeParentRequest("")),
                        argumentSet("categoryNumber 공백", new CategoryChangeParentRequest(" ")),
                        argumentSet("categoryNumber 12자 미만", new CategoryChangeParentRequest("a".repeat(11))),
                        argumentSet("categoryNumber 12자 초과", new CategoryChangeParentRequest("a".repeat(13)))
                );
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase extends Setup {

            @Test
            void basic() throws Exception {
                //when & then
                mvc.perform(delete("/categories/{categoryNumber}", childCategory.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().deleteCategory(childCategory.getCategoryNumber());
            }

            @Test
            void idempotency() throws Exception {
                //when & then 첫 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}", childCategory.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().deleteCategory(childCategory.getCategoryNumber());

                //when & then 두 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}", childCategory.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should(times(2)).deleteCategory(childCategory.getCategoryNumber());
            }
        }

        @Nested
        class FailureCase {

            @Test
            void deleteCategory_Failed_hasChild() throws Exception {
                //given
                Category parentCategory = Category.createCategory(null, "가요");
                Category.createCategory(parentCategory, "댄스");

                willThrow(new IllegalStateException("자식 카테고리가 있어서 삭제할 수 없습니다")).given(categoryService).deleteCategory(anyString());

                //when & then
                mvc.perform(delete("/categories/{categoryNumber}", parentCategory.getCategoryNumber()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_STATUS"))
                        .andExpect(jsonPath("$.message").value("자식 카테고리가 있어서 삭제할 수 없습니다"))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().deleteCategory(parentCategory.getCategoryNumber());
            }
        }
    }

    @Nested
    class DisconnectProduct {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Category category = Category.createCategory(null, "가요");
                Album album1 = Album.builder().name("BANG BANG").build();
                Album album2 = Album.builder().name("타임 캡슐").build();

                ReflectionTestUtils.setField(category, "id", 1L);

                album1.connectCategory(category);
                album2.connectCategory(category);

                ProductDetailResponse productResponse = ProductDetailResponse.from(album1);
                CategoryDisconnectResponse categoryDisconnectResponse = new CategoryDisconnectResponse("가요", List.of(productResponse));

                given(categoryProductService.disconnect(anyString(), anyString())).willReturn(category);
                given(categoryService.getCategoryDisconnectResponse(any(Category.class))).willReturn(categoryDisconnectResponse);

                //when & then
                mvc.perform(delete("/categories/{categoryNumber}/{productNumber}", category.getCategoryNumber(), productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().disconnect(category.getCategoryNumber(), productNumber));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDisconnectResponse(category));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Category category = Category.createCategory(null, "가요");
                Album album1 = Album.builder().name("BANG BANG").build();
                Album album2 = Album.builder().name("타임 캡슐").build();

                ReflectionTestUtils.setField(category, "id", 1L);

                album1.connectCategory(category);
                album2.connectCategory(category);

                ProductDetailResponse productResponse = ProductDetailResponse.from(album1);
                CategoryDisconnectResponse categoryDisconnectResponse = new CategoryDisconnectResponse("가요", List.of(productResponse));

                given(categoryProductService.disconnect(anyString(), anyString())).willReturn(category);
                given(categoryService.getCategoryDisconnectResponse(any(Category.class))).willReturn(categoryDisconnectResponse);

                //when & then 첫 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}/{productNumber}", category.getCategoryNumber(), productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().disconnect(category.getCategoryNumber(), productNumber));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDisconnectResponse(category));
                });

                //when & then 두 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}/{productNumber}", category.getCategoryNumber(), productNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should(times(2)).disconnect(category.getCategoryNumber(), productNumber));
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).getCategoryDisconnectResponse(category));
                });
            }
        }

        @Nested
        class FailureCase extends Setup {

            @Test
            void disconnect_Failed_ProductNotFound() throws Exception {
                //given
                given(categoryProductService.disconnect(anyString(), anyString())).willThrow(new DataNotFoundException("존재하지 않는 상품입니다. productNumber: " + "lllIIIll00OO"));

                //when & then
                mvc.perform(delete("/categories/{categoryNumber}/{productNumber}", parentNumber, "lllIIIll00OO"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다. productNumber: " + "lllIIIll00OO"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().disconnect(parentNumber, "lllIIIll00OO"));
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).getCategoryDisconnectResponse(any()));
                });
            }
        }
    }

    @Nested
    class DisconnectProducts {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Category category = Category.createCategory(null, "가요");
                Album album1 = Album.builder().name("BANG BANG").build();
                Album album2 = Album.builder().name("타임 캡슐").build();

                ReflectionTestUtils.setField(category, "id", 1L);

                album1.connectCategory(category);
                album2.connectCategory(category);

                CategoryDisconnectResponse categoryDisconnectResponse = new CategoryDisconnectResponse("가요", Collections.emptyList());

                given(categoryProductService.disconnectAll(anyString())).willReturn(category);
                given(categoryService.getCategoryDisconnectResponse(any(Category.class))).willReturn(categoryDisconnectResponse);

                //when & then
                mvc.perform(delete("/categories/{categoryNumber}/products", category.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList").isEmpty())
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().disconnectAll(category.getCategoryNumber()));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDisconnectResponse(category));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Category category = Category.createCategory(null, "가요");
                Album album1 = Album.builder().name("BANG BANG").build();
                Album album2 = Album.builder().name("타임 캡슐").build();

                ReflectionTestUtils.setField(category, "id", 1L);

                album1.connectCategory(category);
                album2.connectCategory(category);

                CategoryDisconnectResponse categoryDisconnectResponse = new CategoryDisconnectResponse("가요", Collections.emptyList());

                given(categoryProductService.disconnectAll(anyString())).willReturn(category);
                given(categoryService.getCategoryDisconnectResponse(any(Category.class))).willReturn(categoryDisconnectResponse);

                //when & then 첫 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}/products", category.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList").isEmpty())
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should().disconnectAll(category.getCategoryNumber()));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDisconnectResponse(category));
                });

                //when & then 두 번째 요청
                mvc.perform(delete("/categories/{categoryNumber}/products", category.getCategoryNumber()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.productResponseList").isEmpty())
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryProductService).should(times(2)).disconnectAll(category.getCategoryNumber()));
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).getCategoryDisconnectResponse(category));
                });
            }
        }
    }
}