package com.pizzaplatform.api.repository;

import com.pizzaplatform.api.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory storage of customers, indexed by login.
 * Swapping it for a database-backed implementation would not impact the services.
 */
@Repository
public class CustomerRepository {

    private final ConcurrentMap<String, Customer> customers = new ConcurrentHashMap<>();

    /**
     * Stores a customer unless the login is already taken.
     *
     * @param customer the customer to store
     * @return true if the customer was added, false if the login already exists
     */
    public boolean addIfAbsent(Customer customer) {
        return customers.putIfAbsent(customer.getLogin(), customer) == null;
    }

    public Optional<Customer> findByLogin(String login) {
        return Optional.ofNullable(customers.get(login));
    }

    public List<Customer> findAll() {
        return customers.values().stream()
                .sorted(Comparator.comparing(Customer::getLogin))
                .toList();
    }

    public void delete(String login) {
        customers.remove(login);
    }
}
