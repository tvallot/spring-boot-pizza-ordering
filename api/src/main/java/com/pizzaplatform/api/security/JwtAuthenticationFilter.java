package com.pizzaplatform.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates API requests with the {@code Authorization: Bearer <token>} header.
 * <p>
 * Public routes (menu, registration, login, static web client, API docs) are let through.
 * For every other {@code /customers/**}, {@code /orders/**} and {@code /auth/**} route, a valid token is required
 * and the authenticated login is exposed to controllers as the {@link #AUTHENTICATED_LOGIN} request attribute.
 * Ownership checks (403) are done in the service layer.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute holding the login of the authenticated customer. */
    public static final String AUTHENTICATED_LOGIN = "authenticatedLogin";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!requiresAuthentication(request)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<String> login = extractToken(request).flatMap(jwtService::validate);
        if (login.isEmpty()) {
            rejectUnauthenticated(request, response);
            return;
        }

        request.setAttribute(AUTHENTICATED_LOGIN, login.get());
        chain.doFilter(request, response);
    }

    /**
     * @param request an HTTP request
     * @return the raw token of the Authorization header, if any
     */
    public static Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }

    private static boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        if (HttpMethod.OPTIONS.matches(method)) {
            return false;
        }
        boolean isApiRoute = path.startsWith("/customers") || path.startsWith("/orders") || path.startsWith("/auth");
        if (!isApiRoute) {
            return false; // menu, web client, Swagger UI
        }
        boolean isRegistration = path.equals("/customers") && HttpMethod.POST.matches(method);
        boolean isCustomerList = path.equals("/customers") && HttpMethod.GET.matches(method);
        boolean isLogin = path.equals("/auth/login") && HttpMethod.POST.matches(method);
        return !(isRegistration || isCustomerList || isLogin);
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "A valid bearer token is required. Obtain one with POST /auth/login.");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
