package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
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

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣 ,]{1,50}$", message = "배우는 한글, 영문, 공백, (,)만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String actor;

    @Column(length = 50)
    private String actorInitial;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣]{1,50}$", message = "감독은 한글, 영문만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String director;

    @Column(length = 50)
    private String directorInitial;

    @Builder
    public Movie(String name, Integer price, Integer stockQuantity, String actor, String director) {
        super(name, price, stockQuantity);
        this.actor = actor;
        this.director = director;
    }

    @Override
    protected void preHandler() {
        super.preHandler();
        this.actorInitial = InitialExtractor.extract(this.actor);
        this.directorInitial = InitialExtractor.extract(this.director);
    }
}
