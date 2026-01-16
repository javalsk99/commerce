package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;

import static jakarta.persistence.EnumType.*;

@Entity
@Getter
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @Embedded
    private Address address;

    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;
}
