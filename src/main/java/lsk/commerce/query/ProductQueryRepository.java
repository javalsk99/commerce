package lsk.commerce.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.domain.product.QAlbum;
import lsk.commerce.domain.product.QBook;
import lsk.commerce.domain.product.QMovie;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.QProductResponse;
import lsk.commerce.query.dto.ProductSearchCond;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static lsk.commerce.domain.QCategory.category;
import static lsk.commerce.domain.QCategoryProduct.categoryProduct;
import static lsk.commerce.domain.QProduct.product;

@Repository
public class ProductQueryRepository {

    private final JPAQueryFactory query;

    private final QAlbum album = product.as(QAlbum.class);
    private final QBook book = product.as(QBook.class);
    private final QMovie movie = product.as(QMovie.class);

    public ProductQueryRepository(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    protected List<ProductResponse> search(ProductSearchCond cond) {
        return query.select(new QProductResponse(product.name, product.price, product.stockQuantity,
                        new CaseBuilder()
                                .when(product.instanceOf(Album.class)).then("A")
                                .when(product.instanceOf(Book.class)).then("B")
                                .when(product.instanceOf(Movie.class)).then("M")
                                .otherwise(Expressions.nullExpression(String.class)),
                        album.artist, album.studio, book.author, book.isbn, movie.actor, movie.director))
                .from(product)
                .where(
                        eqCategoryName(cond.getCategoryName()),
                        containsProductName(cond.getProductName()),
                        minPrice(cond.getMinPrice()),
                        maxPrice(cond.getMaxPrice()),
                        containsArtist(cond.getArtist()),
                        containsStudio(cond.getStudio()),
                        containsAuthor(cond.getAuthor()),
                        containsIsbn(cond.getIsbn()),
                        containsActor(cond.getActor()),
                        containsDirector(cond.getDirector())
                )
                .fetch();
    }

    private BooleanExpression eqCategoryName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            return null;
        }

        return JPAExpressions.select(categoryProduct)
                .from(categoryProduct)
                .join(categoryProduct.category, category)
                .where(
                        categoryProduct.product.eq(product),
                        category.name.eq(categoryName)
                )
                .exists();
    }

    private BooleanExpression containsProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return null;
        }

        if (productName.matches("^[ㄱ-ㅎ]+$")) {
            return product.nameInitial.contains(productName);
        }

        return product.name.containsIgnoreCase(productName);
    }

    private BooleanExpression minPrice(Integer minPrice) {
        if (minPrice == null) {
            return null;
        }

        return product.price.goe(minPrice);
    }

    private BooleanExpression maxPrice(Integer maxPrice) {
        if (maxPrice == null) {
            return null;
        }

        return product.price.loe(maxPrice);
    }

    private BooleanExpression containsArtist(String artist) {
        if (!StringUtils.hasText(artist)) {
            return null;
        }

        if (artist.matches("^[ㄱ-ㅎ]+$")) {
            return album.artistInitial.contains(artist);
        }

        return album.artist.containsIgnoreCase(artist);
    }

    private BooleanExpression containsStudio(String studio) {
        if (!StringUtils.hasText(studio)) {
            return null;
        }

        if (studio.matches("^[ㄱ-ㅎ]+$")) {
            return album.studioInitial.contains(studio);
        }

        return album.studio.containsIgnoreCase(studio);
    }

    private BooleanExpression containsAuthor(String author) {
        if (!StringUtils.hasText(author)) {
            return null;
        }

        if (author.matches("^[ㄱ-ㅎ]+$")) {
            return book.authorInitial.contains(author);
        }

        return book.author.containsIgnoreCase(author);
    }

    private BooleanExpression containsIsbn(String isbn) {
        if (!StringUtils.hasText(isbn)) {
            return null;
        }

        return book.isbn.containsIgnoreCase(isbn);
    }

    private BooleanExpression containsActor(String actor) {
        if (!StringUtils.hasText(actor)) {
            return null;
        }

        if (actor.matches("^[ㄱ-ㅎ]+$")) {
            return movie.actorInitial.contains(actor);
        }

        return movie.actor.containsIgnoreCase(actor);
    }

    private BooleanExpression containsDirector(String director) {
        if (!StringUtils.hasText(director)) {
            return null;
        }

        if (director.matches("^[ㄱ-ㅎ]+$")) {
            return movie.directorInitial.contains(director);
        }

        return movie.director.containsIgnoreCase(director);
    }
}
