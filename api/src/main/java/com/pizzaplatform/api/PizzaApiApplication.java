package com.pizzaplatform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the pizza ordering platform.
 * Serves both the REST API and the bundled single-page web client.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PizzaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PizzaApiApplication.class, args);
    }
}
