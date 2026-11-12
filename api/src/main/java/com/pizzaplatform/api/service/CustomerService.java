package com.pizzaplatform.api.service;

import com.pizzaplatform.api.exception.Exceptions.ConflictException;
import com.pizzaplatform.api.exception.Exceptions.ForbiddenException;
import com.pizzaplatform.api.exception.Exceptions.NotFoundException;
import com.pizzaplatform.api.exception.Exceptions.UnauthorizedException;
import com.pizzaplatform.api.model.Customer;
import com.pizzaplatform.api.repository.CustomerRepository;
import com.pizzaplatform.api.repository.OrderRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registration, authentication and profile management of customers.
 */
@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customers, OrderRepository orders, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer register(String login, String name, String password) {
        Customer customer = new Customer(login, name, passwordEncoder.encode(password));
        if (!customers.addIfAbsent(customer)) {
            throw new ConflictException("Login '" + login + "' is already taken.");
        }
        return customer;
    }

    /**
     * Checks the credentials of a customer.
     * The same error is returned for an unknown login and a wrong password, to avoid revealing which logins exist.
     *
     * @return the authenticated customer
     */
    public Customer authenticate(String login, String password) {
        return customers.findByLogin(login)
                .filter(customer -> passwordEncoder.matches(password, customer.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid login or password."));
    }

    public List<Customer> findAll() {
        return customers.findAll();
    }

    /**
     * @param login             login of the requested customer
     * @param authenticatedUser login of the customer making the request
     * @return the customer, if the requester is that customer
     */
    public Customer getOwnProfile(String login, String authenticatedUser) {
        if (!login.equals(authenticatedUser)) {
            throw new ForbiddenException("You can only access your own account.");
        }
        return customers.findByLogin(login)
                .orElseThrow(() -> new NotFoundException("Customer '" + login + "' does not exist."));
    }

    public Customer rename(String login, String newName, String authenticatedUser) {
        Customer customer = getOwnProfile(login, authenticatedUser);
        customer.setName(newName);
        return customer;
    }

    /**
     * Deletes a customer account together with its orders.
     */
    public void delete(String login, String authenticatedUser) {
        getOwnProfile(login, authenticatedUser);
        orders.deleteByCustomer(login);
        customers.delete(login);
    }
}
