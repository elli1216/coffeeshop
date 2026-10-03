package com.local.cofeeshop.dto.order;

import com.local.cofeeshop.models.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id, String customerName, OrderStatus status,
        BigDecimal totalAmount, LocalDateTime createdAt,
        List<OrderItemResponse> items) {}
