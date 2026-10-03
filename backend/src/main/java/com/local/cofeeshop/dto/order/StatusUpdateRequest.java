package com.local.cofeeshop.dto.order;

import com.local.cofeeshop.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull OrderStatus status) {}