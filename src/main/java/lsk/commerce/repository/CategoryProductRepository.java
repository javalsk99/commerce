package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.CategoryProduct;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryProductRepository {

    private final EntityManager em;

    public void delete(CategoryProduct categoryProduct) {
        em.remove(categoryProduct);
    }
}
