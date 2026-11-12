package com.pizzaplatform.api.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.pizzaplatform.api.model.Order;
import com.pizzaplatform.api.model.OrderStatus;
import com.pizzaplatform.api.model.Pizza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

/**
 * Payloads of the order endpoints.
 */
public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * @param pizzas names of the pizzas, as found on the menu (may contain duplicates)
     */
    @JacksonXmlRootElement(localName = "order")
    public record OrderRequest(
            // In XML: <order><pizzas><pizza>Regina</pizza>...</pizzas></order> (child element names are free)
            @NotNull @JacksonXmlElementWrapper(localName = "pizzas")
            List<@NotBlank String> pizzas) { }

    @JacksonXmlRootElement(localName = "pizza")
    public record AddPizzaRequest(@NotBlank String pizzaName) { }

    @JacksonXmlRootElement(localName = "payment")
    public record PaymentRequest(@NotNull @PositiveOrZero BigDecimal amount) { }

    @JacksonXmlRootElement(localName = "order")
    public record OrderResponse(
            int id,
            String customer,
            @JacksonXmlElementWrapper(localName = "pizzas") @JacksonXmlProperty(localName = "pizza")
            List<OrderedPizza> pizzas,
            BigDecimal price,
            boolean paid,
            OrderStatus status,
            String href) {

        public static OrderResponse of(Order order, Clock clock) {
            return new OrderResponse(
                    order.getId(),
                    order.getCustomer().getLogin(),
                    order.getPizzas().stream().map(pizza -> OrderedPizza.of(pizza, clock)).toList(),
                    order.getPrice(),
                    order.isPaid(),
                    order.getStatus(),
                    "/orders/" + order.getId());
        }
    }

    public record OrderedPizza(String name, String base, BigDecimal price, boolean ready) {
        static OrderedPizza of(Pizza pizza, Clock clock) {
            return new OrderedPizza(pizza.getName(), pizza.getBase().name().toLowerCase(Locale.ROOT),
                    pizza.getPrice(), pizza.isReady(clock));
        }
    }

    @JacksonXmlRootElement(localName = "orders")
    public record OrderListResponse(
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "order")
            List<OrderResponse> orders) { }
}
