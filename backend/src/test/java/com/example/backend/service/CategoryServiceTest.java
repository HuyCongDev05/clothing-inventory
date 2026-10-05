package com.example.backend.service;

import com.example.backend.dto.request.CategoryRequestDto;
import com.example.backend.dto.response.CategoryResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.model.Category;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category activeCategory;
    private Category deletedCategory;

    @BeforeEach
    void setUp() {
        activeCategory = new Category();
        activeCategory.setId(1L);
        activeCategory.setName("Áo");
        activeCategory.setStatus(Status.ACTIVE);

        deletedCategory = new Category();
        deletedCategory.setId(2L);
        deletedCategory.setName("Quần");
        deletedCategory.setStatus(Status.DELETED);
    }

    // ─── getAllCategories() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCategories() - lọc bỏ DELETED, chỉ trả về ACTIVE")
    void getAllCategories_filtersDeleted() {
        when(categoryRepository.findAll()).thenReturn(List.of(activeCategory, deletedCategory));
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setName("Áo");
        when(categoryMapper.toResponse(activeCategory)).thenReturn(dto);

        List<CategoryResponseDto> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Áo");
        verify(categoryMapper, never()).toResponse(deletedCategory);
    }

    @Test
    @DisplayName("getAllCategories() - danh sách rỗng → trả về []")
    void getAllCategories_empty_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        List<CategoryResponseDto> result = categoryService.getAllCategories();
        assertThat(result).isEmpty();
    }

    // ─── createCategory() ────────────────────────────────────────────────────

    @Test
    @DisplayName("createCategory() - name đã tồn tại → throw CONFLICT_CATEGORY_NAME")
    void createCategory_nameConflict_throwsException() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("Áo");
        when(categoryRepository.existsByName("Áo")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_CATEGORY_NAME);
    }

    @Test
    @DisplayName("createCategory() - thành công: lưu và trả về DTO")
    void createCategory_success_savesAndReturnsDto() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("Giày");
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setName("Giày");

        when(categoryRepository.existsByName("Giày")).thenReturn(false);
        when(categoryMapper.toEntity(req)).thenReturn(activeCategory);
        when(categoryRepository.save(activeCategory)).thenReturn(activeCategory);
        when(categoryMapper.toResponse(activeCategory)).thenReturn(dto);

        CategoryResponseDto result = categoryService.createCategory(req);

        assertThat(result.getName()).isEqualTo("Giày");
        verify(categoryRepository).save(activeCategory);
    }

    // ─── updateCategory() ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory() - không tìm thấy → throw CATEGORY_NOT_FOUND")
    void updateCategory_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("NewName");

        assertThatThrownBy(() -> categoryService.updateCategory(99L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("updateCategory() - tên mới trùng với category khác → throw CONFLICT_CATEGORY_NAME")
    void updateCategory_nameConflict_throwsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(categoryRepository.existsByName("Quần")).thenReturn(true);

        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("Quần"); // khác tên cũ "Áo"

        assertThatThrownBy(() -> categoryService.updateCategory(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_CATEGORY_NAME);
    }

    @Test
    @DisplayName("updateCategory() - tên giống cũ → không kiểm tra conflict")
    void updateCategory_sameName_doesNotCheckConflict() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        CategoryResponseDto dto = new CategoryResponseDto();
        when(categoryMapper.toResponse(activeCategory)).thenReturn(dto);

        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("Áo"); // chính xác tên cũ

        categoryService.updateCategory(1L, req);
        verify(categoryRepository, never()).existsByName(anyString());
    }

    @Test
    @DisplayName("updateCategory() - tên mới hợp lệ → cập nhật thành công")
    void updateCategory_success_updatesName() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(categoryRepository.existsByName("Phụ kiện")).thenReturn(false);
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setName("Phụ kiện");
        when(categoryMapper.toResponse(activeCategory)).thenReturn(dto);

        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("Phụ kiện");

        CategoryResponseDto result = categoryService.updateCategory(1L, req);
        assertThat(result.getName()).isEqualTo("Phụ kiện");
        assertThat(activeCategory.getName()).isEqualTo("Phụ kiện");
    }

    @Test
    @DisplayName("updateCategory() - name blank → không update name")
    void updateCategory_blankName_doesNotUpdateName() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        CategoryResponseDto dto = new CategoryResponseDto();
        when(categoryMapper.toResponse(activeCategory)).thenReturn(dto);

        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("  "); // blank

        categoryService.updateCategory(1L, req);
        assertThat(activeCategory.getName()).isEqualTo("Áo"); // không đổi
    }

    // ─── deleteCategory() ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory() - không tìm thấy → throw CATEGORY_NOT_FOUND")
    void deleteCategory_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteCategory() - thành công: set status DELETED")
    void deleteCategory_success_setsDeleted() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));

        categoryService.deleteCategory(1L);

        assertThat(activeCategory.getStatus()).isEqualTo(Status.DELETED);
    }

    // ─── restoreCategory() ───────────────────────────────────────────────────

    @Test
    @DisplayName("restoreCategory() - không tìm thấy tên → throw CATEGORY_NOT_FOUND")
    void restoreCategory_notFound_throwsException() {
        when(categoryRepository.findByName("GhostCat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.restoreCategory("GhostCat"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("restoreCategory() - category không phải DELETED → throw CATEGORY_NOT_FOUND")
    void restoreCategory_notDeleted_throwsException() {
        when(categoryRepository.findByName("Áo")).thenReturn(Optional.of(activeCategory));

        assertThatThrownBy(() -> categoryService.restoreCategory("Áo"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("restoreCategory() - thành công: set ACTIVE")
    void restoreCategory_success_setsActive() {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setName("Quần");
        when(categoryRepository.findByName("Quần")).thenReturn(Optional.of(deletedCategory));
        when(categoryMapper.toResponse(deletedCategory)).thenReturn(dto);

        CategoryResponseDto result = categoryService.restoreCategory("Quần");

        assertThat(deletedCategory.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(result.getName()).isEqualTo("Quần");
    }
}
