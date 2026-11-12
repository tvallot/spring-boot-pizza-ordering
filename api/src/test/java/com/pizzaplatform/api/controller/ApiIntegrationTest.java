package com.pizzaplatform.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * End-to-end tests of the HTTP API: routing, authentication, authorization, status codes,
 * content negotiation and conditional requests.
 * Pizzas are baked instantly so that the full order lifecycle can be tested.
 */
@SpringBootTest(properties = "app.kitchen.preparation-time=0s")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------- menu

    @Test
    void menuIsPublicAndCacheable() throws Exception {
        mvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=3600")))
                .andExpect(jsonPath("$.pizzas", hasSize(20)))
                .andExpect(jsonPath("$.pizzas[0].name").value("Margherita"))
                .andExpect(jsonPath("$.pizzas[0].price").value(8.5));
    }

    @Test
    void menuCanBeReturnedAsXml() throws Exception {
        mvc.perform(get("/menu").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/menu/pizza[1]/name").string("Margherita"));
    }

    // ---------------------------------------------------------------- customers & authentication

    @Test
    void registrationValidatesInputAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"x\",\"name\":\"\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("login")))
                .andExpect(jsonPath("$.detail", containsString("password")));

        String login = register();
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(registration(login)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRequiresTheRightPassword() throws Exception {
        String login = register();

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRequireAValidToken() throws Exception {
        mvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        mvc.perform(get("/orders").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = login(register());

        mvc.perform(auth(post("/auth/logout"), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/orders"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void customersCanOnlyAccessTheirOwnProfile() throws Exception {
        String alice = register();
        String bob = register();
        String aliceToken = login(alice);

        mvc.perform(auth(get("/customers/" + alice), aliceToken)).andExpect(status().isOk());
        mvc.perform(auth(get("/customers/" + bob), aliceToken)).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/customers/" + bob), aliceToken)).andExpect(status().isForbidden());
    }

    @Test
    void profileSupportsETagRevalidation() throws Exception {
        String login = register();
        String token = login(login);

        String etag = mvc.perform(auth(get("/customers/" + login), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mvc.perform(auth(get("/customers/" + login), token).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());

        mvc.perform(auth(put("/customers/" + login), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));

        mvc.perform(auth(get("/customers/" + login), token).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- orders

    @Test
    void fullOrderLifecycle() throws Exception {
        String token = login(register());

        JsonNode order = json(mvc.perform(auth(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pizzas\":[\"Margherita\",\"Regina\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT")));
        String url = "/orders/" + order.get("id").asInt();

        mvc.perform(auth(post(url + "/pizzas"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pizzaName\":\"Four Cheese\"}"))
                .andExpect(jsonPath("$.pizzas", hasSize(3)));

        mvc.perform(auth(delete(url + "/pizzas/Margherita"), token))
                .andExpect(jsonPath("$.pizzas", hasSize(2)))
                .andExpect(jsonPath("$.price").value(26.0)); // Regina 11.50 + Four Cheese 14.50

        mvc.perform(auth(post(url + "/payment"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10}"))
                .andExpect(status().isPaymentRequired());

        mvc.perform(auth(post(url + "/payment"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));

        mvc.perform(auth(get(url), token))
                .andExpect(jsonPath("$.status").value("READY"));

        mvc.perform(auth(post(url + "/pizzas"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pizzaName\":\"Regina\"}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(delete(url), token)).andExpect(status().isConflict());
    }

    @Test
    void ordersOfOtherCustomersAreForbidden() throws Exception {
        String aliceToken = login(register());
        String bobToken = login(register());
        int orderId = createOrder(aliceToken);

        mvc.perform(auth(get("/orders/" + orderId), bobToken)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/orders"), bobToken)).andExpect(jsonPath("$.orders", hasSize(0)));
    }

    @Test
    void cancelledOrdersAreGone() throws Exception {
        String token = login(register());
        int orderId = createOrder(token);

        mvc.perform(auth(delete("/orders/" + orderId), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/orders/" + orderId), token)).andExpect(status().isGone());
        mvc.perform(auth(get("/orders/999999"), token)).andExpect(status().isNotFound());
    }

    @Test
    void unknownPizzasAreRejected() throws Exception {
        String token = login(register());

        mvc.perform(auth(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pizzas\":[\"Hawaiian\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Hawaiian")));
    }

    @Test
    void orderSupportsLastModifiedRevalidation() throws Exception {
        String token = login(register());
        int orderId = createOrder(token);

        String lastModified = mvc.perform(auth(get("/orders/" + orderId), token))
                .andReturn().getResponse().getHeader(HttpHeaders.LAST_MODIFIED);

        mvc.perform(auth(get("/orders/" + orderId), token).header(HttpHeaders.IF_MODIFIED_SINCE, lastModified))
                .andExpect(status().isNotModified());
    }

    /**
     * Regression test: Last-Modified has a one-second precision, so a change made within the same second
     * must still be detected thanks to the ETag.
     */
    @Test
    void orderChangesWithinTheSameSecondAreNotMissedByTheCache() throws Exception {
        String token = login(register());
        int orderId = createOrder(token);

        var cached = mvc.perform(auth(get("/orders/" + orderId), token)).andReturn().getResponse();
        String etag = cached.getHeader(HttpHeaders.ETAG);
        String lastModified = cached.getHeader(HttpHeaders.LAST_MODIFIED);

        mvc.perform(auth(post("/orders/" + orderId + "/payment"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100}"))
                .andExpect(status().isOk());

        mvc.perform(auth(get("/orders/" + orderId), token)
                        .header(HttpHeaders.IF_NONE_MATCH, etag)
                        .header(HttpHeaders.IF_MODIFIED_SINCE, lastModified))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void ordersAcceptXmlBodies() throws Exception {
        String token = login(register());

        mvc.perform(auth(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML)
                        .content("<order><pizzas><pizza>Regina</pizza><pizza>Tuna</pizza></pizzas></order>"))
                .andExpect(status().isCreated())
                .andExpect(xpath("count(/order/pizzas/pizza)").number(2.0));
    }

    // ---------------------------------------------------------------- helpers

    private String register() throws Exception {
        String login = "user-" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(registration(login)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/customers/" + login));
        return login;
    }

    private static String registration(String login) {
        return "{\"login\":\"" + login + "\",\"name\":\"Test user\",\"password\":\"secret123\"}";
    }

    private String login(String login) throws Exception {
        return json(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()))
                .get("token").asText();
    }

    private int createOrder(String token) throws Exception {
        return json(mvc.perform(auth(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pizzas\":[\"Regina\"]}"))
                .andExpect(status().isCreated()))
                .get("id").asInt();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
