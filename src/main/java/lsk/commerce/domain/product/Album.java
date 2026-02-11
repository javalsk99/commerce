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
@DiscriminatorValue("A")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Album extends Product {

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String artist;

    private String artistInitial;

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String studio;

    private String studioInitial;

    public Album(String name, Integer price, Integer stockQuantity, String artist, String studio) {
        super(name, price, stockQuantity);
        this.artist = artist;
        this.studio = studio;
    }

    @PrePersist @PreUpdate
    private void preHandler() {
        this.artistInitial = InitialExtractor.extract(this.artist);
        this.studioInitial = InitialExtractor.extract(this.studio);
    }
}
