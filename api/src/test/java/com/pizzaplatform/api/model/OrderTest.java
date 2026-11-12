package com.pizzaplatform.api.model;

import com.pizzaplatform.api.exception.Exceptions.ConflictException;
import com.pizzaplatform.api.exception.Exceptions.InsufficientPaymentException;
import com.pizzaplatform.api.exception.Exceptions.InvalidRequestException;
import com.pizzaplatform.api.exception.Exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Duration PREPARATION_TIME = Duration.ofMinutes(10);

    private final MutableClock clock = new MutableClock(Instant.parse("2025-01-01T12:00:00Z"));
    private final Ingredient mozzarella = new Ingredient("mozzarella", new BigDecimal("2.00"));
    private final Ingredient ham = new Ingredient("ham", new BigDecimal("3.00"));
    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(1, new Customer("alice", "Alice", "hash"), clock);
    }

    @Test
    void priceIsTheSumOfBasesAndIngredients() {
        order.addPizza(new Pizza("Regina", PizzaBase.TOMATO, List.of(mozzarella, ham))); // 2 + 2 + 3
        order.addPizza(new Pizza("White", PizzaBase.CREAM, List.of(mozzarella)));        // 3 + 2

        assertThat(order.getPrice()).isEqualByComparingTo("12.00");
    }

    @Test
    void removePizzaRemovesOnlyOneInstance() {
        order.addPizza(regina());
        order.addPizza(regina());

        order.removePizza("Regina");

        assertThat(order.getPizzas()).hasSize(1);
    }

    @Test
    void removingAnAbsentPizzaFails() {
        assertThatThrownBy(() -> order.removePizza("Regina")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void emptyOrderCannotBePaid() {
        assertThatThrownBy(() -> order.pay(BigDecimal.TEN, PREPARATION_TIME))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void insufficientPaymentIsRejected() {
        order.addPizza(regina());

        assertThatThrownBy(() -> order.pay(new BigDecimal("6.99"), PREPARATION_TIME))
                .isInstanceOf(InsufficientPaymentException.class);
        assertThat(order.isPaid()).isFalse();
    }

    @Test
    void paidOrderCannotBeModified() {
        order.addPizza(regina());
        order.pay(new BigDecimal("7.00"), PREPARATION_TIME);

        assertThatThrownBy(() -> order.addPizza(regina())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> order.removePizza("Regina")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> order.pay(BigDecimal.TEN, PREPARATION_TIME)).isInstanceOf(ConflictException.class);
    }

    @Test
    void statusFollowsThePaymentAndTheKitchen() {
        order.addPizza(regina());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);

        order.pay(new BigDecimal("7.00"), PREPARATION_TIME);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        clock.advance(PREPARATION_TIME);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void lastModifiedMovesWhenPizzasBecomeReady() {
        order.addPizza(regina());
        order.pay(new BigDecimal("7.00"), PREPARATION_TIME);
        Instant paidAt = order.getLastModified();

        clock.advance(Duration.ofMinutes(5));
        assertThat(order.getLastModified()).isEqualTo(paidAt);

        clock.advance(Duration.ofMinutes(10));
        assertThat(order.getLastModified()).isEqualTo(paidAt.plus(PREPARATION_TIME));
    }

    private Pizza regina() {
        return new Pizza("Regina", PizzaBase.TOMATO, List.of(mozzarella, ham));
    }

    /** A clock that tests can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
