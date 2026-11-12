package com.pizzaplatform.api.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * A pizza as described on the menu. Ordering it creates a new {@link Pizza} instance.
 *
 * @param name        name displayed on the menu
 * @param base        sauce of the pizza
 * @param ingredients toppings of the pizza
 */
public record PizzaTemplate(String name, PizzaBase base, List<Ingredient> ingredients) {

    public PizzaTemplate {
        ingredients = List.copyOf(ingredients);
    }

    /**
     * @return base price plus the price of every ingredient
     */
    public BigDecimal price() {
        return ingredients.stream()
                .map(Ingredient::price)
                .reduce(base.getPrice(), BigDecimal::add);
    }

    /**
     * @return a new pizza, ready to be added to an order
     */
    public Pizza toPizza() {
        return new Pizza(name, base, ingredients);
    }
}
