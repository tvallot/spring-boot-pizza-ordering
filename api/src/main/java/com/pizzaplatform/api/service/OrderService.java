package com.pizzaplatform.api.service;

import com.pizzaplatform.api.config.AppProperties;
import com.pizzaplatform.api.exception.Exceptions.ConflictException;
import com.pizzaplatform.api.exception.Exceptions.ForbiddenException;
import com.pizzaplatform.api.exception.Exceptions.GoneException;
import com.pizzaplatform.api.exception.Exceptions.NotFoundException;
import com.pizzaplatform.api.exception.Exceptions.UnauthorizedException;
import com.pizzaplatform.api.model.Customer;
import com.pizzaplatform.api.model.Order;
import com.pizzaplatform.api.model.Pizza;
import com.pizzaplatform.api.repository.CustomerRepository;
import com.pizzaplatform.api.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Order management. Every operation checks that the authenticated customer owns the order.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final MenuService menu;
    private final Duration preparationTime;

    public OrderService(OrderRepository orders, CustomerRepository customers, MenuService menu,
                        AppProperties properties) {
        this.orders = orders;
        this.customers = customers;
        this.menu = menu;
        this.preparationTime = properties.kitchen().preparationTime();
    }

    public Order create(List<String> pizzaNames, String authenticatedUser) {
        Customer customer = customers.findByLogin(authenticatedUser)
                .orElseThrow(() -> new UnauthorizedException("Your account no longer exists."));
        List<Pizza> pizzas = createPizzas(pizzaNames); // validate before creating the order
        Order order = orders.create(customer);
        order.replacePizzas(pizzas);
        return order;
    }

    public List<Order> findMine(String authenticatedUser) {
        return orders.findByCustomer(authenticatedUser);
    }

    /**
     * @return the order, if it exists and belongs to the authenticated customer
     * @throws GoneException      if the order has been deleted
     * @throws NotFoundException  if the order never existed
     * @throws ForbiddenException if the order belongs to someone else
     */
    public Order get(int id, String authenticatedUser) {
        Order order = orders.findById(id).orElseThrow(() -> orders.wasDeleted(id)
                ? new GoneException("Order " + id + " has been cancelled.")
                : new NotFoundException("Order " + id + " does not exist."));
        if (!order.isOwnedBy(authenticatedUser)) {
            throw new ForbiddenException("Order " + id + " belongs to another customer.");
        }
        return order;
    }

    public Order replacePizzas(int id, List<String> pizzaNames, String authenticatedUser) {
        Order order = get(id, authenticatedUser);
        order.replacePizzas(createPizzas(pizzaNames));
        return order;
    }

    public Order addPizza(int id, String pizzaName, String authenticatedUser) {
        Order order = get(id, authenticatedUser);
        order.addPizza(menu.createPizza(pizzaName));
        return order;
    }

    public Order removePizza(int id, String pizzaName, String authenticatedUser) {
        Order order = get(id, authenticatedUser);
        order.removePizza(pizzaName);
        return order;
    }

    public Order pay(int id, BigDecimal amount, String authenticatedUser) {
        Order order = get(id, authenticatedUser);
        order.pay(amount, preparationTime);
        return order;
    }

    /**
     * Cancels an order. Paid orders are already in the oven and cannot be cancelled.
     */
    public void cancel(int id, String authenticatedUser) {
        Order order = get(id, authenticatedUser);
        if (order.isPaid()) {
            throw new ConflictException("Order " + id + " has already been paid and cannot be cancelled.");
        }
        orders.delete(id);
    }

    private List<Pizza> createPizzas(List<String> pizzaNames) {
        return pizzaNames.stream().map(menu::createPizza).toList();
    }
}
