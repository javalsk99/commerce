package lsk.commerce.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    public Optional<Product> findByName(String productName) {
        return em.createQuery("select p from Product p where p.name = :name", Product.class)
                .setParameter("name", productName)
                .getResultStream()
                .findFirst();
    }

    public Optional<Product> findWithCategoryProduct(String productName) {
        return em.createQuery(
                        "select p from Product p" +
                                " left join fetch p.categoryProducts" +
                                " where p.name = :name", Product.class)
                .setParameter("name", productName)
                .getResultStream()
                .findFirst();
    }

    public Optional<Product> findWithCategoryProductCategory(String productName) {
        return em.createQuery(
                        "select p from Product p" +
                                " left join fetch p.categoryProducts cp" +
                                " left join fetch cp.category c" +
                                " where p.name = :name", Product.class)
                .setParameter("name", productName)
                .getResultStream()
                .findFirst();
    }

    public void delete(Product product) {
        em.remove(product);
    }
}
