package com.pizzaplatform.api.model;

import java.util.Objects;

/**
 * A registered customer. The login is the unique, immutable identifier.
 */
public class Customer {

    private final String login;
    private String name;
    private final String passwordHash;

    public Customer(String login, String name, String passwordHash) {
        this.login = login;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer other && login.equals(other.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }
}
