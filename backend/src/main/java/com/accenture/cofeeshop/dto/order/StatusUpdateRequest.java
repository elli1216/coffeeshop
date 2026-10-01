package com.accenture.cofeeshop.dto.order;

import com.accenture.cofeeshop.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull OrderStatus status) {}