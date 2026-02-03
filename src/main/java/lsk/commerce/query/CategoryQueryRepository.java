package lsk.commerce.query;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.query.dto.CategoryQueryDto;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.util.stream.Collectors.*;

@Repository
@RequiredArgsConstructor
public class CategoryQueryRepository {

    private final EntityManager em;



    protected static List<String> toCategoryNames(List<CategoryQueryDto> result) {
        return result.stream()
                .map(c -> c.getName())
                .collect(toList());
    }

    protected List<CategoryQueryDto> findCategories() {
        return em.createQuery(
                        "select new lsk.commerce.query.dto.CategoryQueryDto(c.name)" +
                                " from Category c", CategoryQueryDto.class)
                .getResultList();
    }
}
