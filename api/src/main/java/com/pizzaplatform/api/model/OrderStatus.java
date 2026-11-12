package com.pizzaplatform.api.model;

/**
 * Lifecycle of an order: it can be edited until it is paid, then the kitchen prepares it.
 */
public enum OrderStatus {
    /** The order can still be modified and is waiting for payment. */
    AWAITING_PAYMENT,
    /** The order has been paid and its pizzas are in the oven. */
    PREPARING,
    /** Every pizza is ready, the customer can pick up the order. */
    READY
}
