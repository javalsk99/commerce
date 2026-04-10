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

    protected Optional<CategoryQueryDto> findCategory(String categoryName) {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.CategoryQueryDto(c.name)" +
                                " from Category c" +
                                " where c.name = :name", CategoryQueryDto.class)
                .setParameter("name", categoryName)
                .getResultList()
                .stream()
                .findFirst();
    }
}
