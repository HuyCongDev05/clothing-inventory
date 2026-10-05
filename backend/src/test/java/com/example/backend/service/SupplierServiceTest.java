package com.example.backend.service;

import com.example.backend.dto.request.SupplierRequestDto;
import com.example.backend.dto.response.SupplierResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.SupplierMapper;
import com.example.backend.model.Supplier;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.SupplierRepository;
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

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SupplierService Unit Tests")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier existingSupplier;

    @BeforeEach
    void setUp() {
        existingSupplier = new Supplier();
        existingSupplier.setId(1L);
        existingSupplier.setCode("NCC-260923-ABCD");
        existingSupplier.setName("Supplier A");
        existingSupplier.setEmail("supplier@example.com");
        existingSupplier.setPhone("0901111111");
        existingSupplier.setTaxCode("TAX001");
        existingSupplier.setStatus(Status.ACTIVE);
    }

    // ─── createSupplier() ────────────────────────────────────────────────────

    @Test
    @DisplayName("createSupplier() - email đã tồn tại → throw CONFLICT_SUPPLIER_EMAIL")
    void createSupplier_emailConflict_throwsException() {
        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("New Supplier");
        req.setEmail("taken@example.com");

        when(supplierRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_EMAIL);
    }

    @Test
    @DisplayName("createSupplier() - phone đã tồn tại → throw CONFLICT_SUPPLIER_PHONE")
    void createSupplier_phoneConflict_throwsException() {
        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("New Supplier");
        req.setPhone("0909999999");

        when(supplierRepository.existsByPhone("0909999999")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_PHONE);
    }

    @Test
    @DisplayName("createSupplier() - taxCode đã tồn tại → throw CONFLICT_SUPPLIER_TAX_CODE")
    void createSupplier_taxCodeConflict_throwsException() {
        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("New Supplier");
        req.setTaxCode("DUP-TAX");

        when(supplierRepository.existsByTaxCode("DUP-TAX")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_TAX_CODE);
    }

    @Test
    @DisplayName("createSupplier() - thành công: sinh code, lưu, trả về DTO")
    void createSupplier_success_returnsDto() {
        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("New Supplier");

        Supplier mappedSupplier = new Supplier();
        mappedSupplier.setName("New Supplier");
        SupplierResponseDto responseDto = new SupplierResponseDto();
        responseDto.setName("New Supplier");

        when(supplierMapper.toEntity(req)).thenReturn(mappedSupplier);
        // Mock existsByCode to return false (code doesn't exist yet)
        when(supplierRepository.existsByCode(anyString())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(mappedSupplier);
        when(supplierMapper.toResponse(mappedSupplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.createSupplier(req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Supplier");
        verify(supplierRepository).save(any(Supplier.class));
    }

    // ─── updateSupplier() ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSupplier() - không tìm thấy code → throw SUPPLIER_NOT_FOUND")
    void updateSupplier_notFound_throwsException() {
        when(supplierRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier("MISSING", new SupplierRequestDto()))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND);
    }

    @Test
    @DisplayName("updateSupplier() - phone mới trùng với supplier khác → throw CONFLICT_SUPPLIER_PHONE")
    void updateSupplier_phoneConflict_throwsException() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByPhone("0900000000")).thenReturn(true);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setPhone("0900000000");  // khác số cũ của existingSupplier

        assertThatThrownBy(() -> supplierService.updateSupplier("NCC-260923-ABCD", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_PHONE);
    }

    @Test
    @DisplayName("updateSupplier() - email mới trùng với supplier khác → throw CONFLICT_SUPPLIER_EMAIL")
    void updateSupplier_emailConflict_throwsException() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByEmail("other@example.com")).thenReturn(true);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setEmail("other@example.com");  // khác email cũ

        assertThatThrownBy(() -> supplierService.updateSupplier("NCC-260923-ABCD", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_EMAIL);
    }

    @Test
    @DisplayName("updateSupplier() - taxCode mới trùng với supplier khác → throw CONFLICT_SUPPLIER_TAX_CODE")
    void updateSupplier_taxCodeConflict_throwsException() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByTaxCode("TAX999")).thenReturn(true);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setTaxCode("TAX999");  // khác taxCode cũ

        assertThatThrownBy(() -> supplierService.updateSupplier("NCC-260923-ABCD", req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_SUPPLIER_TAX_CODE);
    }

    @Test
    @DisplayName("updateSupplier() - thành công: cập nhật các trường name, contactPerson, address, note, status")
    void updateSupplier_success_updatesFields() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));

        SupplierResponseDto responseDto = new SupplierResponseDto();
        responseDto.setName("Updated Supplier");
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("Updated Supplier");
        req.setContactPerson("New Contact");
        req.setAddress("123 Main St");
        req.setNote("Updated note");
        req.setStatus(Status.INACTIVE);

        SupplierResponseDto result = supplierService.updateSupplier("NCC-260923-ABCD", req);

        assertThat(result.getName()).isEqualTo("Updated Supplier");
        assertThat(existingSupplier.getName()).isEqualTo("Updated Supplier");
        assertThat(existingSupplier.getStatus()).isEqualTo(Status.INACTIVE);
    }

    @Test
    @DisplayName("updateSupplier() - phone giống cũ → không throw CONFLICT")
    void updateSupplier_samePhone_doesNotConflict() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByPhone("0901111111")).thenReturn(true); // trùng nhưng là của chính nó

        SupplierResponseDto responseDto = new SupplierResponseDto();
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setPhone("0901111111");  // chính xác phone cũ → không conflict

        SupplierResponseDto result = supplierService.updateSupplier("NCC-260923-ABCD", req);
        assertThat(result).isNotNull();
    }

    // ─── getSupplierById() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSupplierById() - không tồn tại → throw SUPPLIER_NOT_FOUND")
    void getSupplierById_notFound_throwsException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(999L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND);
    }

    @Test
    @DisplayName("getSupplierById() - thành công: trả về SupplierResponseDto")
    void getSupplierById_success_returnsDto() {
        SupplierResponseDto responseDto = new SupplierResponseDto();
        responseDto.setCode("NCC-260923-ABCD");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.getSupplierById(1L);
        assertThat(result.getCode()).isEqualTo("NCC-260923-ABCD");
    }

    // ─── deleteSupplier() ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSupplier() - không tìm thấy code → throw SUPPLIER_NOT_FOUND")
    void deleteSupplier_notFound_throwsException() {
        when(supplierRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deleteSupplier("MISSING"))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteSupplier() - thành công: set status DELETED")
    void deleteSupplier_success_setsDeleted() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));

        supplierService.deleteSupplier("NCC-260923-ABCD");

        assertThat(existingSupplier.getStatus()).isEqualTo(Status.DELETED);
    }

    // ─── getAllSuppliersSimpleList() ──────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliersSimpleList() - trả về danh sách")
    void getAllSuppliersSimpleList_returnsAll() {
        when(supplierRepository.findAll()).thenReturn(java.util.List.of(existingSupplier));
        when(supplierMapper.toSimpleResponse(existingSupplier))
                .thenReturn(new com.example.backend.dto.response.SupplierSimpleResponseDto());

        var result = supplierService.getAllSuppliersSimpleList();
        assertThat(result).hasSize(1);
    }

    // ─── updateSupplier() - same email and taxCode branches ───────────────────

    @Test
    @DisplayName("updateSupplier() - email giống cũ → không throw conflict")
    void updateSupplier_sameEmail_doesNotConflict() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByEmail("supplier@example.com")).thenReturn(true);

        SupplierResponseDto responseDto = new SupplierResponseDto();
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setEmail("supplier@example.com"); // trùng với email của chính nó

        SupplierResponseDto result = supplierService.updateSupplier("NCC-260923-ABCD", req);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateSupplier() - taxCode giống cũ → không throw conflict")
    void updateSupplier_sameTaxCode_doesNotConflict() {
        when(supplierRepository.findByCode("NCC-260923-ABCD")).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByTaxCode("TAX001")).thenReturn(true);

        SupplierResponseDto responseDto = new SupplierResponseDto();
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        SupplierRequestDto req = new SupplierRequestDto();
        req.setTaxCode("TAX001"); // trùng với taxCode của chính nó

        SupplierResponseDto result = supplierService.updateSupplier("NCC-260923-ABCD", req);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("createSupplier() - mã code sinh ra bị trùng lần đầu → vòng lặp sinh lại mã khác")
    void createSupplier_codeDuplicate_retriesAndSaves() {
        SupplierRequestDto req = new SupplierRequestDto();
        req.setName("Retry Supplier");

        Supplier mappedSupplier = new Supplier();
        mappedSupplier.setName("Retry Supplier");
        SupplierResponseDto responseDto = new SupplierResponseDto();

        when(supplierMapper.toEntity(req)).thenReturn(mappedSupplier);
        // Lần 1: existsByCode = true (trùng), Lần 2: existsByCode = false (hợp lệ)
        when(supplierRepository.existsByCode(anyString())).thenReturn(true, false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(mappedSupplier);
        when(supplierMapper.toResponse(mappedSupplier)).thenReturn(responseDto);

        SupplierResponseDto result = supplierService.createSupplier(req);
        assertThat(result).isNotNull();
        verify(supplierRepository, times(2)).existsByCode(anyString());
    }

    // ─── getAllSuppliers() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliers() - trả về danh sách phân trang")
    void getAllSuppliers_success_returnsPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Supplier> page = new org.springframework.data.domain.PageImpl<>(List.of(existingSupplier), pageable, 1);

        when(supplierRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);
        SupplierResponseDto responseDto = new SupplierResponseDto();
        when(supplierMapper.toResponse(existingSupplier)).thenReturn(responseDto);

        var result = supplierService.getAllSuppliers("supplier", Status.ACTIVE, pageable);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getAllSuppliers() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getAllSuppliers_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(supplierRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        supplierService.getAllSuppliers("keyword", Status.ACTIVE, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<Supplier>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(supplierRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<Supplier> spec = captor.getValue();

        jakarta.persistence.criteria.Root<Supplier> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);

        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathCode = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathEmail = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathPhone = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predNotDeleted = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("code")).thenReturn(pathCode);
        when(root.get("name")).thenReturn(pathName);
        when(root.get("email")).thenReturn(pathEmail);
        when(root.get("phone")).thenReturn(pathPhone);

        when(cb.notEqual(pathStatus, Status.DELETED)).thenReturn(predNotDeleted);
        when(cb.equal(pathStatus, Status.ACTIVE)).thenReturn(predStatusEqual);

        jakarta.persistence.criteria.Expression expLower = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(any())).thenReturn(expLower);
        jakarta.persistence.criteria.Predicate predLike = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(predLike);
        when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predOr);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predAnd);

        jakarta.persistence.criteria.Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        // Also test null keyword and null status
        supplierService.getAllSuppliers(null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(supplierRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<Supplier> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }
}
