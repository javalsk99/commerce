package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.CategoryProductQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryProductQueryRepository {

    private final EntityManager em;

    protected List<CategoryProductQueryDto> findCategoryProductsByCategoryName(String categoryNumber) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.CategoryProductQueryDto(cp.category.categoryNumber, p.name, p.productNumber)" +
                                " from CategoryProduct cp" +
                                " join cp.product p" +
                                " where cp.category.categoryNumber = :categoryNumber", CategoryProductQueryDto.class)
                .setParameter("categoryNumber", categoryNumber)
                .getResultList();
    }
}
