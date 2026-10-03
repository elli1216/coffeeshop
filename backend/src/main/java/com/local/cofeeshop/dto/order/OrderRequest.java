package com.local.cofeeshop.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotEmpty List<@Valid OrderItemRequest> items) {}

