package com.pizzaplatform.api.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payloads of the customer endpoints.
 */
public final class CustomerDtos {

    private CustomerDtos() {
    }

    @JacksonXmlRootElement(localName = "customer")
    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,30}",
                    message = "must be 3 to 30 letters, digits, '_' or '-'") String login,
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(min = 6, max = 100) String password) { }

    @JacksonXmlRootElement(localName = "customer")
    public record UpdateCustomerRequest(@NotBlank @Size(max = 50) String name) { }

    /**
     * @param login  unique identifier of the customer
     * @param name   display name
     * @param orders links to the orders of the customer
     */
    @JacksonXmlRootElement(localName = "customer")
    public record CustomerResponse(
            String login,
            String name,
            @JacksonXmlElementWrapper(localName = "orders") @JacksonXmlProperty(localName = "link")
            List<LinkDto> orders) { }

    /**
     * @param count     number of registered customers
     * @param customers links to every customer
     */
    @JacksonXmlRootElement(localName = "customers")
    public record CustomerListResponse(
            int count,
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "link")
            List<LinkDto> customers) { }
}
