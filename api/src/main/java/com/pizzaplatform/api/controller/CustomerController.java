package com.pizzaplatform.api.controller;

import com.pizzaplatform.api.config.OpenApiConfig;
import com.pizzaplatform.api.dto.CustomerDtos.CustomerListResponse;
import com.pizzaplatform.api.dto.CustomerDtos.CustomerResponse;
import com.pizzaplatform.api.dto.CustomerDtos.RegisterRequest;
import com.pizzaplatform.api.dto.CustomerDtos.UpdateCustomerRequest;
import com.pizzaplatform.api.dto.LinkDto;
import com.pizzaplatform.api.model.Customer;
import com.pizzaplatform.api.model.Order;
import com.pizzaplatform.api.repository.OrderRepository;
import com.pizzaplatform.api.service.CustomerService;
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
import java.util.List;
import java.util.Objects;

import static com.pizzaplatform.api.security.JwtAuthenticationFilter.AUTHENTICATED_LOGIN;

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderRepository orderRepository;

    public CustomerController(CustomerService customerService, OrderRepository orderRepository) {
        this.customerService = customerService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    @Operation(summary = "List links to every registered customer (public)")
    public CustomerListResponse list() {
        List<LinkDto> links = customerService.findAll().stream()
                .map(customer -> new LinkDto("/customers/" + customer.getLogin()))
                .toList();
        return new CustomerListResponse(links.size(), links);
    }

    @PostMapping
    @Operation(summary = "Create an account (public)")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterRequest request) {
        Customer customer = customerService.register(request.login(), request.name(), request.password());
        return ResponseEntity.created(URI.create("/customers/" + customer.getLogin()))
                .body(toResponse(customer));
    }

    /**
     * Supports conditional requests: the ETag is derived from the customer data,
     * so a client sending {@code If-None-Match} receives {@code 304 Not Modified} when nothing changed.
     */
    @GetMapping("/{login}")
    @Operation(summary = "Get your own profile (supports ETag / If-None-Match)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<CustomerResponse> get(@PathVariable String login,
                                                @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user,
                                                WebRequest webRequest) {
        CustomerResponse response = toResponse(customerService.getOwnProfile(login, user));
        String etag = Integer.toHexString(Objects.hash(response.login(), response.name(), response.orders()));
        if (webRequest.checkNotModified(etag)) {
            return null; // Spring sends 304 Not Modified
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noCache().cachePrivate()).body(response);
    }

    @PutMapping("/{login}")
    @Operation(summary = "Change your display name")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public CustomerResponse update(@PathVariable String login,
                                   @Valid @RequestBody UpdateCustomerRequest request,
                                   @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        return toResponse(customerService.rename(login, request.name(), user));
    }

    @DeleteMapping("/{login}")
    @Operation(summary = "Delete your account and your orders")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> delete(@PathVariable String login,
                                       @Parameter(hidden = true) @RequestAttribute(AUTHENTICATED_LOGIN) String user) {
        customerService.delete(login, user);
        return ResponseEntity.noContent().build();
    }

    private CustomerResponse toResponse(Customer customer) {
        List<LinkDto> orders = orderRepository.findByCustomer(customer.getLogin()).stream()
                .map(Order::getId)
                .map(id -> new LinkDto("/orders/" + id))
                .toList();
        return new CustomerResponse(customer.getLogin(), customer.getName(), orders);
    }
}
