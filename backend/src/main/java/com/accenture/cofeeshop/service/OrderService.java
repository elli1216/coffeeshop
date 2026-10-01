package com.accenture.cofeeshop.service;

import com.accenture.cofeeshop.dto.order.OrderItemRequest;
import com.accenture.cofeeshop.dto.order.OrderItemResponse;
import com.accenture.cofeeshop.dto.order.OrderRequest;
import com.accenture.cofeeshop.dto.order.OrderResponse;
import com.accenture.cofeeshop.exception.*;
import com.accenture.cofeeshop.models.*;
import com.accenture.cofeeshop.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductService productService;

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public OrderResponse findById(Long id) {
        return toResponse(getOrder(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(request.customerName());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.getProduct(itemRequest.productId());
            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new BusinessException("Product is not available: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getPrice()); // lock in today's price
            order.addItem(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(total);
        return toResponse(orderRepository.save(order)); // cascades to order_items
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order = getOrder(id);
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        return updateStatus(id, OrderStatus.CANCELLED);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean allowed = switch (current) {
            case PENDING   -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessException("Cannot change order status from " + current + " to " + next);
        }
    }

    private CustomerOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(CustomerOrder o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        return new OrderResponse(o.getId(), o.getCustomerName(), o.getStatus(),
                o.getTotalAmount(), o.getCreatedAt(), items);
    }
}
