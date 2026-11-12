package com.pizzaplatform.api.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

/**
 * The sauce spread on the dough. Each base has its own starting price.
 */
public enum PizzaBase {
    TOMATO(new BigDecimal("2.00")),
    CREAM(new BigDecimal("3.00"));

    private final BigDecimal price;

    PizzaBase(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Case-insensitive lookup, used when reading the menu files.
     *
     * @param value "tomato" or "cream"
     * @return the matching base
     * @throws IllegalArgumentException if the value is not a known base
     */
    public static PizzaBase parse(String value) {
        return Arrays.stream(values())
                .filter(base -> base.name().equals(value.trim().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown pizza base: " + value));
    }
}
