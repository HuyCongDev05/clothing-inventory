package com.example.backend.service;

import com.example.backend.dto.request.CategoryRequestDto;
import com.example.backend.dto.response.CategoryResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.model.Category;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getStatus() != Status.DELETED)
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new InvalidException(ErrorCode.CONFLICT_CATEGORY_NAME);
        }

        Category category = categoryMapper.toEntity(request);
        category.setStatus(Status.ACTIVE);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = findCategoryById(id);

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new InvalidException(ErrorCode.CONFLICT_CATEGORY_NAME);
            }
            category.setName(request.getName());
        }

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryById(id);
        category.setStatus(Status.DELETED);
    }

    @Transactional
    public CategoryResponseDto restoreCategory(String name) {
        Category categoryToRestore = categoryRepository.findByName(name)
                .orElseThrow(() -> new InvalidException(ErrorCode.CATEGORY_NOT_FOUND));

        if (categoryToRestore.getStatus() != Status.DELETED) {
            throw new InvalidException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        categoryToRestore.setStatus(Status.ACTIVE);
        return categoryMapper.toResponse(categoryToRestore);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
