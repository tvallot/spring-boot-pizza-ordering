package com.pizzaplatform.api.controller;

import com.pizzaplatform.api.config.OpenApiConfig;
import com.pizzaplatform.api.dto.AuthDtos.LoginRequest;
import com.pizzaplatform.api.dto.AuthDtos.TokenResponse;
import com.pizzaplatform.api.model.Customer;
import com.pizzaplatform.api.security.JwtAuthenticationFilter;
import com.pizzaplatform.api.security.JwtService;
import com.pizzaplatform.api.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final CustomerService customerService;
    private final JwtService jwtService;

    public AuthController(CustomerService customerService, JwtService jwtService) {
        this.customerService = customerService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a JWT (also returned in the Authorization header)")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerService.authenticate(request.login(), request.password());
        String token = jwtService.generateToken(customer);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new TokenResponse(token, jwtService.getExpiration().toSeconds()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current token")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        JwtAuthenticationFilter.extractToken(request).ifPresent(jwtService::revoke);
        return ResponseEntity.noContent().build();
    }
}
