package com.pizzaplatform.api.repository;

import com.pizzaplatform.api.model.Customer;
import com.pizzaplatform.api.model.Order;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory storage of orders.
 * Remembers deleted identifiers so that the API can answer {@code 410 Gone} instead of {@code 404 Not Found}.
 */
@Repository
public class OrderRepository {

    private final AtomicInteger sequence = new AtomicInteger();
    private final ConcurrentMap<Integer, Order> orders = new ConcurrentHashMap<>();
    private final Set<Integer> deletedIds = ConcurrentHashMap.newKeySet();
    private final Clock clock;

    public OrderRepository(Clock clock) {
        this.clock = clock;
    }

    /**
     * Creates and stores a new empty order with a unique identifier.
     *
     * @param customer owner of the order
     * @return the stored order
     */
    public Order create(Customer customer) {
        Order order = new Order(sequence.incrementAndGet(), customer, clock);
        orders.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findById(int id) {
        return Optional.ofNullable(orders.get(id));
    }

    public boolean wasDeleted(int id) {
        return deletedIds.contains(id);
    }

    public List<Order> findByCustomer(String login) {
        return orders.values().stream()
                .filter(order -> order.isOwnedBy(login))
                .sorted(Comparator.comparingInt(Order::getId))
                .toList();
    }

    public void delete(int id) {
        if (orders.remove(id) != null) {
            deletedIds.add(id);
        }
    }

    public void deleteByCustomer(String login) {
        findByCustomer(login).forEach(order -> delete(order.getId()));
    }
}
