package com.pizzaplatform.api.model;

import com.pizzaplatform.api.exception.Exceptions.ConflictException;
import com.pizzaplatform.api.exception.Exceptions.InsufficientPaymentException;
import com.pizzaplatform.api.exception.Exceptions.InvalidRequestException;
import com.pizzaplatform.api.exception.Exceptions.NotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An order placed by a customer.
 * <p>
 * Business rules enforced here (and unit-tested):
 * <ul>
 *     <li>an order can only be modified while it has not been paid;</li>
 *     <li>an empty order cannot be paid;</li>
 *     <li>the payment must cover the total price;</li>
 *     <li>once paid, every pizza goes to the oven and the order becomes ready when all of them are baked.</li>
 * </ul>
 * Methods are synchronized because the same order may be accessed by concurrent HTTP requests.
 */
public class Order {

    private final int id;
    private final Customer customer;
    private final Clock clock;
    private final List<Pizza> pizzas = new ArrayList<>();
    private boolean paid;
    private Instant lastModified;
    private long version;

    public Order(int id, Customer customer, Clock clock) {
        this.id = id;
        this.customer = customer;
        this.clock = clock;
        touch();
    }

    public int getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public synchronized List<Pizza> getPizzas() {
        return List.copyOf(pizzas);
    }

    public synchronized boolean isPaid() {
        return paid;
    }

    public synchronized BigDecimal getPrice() {
        return pizzas.stream().map(Pizza::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized void addPizza(Pizza pizza) {
        ensureEditable();
        pizzas.add(pizza);
        touch();
    }

    /**
     * Removes the first pizza with the given name.
     *
     * @param pizzaName name of the pizza to remove
     * @throws NotFoundException if the order contains no pizza with that name
     */
    public synchronized void removePizza(String pizzaName) {
        ensureEditable();
        Pizza pizza = pizzas.stream()
                .filter(p -> p.getName().equals(pizzaName))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Order " + id + " contains no pizza named '" + pizzaName + "'."));
        pizzas.remove(pizza);
        touch();
    }

    public synchronized void replacePizzas(Collection<Pizza> newPizzas) {
        ensureEditable();
        pizzas.clear();
        pizzas.addAll(newPizzas);
        touch();
    }

    /**
     * Pays the order and sends its pizzas to the kitchen.
     *
     * @param amount          amount given by the customer (the pizzeria keeps the change)
     * @param preparationTime time needed to bake the pizzas
     */
    public synchronized void pay(BigDecimal amount, Duration preparationTime) {
        ensureEditable();
        if (pizzas.isEmpty()) {
            throw new InvalidRequestException("Order " + id + " is empty and cannot be paid.");
        }
        BigDecimal price = getPrice();
        if (amount.compareTo(price) < 0) {
            throw new InsufficientPaymentException(
                    "Amount " + amount + " does not cover the order price (" + price + ").");
        }
        paid = true;
        pizzas.forEach(pizza -> pizza.startPreparation(clock, preparationTime));
        touch();
    }

    public synchronized OrderStatus getStatus() {
        if (!paid) {
            return OrderStatus.AWAITING_PAYMENT;
        }
        return pizzas.stream().allMatch(pizza -> pizza.isReady(clock)) ? OrderStatus.READY : OrderStatus.PREPARING;
    }

    /**
     * Last time the representation of the order changed, used for the {@code Last-Modified} HTTP header.
     * Pizzas becoming ready also changes the representation, even though no request modified the order.
     *
     * @return the most recent change
     */
    public synchronized Instant getLastModified() {
        Instant now = clock.instant();
        return pizzas.stream()
                .map(Pizza::getReadyAt)
                .flatMap(Optional::stream)
                .filter(readyAt -> !readyAt.isAfter(now))
                .reduce(lastModified, (a, b) -> a.isAfter(b) ? a : b);
    }

    /**
     * Entity tag of the current representation, used for the {@code ETag} HTTP header.
     * Unlike {@code Last-Modified} (one-second precision), it changes on every modification,
     * even when several requests happen within the same second.
     *
     * @return an opaque validator
     */
    public synchronized String getETag() {
        return id + "-" + version + "-" + getStatus();
    }

    public boolean isOwnedBy(String login) {
        return customer.getLogin().equals(login);
    }

    private void ensureEditable() {
        if (paid) {
            throw new ConflictException("Order " + id + " has already been paid and can no longer be modified.");
        }
    }

    private void touch() {
        lastModified = clock.instant();
        version++;
    }
}
