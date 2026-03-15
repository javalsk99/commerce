package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

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

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                CategoryRequest request = new CategoryRequest("가요", null);
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.create(any(CategoryRequest.class))).willReturn("가요");

                //when & then
                mvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(content().string("가요"))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().create(request);
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidCategoryRequestProvider")
            void invalidInput(CategoryRequest request) throws Exception {
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
                CategoryRequest request = new CategoryRequest("댄스", "록");
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.create(any(CategoryRequest.class))).willThrow(new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + "록"));

                //when & then
                mvc.perform(post("/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다. name: " + "록"))
                        .andDo(print());

                //then
                BDDMockito.then(categoryService).should().create(request);
            }

            static Stream<Arguments> invalidCategoryRequestProvider() {
                return Stream.of(
                        argumentSet("categoryName null", new CategoryRequest(null, null)),
                        argumentSet("categoryName 빈 문자열", new CategoryRequest("", null)),
                        argumentSet("categoryName 공백", new CategoryRequest(" ", null)),
                        argumentSet("categoryName 20자 초과", new CategoryRequest("a".repeat(21), null))
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

                given(categoryService.findCategories()).willReturn(List.of(parentCategory, childCategory));
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
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().findCategories());
                    softly.check(() -> BDDMockito.then(categoryService).should(times(2)).getCategoryDto(any(Category.class)));
                });
            }
        }
    }

    @Nested
    class FindCategory {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Category category = Category.createCategory(null, "가요");
                CategoryResponse categoryResponse = CategoryResponse.from(category);

                given(categoryService.findCategoryByName(anyString())).willReturn(category);
                given(categoryService.getCategoryDto(any(Category.class))).willReturn(categoryResponse);

                //when & then
                mvc.perform(get("/categories/{categoryName}", "가요"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.children").isEmpty())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().findCategoryByName("가요"));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDto(category));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void findCategoryName_Failed_categoryNotFound() throws Exception {
                //given
                given(categoryService.findCategoryByName(anyString())).willThrow(new DataNotFoundException("존재하지 않는 카테고리입니다. name: " + "록"));

                //when & then
                mvc.perform(get("/categories/{categoryName}", "록"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다. name: " + "록"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().findCategoryByName("록"));
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).getCategoryDto(any(Category.class)));
                });
            }
        }
    }

    @Nested
    class ChangeParentCategory {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Category childCategory = Category.createCategory(null, "댄스");
                CategoryResponse childCategoryResponse = new CategoryResponse("댄스", Collections.emptyList());
                CategoryResponse categoryResponse = new CategoryResponse("가요", List.of(childCategoryResponse));
                CategoryChangeParentRequest request = new CategoryChangeParentRequest("가요");
                String json = objectMapper.writeValueAsString(request);

                given(categoryService.changeParentCategory(anyString(), any(CategoryChangeParentRequest.class))).willReturn(childCategory);
                given(categoryService.getCategoryDto(any(Category.class))).willReturn(categoryResponse);

                //when & then
                mvc.perform(post("/categories/{categoryName}", "댄스")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("가요"))
                        .andExpect(jsonPath("$.data.children[0].name").value("댄스"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryService).should().changeParentCategory("댄스", request));
                    softly.check(() -> BDDMockito.then(categoryService).should().getCategoryDto(any(Category.class)));
                });
            }
        }
    }
}