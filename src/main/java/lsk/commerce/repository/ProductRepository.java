package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.product.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepository {

    private final EntityManager em;

    public void save(Product product) {
        em.persist(product);
    }

    public Product findOne(Long productId) {
        return em.find(Product.class, productId);
    }

    public List<Product> findAll() {
        return em.createQuery("select p from Product p", Product.class)
                .getResultList();
    }

    public void delete(Product product) {
        em.remove(product);
    }
}
