package lsk.commerce;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.ProductService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final MemberService memberService;
        private final CategoryService categoryService;
        private final ProductService productService;
        private final OrderService orderService;

        public void dbInit() {
            createOrders();
        }

        private void createOrders() {
            memberService.adminJoin(createMemberRequest("test", "testId", "testPassword"));
            String userAId = memberService.join(createMemberRequest("userA", "userAId", "userAPassword"));
            String userBId = memberService.join(createMemberRequest("userB", "userBId", "userBPassword"));
            String userCId = memberService.join(createMemberRequest("userC", "userCId", "userCPassword"));

            categoryService.create(new CategoryRequest("가요", null));
            String albumCategoryName2 = categoryService.create(new CategoryRequest("댄스", "가요"));
            String albumCategoryName3 = categoryService.create(new CategoryRequest("발라드", "가요"));
            categoryService.create(new CategoryRequest("컴퓨터/IT", null));
            categoryService.create(new CategoryRequest("프로그래밍 언어", "컴퓨터/IT"));
            String bookCategoryName3 = categoryService.create(new CategoryRequest("Java", "프로그래밍 언어"));
            String bookCategoryName4 = categoryService.create(new CategoryRequest("Python", "프로그래밍 언어"));
            String bookCategoryName5 = categoryService.create(new CategoryRequest("프로그래밍 일반", "프로그래밍 언어"));
            categoryService.create(new CategoryRequest("국내 영화", null));
            String movieCategoryName2 = categoryService.create(new CategoryRequest("액션 영화", "국내 영화"));
            String movieCategoryName3 = categoryService.create(new CategoryRequest("코미디 영화", "국내 영화"));

            String albumName1 = productService.register(ProductRequest.builder().name("BANG BANG").price(15000).stockQuantity(100).dtype("A").artist("IVE").studio("STARSHIP").build(), List.of(albumCategoryName2));
            productService.register(ProductRequest.builder().name("Blue Valentine").price(15000).stockQuantity(100).dtype("A").artist("NMIXX").studio("JYP").build(), List.of(albumCategoryName2));
            productService.register(ProductRequest.builder().name("404").price(15000).stockQuantity(100).dtype("A").artist("KiiiKiii").studio("STARSHIP").build(), List.of(albumCategoryName2));
            String albumName4 = productService.register(ProductRequest.builder().name("타임 캡슐").price(15000).stockQuantity(100).dtype("A").artist("다비치").studio("씨에이엠위더스").build(), List.of(albumCategoryName3));
            productService.register(ProductRequest.builder().name("너의 모든 순간").price(15000).stockQuantity(100).dtype("A").artist("성시경").studio("에스케이재원").build(), List.of(albumCategoryName3));
            String albumName6 = productService.register(ProductRequest.builder().name("천상연").price(15000).stockQuantity(100).dtype("A").artist("이창섭").studio("판타지오").build(), List.of(albumCategoryName3));
            String bookName1 = productService.register(ProductRequest.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(100).dtype("B").author("김영한").isbn("9788960777330").build(), List.of(bookCategoryName3, bookCategoryName5));
            String bookName2 = productService.register(ProductRequest.builder().name("면접을 위한 CS 전공지식 노트").price(15000).stockQuantity(100).dtype("B").author("주홍철").isbn("9791165219529").build(), List.of(bookCategoryName5));
            productService.register(ProductRequest.builder().name("Do it! 점프 투 파이썬").price(15000).stockQuantity(100).dtype("B").author("박응용").isbn("9791163034735").build(), List.of(bookCategoryName4));
            String movieName1 = productService.register(ProductRequest.builder().name("범죄도시").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("강윤성").build(), List.of(movieCategoryName2, movieCategoryName3));
            String movieName2 = productService.register(ProductRequest.builder().name("범죄도시2").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("이상용").build(), List.of(movieCategoryName2, movieCategoryName3));
            productService.register(ProductRequest.builder().name("범죄도시3").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("이상용").build(), List.of(movieCategoryName2, movieCategoryName3));
            productService.register(ProductRequest.builder().name("범죄도시4").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("허명행").build(), List.of(movieCategoryName2, movieCategoryName3));

            orderService.order(userAId, Map.of(albumName1, 3, albumName4, 2, albumName6, 5));
            orderService.order(userAId, Map.of(albumName1, 2, bookName1, 4, movieName2, 3));
            orderService.order(userAId, Map.of(albumName1, 3, albumName4, 2, albumName6, 5));
            orderService.order(userBId, Map.of(bookName1, 1, bookName2, 4, movieName1, 3, movieName2, 2));
            orderService.order(userBId, Map.of(movieName1, 5));
        }

        private static MemberRequest createMemberRequest(String name, String loginId, String password) {
            return MemberRequest.builder()
                    .name(name)
                    .loginId(loginId)
                    .password(password)
                    .city("Seoul")
                    .street("Gangbuk")
                    .zipcode("11111")
                    .build();
        }
    }
}
