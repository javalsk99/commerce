package lsk.commerce.api.portone;

import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortoneConfig {

    @Bean
    public PaymentClient paymentClient(PortoneSecretProperties secret) {
        return new PaymentClient(secret.api(), "https://api.portone.io", "store-3218fbd8-7af7-4043-8a4e-ec6e84fd858c");
    }

    @Bean
    public WebhookVerifier webhookVerifier(PortoneSecretProperties secret) {
        return new WebhookVerifier(secret.webhook());
    }
}
