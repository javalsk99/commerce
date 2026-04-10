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
@DiscriminatorValue("A")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Album extends Product {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣0-9 (),._]{1,50}$", message = "가수는 한글, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String artist;

    @Column(length = 50)
    private String artistInitial;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣0-9 ]{1,50}$", message = "기획사는 한글, 영문, 숫자, 공백만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String studio;

    @Column(length = 50)
    private String studioInitial;

    @Builder
    public Album(String name, Integer price, Integer stockQuantity, String artist, String studio) {
        super(name, price, stockQuantity);
        this.artist = artist;
        this.studio = studio;
    }

    @Override
    protected void preHandler() {
        super.preHandler();
        this.artistInitial = InitialExtractor.extract(this.artist);
        this.studioInitial = InitialExtractor.extract(this.studio);
    }
}
