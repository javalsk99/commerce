package lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
@DiscriminatorValue("A")
@Getter
public class Album extends Product{

    private String artist;
    private String studio;
}
