package com.accenture.cofeeshop.service;

import com.accenture.cofeeshop.dto.*;
import com.accenture.cofeeshop.dto.product.ProductRequest;
import com.accenture.cofeeshop.dto.product.ProductResponse;
import com.accenture.cofeeshop.exception.ResourceNotFoundException;
import com.accenture.cofeeshop.models.Product;
import com.accenture.cofeeshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ProductResponse> findAvailable() {
        return productRepository.findByAvailableTrue().stream().map(this::toResponse).toList();
    }

    public List<ProductResponse> findByCategory(Long categoryId) {
        categoryService.getCategory(categoryId); // 404 if category doesn't exist
        return productRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        apply(product, request);
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    // Used by OrderService
    Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailable(request.available() == null || request.available());
        product.setCategory(categoryService.getCategory(request.categoryId()));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getAvailable(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null);
    }
}
