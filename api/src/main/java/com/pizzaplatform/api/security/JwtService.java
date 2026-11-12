package com.pizzaplatform.api.security;

import com.pizzaplatform.api.config.AppProperties;
import com.pizzaplatform.api.model.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues, validates and revokes the JSON Web Tokens used to authenticate customers.
 * <p>
 * Tokens are stateless, so logging out adds the token identifier ({@code jti}) to a deny-list
 * until the token would have expired anyway.
 */
@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "pizza-api";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public JwtService(AppProperties properties, Clock clock) {
        this.expiration = properties.jwt().expiration();
        this.clock = clock;
        String secret = properties.jwt().secret();
        if (secret.isBlank()) {
            LOGGER.warn("No app.jwt.secret configured: using a random key, tokens will not survive a restart.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
    }

    /**
     * @param customer the authenticated customer
     * @return a signed token whose subject is the customer login
     */
    public String generateToken(Customer customer) {
        Instant now = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(customer.getLogin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Duration getExpiration() {
        return expiration;
    }

    /**
     * @param token a compact JWT
     * @return the customer login if the token is valid, correctly signed, not expired and not revoked
     */
    public Optional<String> validate(String token) {
        return parse(token)
                .filter(claims -> !revokedTokens.containsKey(claims.getId()))
                .map(Claims::getSubject);
    }

    /**
     * Revokes a token until its expiration date.
     *
     * @param token a compact JWT
     */
    public void revoke(String token) {
        parse(token).ifPresent(claims -> revokedTokens.put(claims.getId(), claims.getExpiration().toInstant()));
        Instant now = clock.instant();
        revokedTokens.values().removeIf(expiry -> expiry.isBefore(now));
    }

    private Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
