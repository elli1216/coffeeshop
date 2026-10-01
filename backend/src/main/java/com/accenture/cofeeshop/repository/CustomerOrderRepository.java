package com.accenture.cofeeshop.repository;

import com.accenture.cofeeshop.models.CustomerOrder;
import com.accenture.cofeeshop.models.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByStatus(OrderStatus status);
}
