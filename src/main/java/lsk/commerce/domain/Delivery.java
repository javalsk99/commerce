package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.DeliveryStatus.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @Embedded
    private Address address;

    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;

    public Delivery(Member member) {
        this.address = member.getAddress();
        this.deliveryStatus = PREPARING;
    }

    public Delivery(Address address) {
        this.address = address;
        this.deliveryStatus = PREPARING;
    }
}
