package com.pizzaplatform.api.service;

import com.pizzaplatform.api.exception.Exceptions.InvalidRequestException;
import com.pizzaplatform.api.model.Pizza;
import com.pizzaplatform.api.model.PizzaBase;
import com.pizzaplatform.api.model.PizzaTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuServiceTest {

    private final MenuService menu = new MenuService(
            new ClassPathResource("data/ingredients.csv"),
            new ClassPathResource("data/pizzas.csv"));

    @Test
    void loadsTheWholeMenuInFileOrder() {
        assertThat(menu.getPizzas()).hasSize(20);
        assertThat(menu.getPizzas().getFirst().name()).isEqualTo("Margherita");
    }

    @Test
    void computesTemplatePrices() {
        PizzaTemplate margherita = menu.getPizzas().getFirst();

        // tomato base 2.00 + mozzarella 2.00 + oregano 2.00 + olives 2.50
        assertThat(margherita.base()).isEqualTo(PizzaBase.TOMATO);
        assertThat(margherita.price()).isEqualByComparingTo("8.50");
    }

    @Test
    void createsPizzasFromTheMenu() {
        Pizza pizza = menu.createPizza("Norwegian");

        assertThat(pizza.getBase()).isEqualTo(PizzaBase.CREAM);
        assertThat(pizza.getIngredients()).extracting("name").containsExactly("mozzarella", "salmon");
    }

    @Test
    void rejectsPizzasThatAreNotOnTheMenu() {
        assertThatThrownBy(() -> menu.createPizza("Hawaiian")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void failsFastOnUnknownIngredients() {
        var ingredients = new ByteArrayResource("mozzarella,2".getBytes(StandardCharsets.UTF_8));
        var pizzas = new ByteArrayResource("Broken,tomato,mozzarella,pineapple".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new MenuService(ingredients, pizzas))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pineapple");
    }
}
