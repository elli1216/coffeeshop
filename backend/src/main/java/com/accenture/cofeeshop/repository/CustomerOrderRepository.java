package com.accenture.cofeeshop.repository;

import com.accenture.cofeeshop.models.CustomerOrder;
import com.accenture.cofeeshop.models.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByStatus(OrderStatus status);
}
