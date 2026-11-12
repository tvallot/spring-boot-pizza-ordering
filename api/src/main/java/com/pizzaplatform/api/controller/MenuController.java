package com.pizzaplatform.api.controller;

import com.pizzaplatform.api.dto.MenuDtos.MenuResponse;
import com.pizzaplatform.api.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/menu")
@Tag(name = "Menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(summary = "List the pizzas on the menu (public, cacheable for one hour)")
    public ResponseEntity<MenuResponse> getMenu() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(MenuResponse.of(menuService.getPizzas()));
    }
}
