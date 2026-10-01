package com.accenture.cofeeshop.repository;

import com.accenture.cofeeshop.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCustomerId(Long customerId);
    List<Product> findByAvailableTrue();
    List<Product> findByName(String name);

    List<Product> findByCategoryId(Long categoryId);
}
