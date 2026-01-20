package lsk.commerce.api.portone;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("portone.secret")
public record PortoneSecretProperties(String api, String webhook) {
}
