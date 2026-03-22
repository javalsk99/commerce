package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@Transactional
@SpringBootTest
public class CategoryIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    CategoryService categoryService;

    @Nested
    class Create {

        @Nested
        class FailureCase {

            @Test
            @DisplayName("부모 카테고리의 이름이 달라도 같은 이름으로 생성할 수 없다")
            void duplicateCreate() {
                //given
                String parentName1 = categoryService.create(new CategoryCreateRequest("가요", null));
                String parentName2 = categoryService.create(new CategoryCreateRequest("컴퓨터/IT", null));

                categoryService.create(new CategoryCreateRequest("댄스", parentName1));

                em.flush();
                em.clear();

                CategoryCreateRequest request = new CategoryCreateRequest("댄스", parentName2);

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    categoryService.create(request);
                    em.flush();
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 존재하는 카테고리입니다. name: " + "댄스");

                System.out.println("================= WHEN END ===================");
            }
        }
    }
}
