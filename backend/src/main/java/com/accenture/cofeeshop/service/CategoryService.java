package com.accenture.cofeeshop.service;

import com.accenture.cofeeshop.dto.*;
import com.accenture.cofeeshop.dto.category.CategoryRequest;
import com.accenture.cofeeshop.dto.category.CategoryResponse;
import com.accenture.cofeeshop.exception.*;
import com.accenture.cofeeshop.models.Category;
import com.accenture.cofeeshop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CategoryResponse findById(Long id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Category already exists: " + request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(category); // saved automatically at commit
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getCategory(id));
    }

    // Used by ProductService
    Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
