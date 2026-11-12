package com.pizzaplatform.api.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;

/**
 * Payloads of the authentication endpoints.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @JacksonXmlRootElement(localName = "credentials")
    public record LoginRequest(@NotBlank String login, @NotBlank String password) { }

    /**
     * @param token     the JWT to send in the {@code Authorization: Bearer} header
     * @param tokenType always "Bearer"
     * @param expiresIn token lifetime in seconds
     */
    @JacksonXmlRootElement(localName = "token")
    public record TokenResponse(String token, String tokenType, long expiresIn) {
        public TokenResponse(String token, long expiresIn) {
            this(token, "Bearer", expiresIn);
        }
    }
}
