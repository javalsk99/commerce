package lsk.commerce.query;

import lsk.commerce.config.QuerydslConfig;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Role;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.product.Album;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.query.dto.MemberQueryDto;
import lsk.commerce.query.dto.MemberSearchCond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        MemberQueryRepository.class,
        QuerydslConfig.class
})
class MemberQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    MemberQueryRepository memberQueryRepository;

    @BeforeEach
    void beforeEach() {
        Member member1 = Member.builder()
                .name("유저A")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
        Member member2 = Member.builder()
                .name("유저B")
                .loginId("id_B")
                .password("11111111")
                .city("Seoul")
                .street("Gangbuk")
                .zipcode("01235")
                .build();
        Member member3 = Member.builder()
                .name("유저C")
                .loginId("id_C")
                .password("22222222")
                .city("Seoul")
                .street("Gangdong")
                .zipcode("01236")
                .build();
        Delivery delivery = new Delivery(member1);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);

        Category category1 = Category.createCategory(null, "가요");
        Category category2 = Category.createCategory(category1, "댄스");
        Category category3 = Category.createCategory(category1, "발라드");
        em.persist(category1);
        em.persist(category2);
        em.persist(category3);

        Album album1 = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        Album album2 = Album.builder().name("타임 캡슐").price(15000).stockQuantity(10).artist("다비치").studio("씨에이엠위더스").build();
        em.persist(album1);
        em.persist(album2);
        album1.connectCategory(category2);
        album2.connectCategory(category3);

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album1, 5);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(album2, 4);
        List<OrderProduct> orderProducts = List.of(orderProduct1, orderProduct2);

        Order order = Order.createOrder(member1, delivery, orderProducts);
        em.persist(order);
        em.persist(orderProduct1);
        em.persist(orderProduct2);

        em.flush();
        em.clear();
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<MemberQueryDto> memberQueryDto = memberQueryRepository.findMember("id_A");

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(memberQueryDto).isPresent();
                    softly.then(memberQueryDto.get().orderQueryDtoList()).isEmpty();
                    softly.then(memberQueryDto.get())
                            .extracting("role", "loginId")
                            .containsExactly(Role.USER, "id_A");
                });
            }

            @Test
            void shouldReturnEmpty_WhenLoginIdDoesNotExist() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<MemberQueryDto> memberQueryDto = memberQueryRepository.findMember("id_D");

                System.out.println("================= WHEN END ===================");

                //then
                then(memberQueryDto).isEmpty();
            }
        }
    }

    @Nested
    class Search {

        @Nested
        class SuccessCase {

            @Test
            void shouldFindAll_WhenCondIsEmpty() {
                //given
                MemberSearchCond cond = new MemberSearchCond(null, null);

                System.out.println("================= WHEN START =================");

                //when
                List<MemberResponse> memberResponseList = memberQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(memberResponseList)
                        .extracting("loginId")
                        .containsExactlyInAnyOrder("id_A", "id_B", "id_C");
            }

            @ParameterizedTest
            @MethodSource("nameCondProvider")
            void shouldFilterByName_WhenNameIsPresent(MemberSearchCond cond, int size, List<String> loginIds) {
                assertThatContainsExactlyLoginIds(cond, size, loginIds);
            }

            @ParameterizedTest
            @MethodSource("loginIdCondProvider")
            void shouldFilterByLoginID_WhenLoginIdIsPresent(MemberSearchCond cond, int size, List<String> loginIds) {
                assertThatContainsExactlyLoginIds(cond, size, loginIds);
            }

            @ParameterizedTest
            @MethodSource("nameAndLoginIdCondProvider")
            void shouldFilterByAll_WhenNameAndLoginIdBothPresent(MemberSearchCond cond, int size, List<String> loginIds) {
                assertThatContainsExactlyLoginIds(cond, size, loginIds);
            }

            static Stream<Arguments> nameCondProvider() {
                return Stream.of(
                        argumentSet("이름을 유저로 검색", new MemberSearchCond("유저", null), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("이름을 ㅇㅈ으로 검색", new MemberSearchCond("ㅇㅈ", null), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("이름을 유저A로 검색", new MemberSearchCond("유저A", null), 1, List.of("id_A")),
                        argumentSet("이름을 유저a로 검색", new MemberSearchCond("유저a", null), 1, List.of("id_A")),
                        argumentSet("이름을 ㅇㅈA로 검색", new MemberSearchCond("ㅇㅈA", null), 1, List.of("id_A")),
                        argumentSet("이름을 ㅇㅈa로 검색", new MemberSearchCond("ㅇㅈa", null), 1, List.of("id_A")),
                        argumentSet("이름을 ㄱㄴㄷ으로 검색", new MemberSearchCond("ㄱㄴㄷ", null), 0, Collections.emptyList()),
                        argumentSet("이름을 ㅇa로 검색", new MemberSearchCond("ㅇa", null), 0, Collections.emptyList())
                );
            }

            static Stream<Arguments> loginIdCondProvider() {
                return Stream.of(
                        argumentSet("로그인 아이디를 id로 검색", new MemberSearchCond(null, "id"), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("로그인 아이디를 _로 검색", new MemberSearchCond(null, "_"), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("로그인 아이디를 ID로 검색", new MemberSearchCond(null, "ID"), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("로그인 아이디를 id_A로 검색", new MemberSearchCond(null, "id_A"), 1, List.of("id_A")),
                        argumentSet("로그인 아이디를 id_a로 검색", new MemberSearchCond(null, "id_a"), 1, List.of("id_A")),
                        argumentSet("로그인 아이디를 ab로 검색", new MemberSearchCond(null, "ab"), 0, Collections.emptyList())
                );
            }

            static Stream<Arguments> nameAndLoginIdCondProvider() {
                return Stream.of(
                        argumentSet("이름을 유저, 로그인 아이디를 id로 검색", new MemberSearchCond("유저", "id"), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("이름을 유저, 로그인 아이디를 a로 검색", new MemberSearchCond("유저", "a"), 1, List.of("id_A")),
                        argumentSet("이름을 a, 로그인 아이디를 id로 검색", new MemberSearchCond("a", "id"), 1, List.of("id_A")),
                        argumentSet("이름을 a, 로그인 아이디를 a로 검색", new MemberSearchCond("a", "a"), 1, List.of("id_A")),
                        argumentSet("이름을 a, 로그인 아이디를 b로 검색", new MemberSearchCond("a", "b"), 0, Collections.emptyList()),
                        argumentSet("이름을 ㄱㄴㄷ, 로그인 아이디를 id로 검색", new MemberSearchCond("ㄱㄴㄷ", "id"), 0, Collections.emptyList())
                );
            }

            private void assertThatContainsExactlyLoginIds(MemberSearchCond cond, int size, List<String> loginIds) {
                System.out.println("================= WHEN START =================");

                //when
                List<MemberResponse> memberResponseList = memberQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(memberResponseList)
                        .hasSize(size)
                        .extracting("loginId")
                        .containsExactlyInAnyOrderElementsOf(loginIds);
            }
        }
    }

    @Nested
    class ExtractLoginIds {

        @Nested
        class SuccessCase {

            @ParameterizedTest
            @MethodSource("memberQueryDtoListProvider")
            void basic(List<MemberQueryDto> memberQueryDtoList, int size, List<String> expectLoginIds) {
                System.out.println("================= WHEN START =================");

                //when
                List<String> loginIds = memberQueryRepository.extractLoginIds(memberQueryDtoList);

                System.out.println("================= WHEN END ===================");

                //then
                then(loginIds)
                        .hasSize(size)
                        .isEqualTo(expectLoginIds);
            }

            static Stream<Arguments> memberQueryDtoListProvider() {
                return Stream.of(
                        argumentSet("MemberQueryDto 3개", List.of(MemberQueryDto.builder().loginId("id_A").role(Role.USER).build(), MemberQueryDto.builder().loginId("id_B").role(Role.USER).build(), MemberQueryDto.builder().loginId("id_C").role(Role.USER).build()), 3, List.of("id_A", "id_B", "id_C")),
                        argumentSet("MemberQueryDto 2개", List.of(MemberQueryDto.builder().loginId("id_A").role(Role.USER).build(), MemberQueryDto.builder().loginId("id_B").role(Role.USER).build()), 2, List.of("id_A", "id_B")),
                        argumentSet("MemberQueryDto 1개", List.of(MemberQueryDto.builder().loginId("id_A").role(Role.USER).build()), 1, List.of("id_A")),
                        argumentSet("MemberQueryDto 0개", Collections.emptyList(), 0, Collections.emptyList()),
                        argumentSet("loginId가 없는 MemberQueryDto", List.of(MemberQueryDto.builder().loginId(null).role(Role.USER).build()), 0, Collections.emptyList())
                );
            }
        }
    }
}