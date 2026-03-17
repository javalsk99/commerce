package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.query.ProductQueryService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                ProductRequest request = ProductRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(productService.register(any(ProductRequest.class), anyList())).willReturn("BANG BANG");

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
            @MethodSource("invalidProductRequestCategoryNamesProvider")
            void invalidInput(ProductRequest request, String categoryName1, String categoryName2) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .param("categoryNames", categoryName1)
                                .param("categoryNames", categoryName2))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                BDDMockito.then(productService).should(never()).register(any(), any());
            }

            @Test
            void register_Failed_DuplicateProduct() throws Exception {
                //given
                ProductRequest request = ProductRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                String json = objectMapper.writeValueAsString(request);

                given(productService.register(any(ProductRequest.class), anyList())).willThrow(new IllegalArgumentException("이미 존재하는 상품입니다"));

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

            static Stream<Arguments> invalidProductRequestCategoryNamesProvider() {
                return Stream.of(
                        argumentSet("productName null",
                                ProductRequest.builder()
                                        .price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("price null",
                                ProductRequest.builder()
                                        .name("BANG BANG").stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("stockQuantity null",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("dtype null",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("dtype 빈 문자열",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("dtype 공백",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype(" ").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("dtype 1자 초과",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("a".repeat(2)).artist("IVE").studio("STARSHIP")
                                        .build(),
                                "가요", "댄스"
                        ),
                        argumentSet("categoryName null",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                null, "댄스"
                        ),
                        argumentSet("categoryName 빈 문자열",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "", "댄스"
                        ),
                        argumentSet("categoryName 공백",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                " ", "댄스"
                        ),
                        argumentSet("categoryName 20자 초과",
                                ProductRequest.builder()
                                        .name("BANG BANG").price(15000).stockQuantity(10)
                                        .dtype("A").artist("IVE").studio("STARSHIP")
                                        .build(),
                                "a".repeat(21), "댄스"
                        )
                );
            }
        }
    }
}