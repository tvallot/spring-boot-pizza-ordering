package com.pizzaplatform.api.model;

import java.math.BigDecimal;

/**
 * An ingredient that can be put on a pizza.
 *
 * @param name  unique name of the ingredient
 * @param price extra cost of the ingredient
 */
public record Ingredient(String name, BigDecimal price) {
}
