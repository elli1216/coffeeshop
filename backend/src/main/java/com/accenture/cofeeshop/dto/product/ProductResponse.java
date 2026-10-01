package com.accenture.cofeeshop.dto.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id, String name, String description,
        BigDecimal price, Boolean available,
        Long categoryId, String categoryName) {}
