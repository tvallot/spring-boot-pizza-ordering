package com.pizzaplatform.api.service;

import com.pizzaplatform.api.exception.Exceptions.InvalidRequestException;
import com.pizzaplatform.api.model.Ingredient;
import com.pizzaplatform.api.model.Pizza;
import com.pizzaplatform.api.model.PizzaBase;
import com.pizzaplatform.api.model.PizzaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pizzeria menu, loaded once at startup from two CSV files:
 * <ul>
 *     <li>{@code ingredients.csv}: {@code name,price}</li>
 *     <li>{@code pizzas.csv}: {@code name,base,ingredient1,ingredient2,...}</li>
 * </ul>
 */
@Service
public class MenuService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuService.class);
    private static final String SEPARATOR = ",";

    private final Map<String, Ingredient> ingredients;
    private final Map<String, PizzaTemplate> pizzas;

    public MenuService(@Value("classpath:data/ingredients.csv") Resource ingredientsFile,
                       @Value("classpath:data/pizzas.csv") Resource pizzasFile) {
        this.ingredients = readIngredients(ingredientsFile);
        this.pizzas = readPizzas(pizzasFile);
        LOGGER.info("Menu loaded: {} ingredients, {} pizzas", ingredients.size(), pizzas.size());
    }

    public List<PizzaTemplate> getPizzas() {
        return List.copyOf(pizzas.values());
    }

    /**
     * Creates a pizza from its name on the menu.
     *
     * @param name name of the pizza on the menu
     * @return a new pizza instance
     * @throws InvalidRequestException if the pizza is not on the menu
     */
    public Pizza createPizza(String name) {
        PizzaTemplate template = pizzas.get(name);
        if (template == null) {
            throw new InvalidRequestException("'" + name + "' is not on the menu.");
        }
        return template.toPizza();
    }

    private static Map<String, Ingredient> readIngredients(Resource file) {
        Map<String, Ingredient> result = new LinkedHashMap<>();
        for (String[] fields : readCsv(file)) {
            String name = fields[0].trim();
            result.put(name, new Ingredient(name, new BigDecimal(fields[1].trim())));
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, PizzaTemplate> readPizzas(Resource file) {
        Map<String, PizzaTemplate> result = new LinkedHashMap<>();
        for (String[] fields : readCsv(file)) {
            String name = fields[0].trim();
            PizzaBase base = PizzaBase.parse(fields[1]);
            List<Ingredient> toppings = Arrays.stream(fields, 2, fields.length)
                    .map(String::trim)
                    .map(this::findIngredient)
                    .toList();
            result.put(name, new PizzaTemplate(name, base, toppings));
        }
        return Collections.unmodifiableMap(result);
    }

    private Ingredient findIngredient(String name) {
        Ingredient ingredient = ingredients.get(name);
        if (ingredient == null) {
            throw new IllegalStateException("pizzas.csv references an unknown ingredient: " + name);
        }
        return ingredient;
    }

    private static List<String[]> readCsv(Resource file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split(SEPARATOR))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file.getFilename(), e);
        }
    }
}
