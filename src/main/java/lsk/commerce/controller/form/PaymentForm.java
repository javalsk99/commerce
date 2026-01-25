package lsk.commerce.controller.form;

import lombok.Getter;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;

@Getter
public class PaymentForm {

    private String paymentId;
    private PaymentStatus paymentStatus;

    public PaymentForm(String paymentId, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
    }

    public static PaymentForm paymentChangeForm(Payment payment) {
        return new PaymentForm(payment.getPaymentId(), payment.getPaymentStatus());
    }
}
