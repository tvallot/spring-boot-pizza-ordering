package com.pizzaplatform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Type-safe application settings, bound from the {@code app.*} keys of {@code application.properties}.
 *
 * @param jwt     token signing and lifetime settings
 * @param kitchen simulated kitchen settings
 * @param cors    origins allowed to call the API from another domain
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Kitchen kitchen, Cors cors) {

    /**
     * @param secret     Base64-encoded HMAC key (at least 256 bits). When blank, a random key is generated at startup.
     * @param expiration token lifetime
     */
    public record Jwt(@DefaultValue("") String secret, @DefaultValue("1h") Duration expiration) { }

    /**
     * @param preparationTime time needed to bake a pizza once the order has been paid
     */
    public record Kitchen(@DefaultValue("20s") Duration preparationTime) { }

    /**
     * @param allowedOrigins origins allowed by the CORS policy
     */
    public record Cors(@DefaultValue List<String> allowedOrigins) { }
}
