package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.CategoryQueryDto;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryQueryRepository {

    private final EntityManager em;

    protected Optional<CategoryQueryDto> findCategory(String categoryNumber) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.CategoryQueryDto(c.name, c.categoryNumber)" +
                                " from Category c" +
                                " where c.categoryNumber = :categoryNumber", CategoryQueryDto.class)
                .setParameter("categoryNumber", categoryNumber)
                .getResultList()
                .stream()
                .findFirst();
    }
}
