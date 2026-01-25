package lsk.commerce.api.portone;

public final class CompletePaymentRequest {
    public String paymentId;

    public CompletePaymentRequest(String paymentId) {
        this.paymentId = paymentId;
    }
}
