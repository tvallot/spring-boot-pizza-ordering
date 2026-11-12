package com.pizzaplatform.api.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.pizzaplatform.api.model.Ingredient;
import com.pizzaplatform.api.model.PizzaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Payloads of the menu endpoint.
 */
public final class MenuDtos {

    private MenuDtos() {
    }

    @JacksonXmlRootElement(localName = "menu")
    public record MenuResponse(
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "pizza")
            List<MenuItem> pizzas) {

        public static MenuResponse of(List<PizzaTemplate> templates) {
            return new MenuResponse(templates.stream().map(MenuItem::of).toList());
        }
    }

    public record MenuItem(
            String name,
            String base,
            @JacksonXmlElementWrapper(localName = "ingredients") @JacksonXmlProperty(localName = "ingredient")
            List<IngredientDto> ingredients,
            BigDecimal price) {

        static MenuItem of(PizzaTemplate template) {
            return new MenuItem(
                    template.name(),
                    template.base().name().toLowerCase(Locale.ROOT),
                    template.ingredients().stream().map(IngredientDto::of).toList(),
                    template.price());
        }
    }

    public record IngredientDto(String name, BigDecimal price) {
        static IngredientDto of(Ingredient ingredient) {
            return new IngredientDto(ingredient.name(), ingredient.price());
        }
    }
}
