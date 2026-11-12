package com.pizzaplatform.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata. The interactive documentation is available at {@code /swagger-ui.html}.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Pizza Ordering API",
        version = "1.0.0",
        description = "Register, log in, browse the menu, place and pay for pizza orders. "
                + "Protected endpoints expect a JWT obtained from POST /auth/login."))
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    /** Name of the security scheme referenced by protected controllers. */
    public static final String BEARER_AUTH = "bearerAuth";
}
