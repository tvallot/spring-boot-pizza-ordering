package com.pizzaplatform.api.model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A pizza belonging to an order.
 * Its preparation starts when the order is paid and it becomes ready once the preparation time has elapsed.
 */
public class Pizza {

    private final String name;
    private final PizzaBase base;
    private final List<Ingredient> ingredients;
    private Instant readyAt;

    public Pizza(String name, PizzaBase base, List<Ingredient> ingredients) {
        this.name = name;
        this.base = base;
        this.ingredients = List.copyOf(ingredients);
    }

    public String getName() {
        return name;
    }

    public PizzaBase getBase() {
        return base;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public BigDecimal getPrice() {
        return ingredients.stream()
                .map(Ingredient::price)
                .reduce(base.getPrice(), BigDecimal::add);
    }

    /**
     * Sends the pizza to the oven.
     *
     * @param clock           time source
     * @param preparationTime time needed before the pizza is ready
     */
    void startPreparation(Clock clock, Duration preparationTime) {
        this.readyAt = clock.instant().plus(preparationTime);
    }

    /**
     * @param clock time source
     * @return true if the preparation has started and is over
     */
    public boolean isReady(Clock clock) {
        return readyAt != null && !clock.instant().isBefore(readyAt);
    }

    /**
     * @return the instant at which the pizza is (or will be) ready, empty if its preparation has not started
     */
    Optional<Instant> getReadyAt() {
        return Optional.ofNullable(readyAt);
    }
}
