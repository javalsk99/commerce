package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.util.InitialExtractor;

import static lombok.AccessLevel.PUBLIC;

@Entity
@DiscriminatorValue("M")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Movie extends Product {

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String actor;

    private String actorInitial;

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String director;

    private String directorInitial;

    public Movie(String name, Integer price, Integer stockQuantity, String actor, String director) {
        super(name, price, stockQuantity);
        this.actor = actor;
        this.director = director;
    }

    @PrePersist @PreUpdate
    private void preHandler() {
        this.actorInitial = InitialExtractor.extract(this.actor);
        this.directorInitial = InitialExtractor.extract(this.director);
    }
}
