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
                                " left join fetch cp.category" +
                                " where p.name = :name", Product.class)
                .setParameter("name", productName)
                .getResultStream()
                .findFirst();
    }

    public void delete(Product product) {
        em.remove(product);
    }

    public boolean existsAlbum(String name, String artist, String studio) {
        return em.createQuery(
                        "select count(a) > 0 from Album a" +
                                " where a.name = :name" +
                                " and a.artist = :artist" +
                                " and a.studio = :studio", Boolean.class)
                .setParameter("name", name)
                .setParameter("artist", artist)
                .setParameter("studio", studio)
                .getSingleResult();
    }

    public boolean existsBook(String name, String author, String isbn) {
        return em.createQuery(
                        "select count(b) > 0 from Book b" +
                                " where b.name = :name" +
                                " and b.author = :author" +
                                " and b.isbn = :isbn", Boolean.class)
                .setParameter("name", name)
                .setParameter("author", author)
                .setParameter("isbn", isbn)
                .getSingleResult();
    }

    public boolean existsMovie(String name, String actor, String director) {
        return em.createQuery(
                        "select count(m) > 0 from Movie m" +
                                " where m.name = :name" +
                                " and m.actor = :actor" +
                                " and m.director = :director", Boolean.class)
                .setParameter("name", name)
                .setParameter("actor", actor)
                .setParameter("director", director)
                .getSingleResult();
    }
}
