package lsk.commerce;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.repository.MemberRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.ProductService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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

        private final JdbcTemplate jdbcTemplate;
        private final PasswordEncoder passwordEncoder;
        private final MemberRepository memberRepository;
        private final MemberService memberService;
        private final CategoryService categoryService;
        private final ProductService productService;
        private final OrderService orderService;

        public void dbInit() {
            createOrders();
        }

        private void createOrders() {
            createAdmin();
            String userAId = memberService.join(createMemberRequest("userA", "userAId", "userAPassword"));
            String userBId = memberService.join(createMemberRequest("userB", "userBId", "userBPassword"));
            memberService.join(createMemberRequest("userC", "userCId", "userCPassword"));

            categoryService.create(new CategoryCreateRequest("가요", null));
            String albumCategoryName2 = categoryService.create(new CategoryCreateRequest("댄스", "가요"));
            String albumCategoryName3 = categoryService.create(new CategoryCreateRequest("발라드", "가요"));
            categoryService.create(new CategoryCreateRequest("컴퓨터/IT", null));
            categoryService.create(new CategoryCreateRequest("프로그래밍 언어", "컴퓨터/IT"));
            String bookCategoryName3 = categoryService.create(new CategoryCreateRequest("Java", "프로그래밍 언어"));
            String bookCategoryName4 = categoryService.create(new CategoryCreateRequest("Python", "프로그래밍 언어"));
            String bookCategoryName5 = categoryService.create(new CategoryCreateRequest("프로그래밍 일반", "프로그래밍 언어"));
            categoryService.create(new CategoryCreateRequest("국내 영화", null));
            String movieCategoryName2 = categoryService.create(new CategoryCreateRequest("액션 영화", "국내 영화"));
            String movieCategoryName3 = categoryService.create(new CategoryCreateRequest("코미디 영화", "국내 영화"));

            String albumNumber1 = productService.register(ProductCreateRequest.builder().name("BANG BANG").price(15000).stockQuantity(100).dtype("A").artist("IVE").studio("STARSHIP").build(), List.of(albumCategoryName2));
            productService.register(ProductCreateRequest.builder().name("Blue Valentine").price(15000).stockQuantity(100).dtype("A").artist("NMIXX").studio("JYP").build(), List.of(albumCategoryName2));
            productService.register(ProductCreateRequest.builder().name("404").price(15000).stockQuantity(100).dtype("A").artist("KiiiKiii").studio("STARSHIP").build(), List.of(albumCategoryName2));
            String albumNumber4 = productService.register(ProductCreateRequest.builder().name("타임 캡슐").price(15000).stockQuantity(100).dtype("A").artist("다비치").studio("씨에이엠위더스").build(), List.of(albumCategoryName3));
            productService.register(ProductCreateRequest.builder().name("너의 모든 순간").price(15000).stockQuantity(100).dtype("A").artist("성시경").studio("에스케이재원").build(), List.of(albumCategoryName3));
            String albumNumber6 = productService.register(ProductCreateRequest.builder().name("천상연").price(15000).stockQuantity(100).dtype("A").artist("이창섭").studio("판타지오").build(), List.of(albumCategoryName3));
            String bookNumber1 = productService.register(ProductCreateRequest.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(100).dtype("B").author("김영한").isbn("9788960777330").build(), List.of(bookCategoryName3, bookCategoryName5));
            String bookNumber2 = productService.register(ProductCreateRequest.builder().name("면접을 위한 CS 전공지식 노트").price(15000).stockQuantity(100).dtype("B").author("주홍철").isbn("9791165219529").build(), List.of(bookCategoryName5));
            productService.register(ProductCreateRequest.builder().name("Do it! 점프 투 파이썬").price(15000).stockQuantity(100).dtype("B").author("박응용").isbn("9791163034735").build(), List.of(bookCategoryName4));
            String movieNumber1 = productService.register(ProductCreateRequest.builder().name("범죄도시").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("강윤성").build(), List.of(movieCategoryName2, movieCategoryName3));
            String movieNumber2 = productService.register(ProductCreateRequest.builder().name("범죄도시2").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("이상용").build(), List.of(movieCategoryName2, movieCategoryName3));
            productService.register(ProductCreateRequest.builder().name("범죄도시3").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("이상용").build(), List.of(movieCategoryName2, movieCategoryName3));
            productService.register(ProductCreateRequest.builder().name("범죄도시4").price(15000).stockQuantity(100).dtype("M").actor("마동석").director("허명행").build(), List.of(movieCategoryName2, movieCategoryName3));

            orderService.order(new OrderCreateRequest(Map.of(albumNumber1, 3, albumNumber4, 2, albumNumber6, 5)), userAId);
            orderService.order(new OrderCreateRequest(Map.of(albumNumber1, 2, bookNumber1, 4, movieNumber2, 3)), userAId);
            orderService.order(new OrderCreateRequest(Map.of(albumNumber1, 3, albumNumber4, 2, albumNumber6, 5)), userAId);
            orderService.order(new OrderCreateRequest(Map.of(bookNumber1, 1, bookNumber2, 4, movieNumber1, 3, movieNumber2, 2)), userBId);
            orderService.order(new OrderCreateRequest(Map.of(movieNumber1, 5)), userBId);
        }

        private void createAdmin() {
            Member admin = Member.builder()
                    .name("test")
                    .loginId("testId")
                    .password(passwordEncoder.encode("testPassword"))
                    .city("Seoul")
                    .street("Gangbuk")
                    .zipcode("11111")
                    .build();
            memberRepository.save(admin);

            String sql = "UPDATE member SET grade = 'ADMIN' WHERE name = ?";
            jdbcTemplate.update(sql, admin.getName());
        }

        private static MemberCreateRequest createMemberRequest(String name, String loginId, String password) {
            return MemberCreateRequest.builder()
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
