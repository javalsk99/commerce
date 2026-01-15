package Java.lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.*;

@Entity
@Getter
public class Payment {

    @Id @GeneratedValue
    @Column(name = "payment_id")
    private Long id;

    private int paymentAmount;

    private LocalDateTime paymentDate;

    @Enumerated(STRING)
    private PaymentStatus paymentStatus;
}
