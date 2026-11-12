package com.pizzaplatform.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration: content negotiation and CORS.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * JSON is the default representation; XML is returned when the client explicitly asks for it
     * with {@code Accept: application/xml}.
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
    }

    /**
     * Allows the web client to be hosted on another origin (e.g. a static file server during development).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT,
                        HttpHeaders.IF_NONE_MATCH, HttpHeaders.IF_MODIFIED_SINCE)
                .exposedHeaders(HttpHeaders.AUTHORIZATION, HttpHeaders.LOCATION, HttpHeaders.ETAG,
                        HttpHeaders.LAST_MODIFIED)
                .maxAge(3600);
    }
}
