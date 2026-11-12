package com.pizzaplatform.api.controller;

import com.pizzaplatform.api.config.OpenApiConfig;
import com.pizzaplatform.api.dto.OrderDtos.AddPizzaRequest;
import com.pizzaplatform.api.dto.OrderDtos.OrderListResponse;
import com.pizzaplatform.api.dto.OrderDtos.OrderRequest;
import com.pizzaplatform.api.dto.OrderDtos.OrderResponse;
import com.pizzaplatform.api.dto.OrderDtos.PaymentRequest;
import com.pizzaplatform.api.model.Order;
import com.pizzaplatform.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Clock;

import static com.pizzaplatform.api.security.JwtAuthenticationFilter.AUTHENTICATED_LOGIN;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OrderController {

    private final OrderService orderService;
    private final Clock clock;

    public OrderController(OrderService orderService, Clock clock) {
        this.orderService = orderService;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "List your orders")
    public OrderListResponse listMine(@Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return new OrderListResponse(orderService.findMine(user).stream().map(this::toResponse).toList());
    }

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request,
                                                @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        Order order = orderService.create(request.pizzas(), user);
        return ResponseEntity.created(URI.create("/orders/" + order.getId())).body(toResponse(order));
    }

    /**
     * Supports conditional requests with both validators: {@code ETag} (exact, preferred by clients)
     * and {@code Last-Modified} (one-second precision). Returns {@code 304 Not Modified} when the order
     * has not changed.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get one of your orders (supports ETag and Last-Modified revalidation)")
    public ResponseEntity<OrderResponse> get(@PathVariable int id,
                                             @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user,
                                             WebRequest webRequest) {
        Order order = orderService.get(id, user);
        if (webRequest.checkNotModified(order.getETag(), order.getLastModified().toEpochMilli())) {
            return null; // Spring sends 304 Not Modified
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noCache().cachePrivate()).body(toResponse(order));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the pizzas of an unpaid order")
    public OrderResponse replacePizzas(@PathVariable int id, @Valid @RequestBody OrderRequest request,
                                       @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return toResponse(orderService.replacePizzas(id, request.pizzas(), user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an unpaid order (a second call returns 410 Gone)")
    public ResponseEntity<Void> cancel(@PathVariable int id,
                                       @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        orderService.cancel(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pizzas")
    @Operation(summary = "Add a pizza to an unpaid order")
    public OrderResponse addPizza(@PathVariable int id, @Valid @RequestBody AddPizzaRequest request,
                                  @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return toResponse(orderService.addPizza(id, request.pizzaName(), user));
    }

    @DeleteMapping("/{id}/pizzas/{pizzaName}")
    @Operation(summary = "Remove a pizza from an unpaid order")
    public OrderResponse removePizza(@PathVariable int id, @PathVariable String pizzaName,
                                     @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return toResponse(orderService.removePizza(id, pizzaName, user));
    }

    @PostMapping("/{id}/payment")
    @Operation(summary = "Pay an order: its pizzas then go to the oven (402 if the amount is too low)")
    public OrderResponse pay(@PathVariable int id, @Valid @RequestBody PaymentRequest request,
                             @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return toResponse(orderService.pay(id, request.amount(), user));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.of(order, clock);
    }
}
