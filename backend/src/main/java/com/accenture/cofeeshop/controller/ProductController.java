package com.accenture.cofeeshop.controller;

import com.accenture.cofeeshop.dto.*;
import com.accenture.cofeeshop.dto.product.ProductRequest;
import com.accenture.cofeeshop.dto.product.ProductResponse;
import com.accenture.cofeeshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAll(
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return productService.findByCategory(categoryId);
        }
        if (Boolean.TRUE.equals(available)) {
            return productService.findAvailable();
        }
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
