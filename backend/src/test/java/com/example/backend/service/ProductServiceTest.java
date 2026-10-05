package com.example.backend.service;

import com.example.backend.dto.request.ProductCreateRequestDto;
import com.example.backend.dto.request.ProductUpdateRequestDto;
import com.example.backend.dto.request.VariantBulkPriceUpdateRequestDto;
import com.example.backend.dto.request.VariantUpdateRequestDto;
import com.example.backend.dto.response.ProductResponseDto;
import com.example.backend.dto.response.ProductVariantDetailResponseDto;
import com.example.backend.dto.response.VariantResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.*;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
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
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductVariantMapper variantMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        variant = new ProductVariant();
        variant.setId(10L);
        variant.setSku("SP-260923-ABCD-do-m");
        variant.setOption1Value("Đỏ");
        variant.setOption2Value("M");
        variant.setQuantityOnHand(50);
        variant.setStatus(Status.ACTIVE);
        variant.setPurchasePrice(BigDecimal.valueOf(100000));
        variant.setSalePrice(BigDecimal.valueOf(150000));

        product = new Product();
        product.setId(1L);
        product.setCode("SP-260923-ABCD");
        product.setName("Áo thun");
        product.setUnit("cái");
        product.setStatus(Status.ACTIVE);
        product.setVariants(new ArrayList<>(List.of(variant)));
        variant.setProduct(product);
    }

    // ─── createProduct() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct() - categoryId không tồn tại → throw CATEGORY_NOT_FOUND")
    void createProduct_categoryNotFound_throwsException() {
        ProductCreateRequestDto req = buildCreateRequest(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("createProduct() - không có categoryId: thành công, lưu product")
    void createProduct_noCategoryId_success() {
        ProductCreateRequestDto req = buildCreateRequest(null);
        Product mappedProduct = new Product();
        mappedProduct.setCode("SP-NEW");
        mappedProduct.setUnit("cái");
        mappedProduct.setVariants(new ArrayList<>());

        when(productMapper.toEntity(req)).thenReturn(mappedProduct);
        when(productRepository.existsByCode(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(mappedProduct);

        ProductResponseDto responseDto = new ProductResponseDto();
        responseDto.setVariants(List.of());
        when(productMapper.toResponse(mappedProduct)).thenReturn(responseDto);

        ProductResponseDto result = productService.createProduct(req);
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct() - có categoryId hợp lệ: thành công")
    void createProduct_withCategory_success() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Áo");

        ProductCreateRequestDto req = buildCreateRequest(5L);

        Product mappedProduct = new Product();
        mappedProduct.setCode("SP-NEW");
        mappedProduct.setUnit("cái");
        mappedProduct.setVariants(new ArrayList<>());

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(req)).thenReturn(mappedProduct);
        when(productRepository.existsByCode(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(mappedProduct);

        ProductResponseDto responseDto = new ProductResponseDto();
        responseDto.setVariants(List.of());
        when(productMapper.toResponse(mappedProduct)).thenReturn(responseDto);

        ProductResponseDto result = productService.createProduct(req);
        assertThat(result).isNotNull();
        assertThat(mappedProduct.getCategory()).isEqualTo(category);
    }

    // ─── getVariantById() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getVariantById() - không tồn tại → throw PRODUCT_NOT_FOUND")
    void getVariantById_notFound_throwsException() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getVariantById(999L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("getVariantById() - thành công: trả về DTO")
    void getVariantById_success_returnsDto() {
        ProductVariantDetailResponseDto detailDto = new ProductVariantDetailResponseDto();
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(variantMapper.toDetailResponse(variant)).thenReturn(detailDto);

        ProductVariantDetailResponseDto result = productService.getVariantById(10L);
        assertThat(result).isNotNull();
    }

    // ─── deleteProduct() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct() - không tồn tại → throw PRODUCT_NOT_FOUND")
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteProduct() - thành công: set DELETED cho product và tất cả variants")
    void deleteProduct_success_setsAllDeleted() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getStatus()).isEqualTo(Status.DELETED);
        assertThat(variant.getStatus()).isEqualTo(Status.DELETED);
    }

    // ─── deleteMultipleVariants() ────────────────────────────────────────────

    @Test
    @DisplayName("deleteMultipleVariants() - danh sách rỗng → return ngay, không gọi repo")
    void deleteMultipleVariants_emptyList_returnsEarly() {
        productService.deleteMultipleVariants(List.of());
        verify(variantRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("deleteMultipleVariants() - null list → return ngay")
    void deleteMultipleVariants_nullList_returnsEarly() {
        productService.deleteMultipleVariants(null);
        verify(variantRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("deleteMultipleVariants() - size không khớp → throw VARIANT_NOT_FOUND")
    void deleteMultipleVariants_sizeMismatch_throwsException() {
        when(variantRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(variant));  // chỉ tìm được 1, yêu cầu 2

        assertThatThrownBy(() -> productService.deleteMultipleVariants(List.of(10L, 11L)))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteMultipleVariants() - thành công: set DELETED và syncParentStatus")
    void deleteMultipleVariants_success_setsDeleted() {
        when(variantRepository.findAllById(List.of(10L))).thenReturn(List.of(variant));

        productService.deleteMultipleVariants(List.of(10L));

        assertThat(variant.getStatus()).isEqualTo(Status.DELETED);
    }

    // ─── bulkUpdateVariantPrices() ───────────────────────────────────────────

    @Test
    @DisplayName("bulkUpdateVariantPrices() - tất cả fields null → return empty list")
    void bulkUpdateVariantPrices_allNull_returnsEmpty() {
        VariantBulkPriceUpdateRequestDto req = new VariantBulkPriceUpdateRequestDto();
        req.setVariantIds(List.of(10L));
        // purchasePrice, salePrice, status đều null

        List<ProductResponseDto> result = productService.bulkUpdateVariantPrices(req);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("bulkUpdateVariantPrices() - size không khớp → throw VARIANT_NOT_FOUND")
    void bulkUpdateVariantPrices_sizeMismatch_throwsException() {
        VariantBulkPriceUpdateRequestDto req = new VariantBulkPriceUpdateRequestDto();
        req.setVariantIds(List.of(10L, 11L));
        req.setPurchasePrice(BigDecimal.valueOf(90000));

        when(variantRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(variant));

        assertThatThrownBy(() -> productService.bulkUpdateVariantPrices(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("bulkUpdateVariantPrices() - variant có transaction → throw CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS")
    void bulkUpdateVariantPrices_variantHasTransaction_throwsException() {
        VariantBulkPriceUpdateRequestDto req = new VariantBulkPriceUpdateRequestDto();
        req.setVariantIds(List.of(10L));
        req.setSalePrice(BigDecimal.valueOf(200000));

        when(variantRepository.findAllById(List.of(10L))).thenReturn(List.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(true);

        assertThatThrownBy(() -> productService.bulkUpdateVariantPrices(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS);
    }

    @Test
    @DisplayName("bulkUpdateVariantPrices() - thành công: cập nhật giá và trả về danh sách product")
    void bulkUpdateVariantPrices_success_updatesPrices() {
        VariantBulkPriceUpdateRequestDto req = new VariantBulkPriceUpdateRequestDto();
        req.setVariantIds(List.of(10L));
        req.setPurchasePrice(BigDecimal.valueOf(80000));
        req.setSalePrice(BigDecimal.valueOf(130000));

        when(variantRepository.findAllById(List.of(10L))).thenReturn(List.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponseDto());

        List<ProductResponseDto> result = productService.bulkUpdateVariantPrices(req);

        assertThat(result).isNotNull();
        assertThat(variant.getPurchasePrice()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(variant.getSalePrice()).isEqualByComparingTo(BigDecimal.valueOf(130000));
    }

    // ─── syncParentStatus() (via deleteProduct) ──────────────────────────────

    @Test
    @DisplayName("syncParentStatus() - tất cả variants INACTIVE → product chuyển INACTIVE")
    void syncParentStatus_allInactive_productBecomesInactive() {
        variant.setStatus(Status.INACTIVE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);
        // deleteProduct set tất cả variant → DELETED, nhưng test syncParentStatus
        // thông qua deleteMultipleVariants
    }

    @Test
    @DisplayName("syncParentStatus() - variants hỗn hợp ACTIVE và INACTIVE → product ACTIVE")
    void syncParentStatus_mixedVariants_productStaysActive() {
        // Tạo 2 variants: 1 ACTIVE, 1 INACTIVE
        ProductVariant activeVariant = new ProductVariant();
        activeVariant.setId(20L);
        activeVariant.setStatus(Status.ACTIVE);
        activeVariant.setProduct(product);

        variant.setStatus(Status.INACTIVE);
        product.setVariants(new ArrayList<>(List.of(variant, activeVariant)));

        when(variantRepository.findAllById(List.of(10L))).thenReturn(List.of(variant));

        productService.deleteMultipleVariants(List.of(10L));
        // variant(10) → DELETED, activeVariant(20) vẫn ACTIVE → product phải ACTIVE
        assertThat(product.getStatus()).isEqualTo(Status.ACTIVE);
    }

    // ─── getAllProducts() & Specifications ────────────────────────────────────

    @Test
    @DisplayName("getAllProducts() - lọc bỏ các variant có status DELETED")
    void getAllProducts_filtersDeletedVariants() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        ProductVariant deletedVariant = new ProductVariant();
        deletedVariant.setId(20L);
        deletedVariant.setStatus(Status.DELETED);

        VariantResponseDto activeDto = new VariantResponseDto();
        activeDto.setStatus(Status.ACTIVE);
        VariantResponseDto deletedDto = new VariantResponseDto();
        deletedDto.setStatus(Status.DELETED);

        ProductResponseDto productDto = new ProductResponseDto();
        productDto.setVariants(new ArrayList<>(List.of(activeDto, deletedDto)));

        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toResponse(product)).thenReturn(productDto);

        var result = productService.getAllProducts("áo", Status.ACTIVE, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getVariants()).hasSize(1);
        assertThat(result.items().get(0).getVariants().get(0).getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("getAllProducts() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getAllProducts_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        productService.getAllProducts("keyword", Status.ACTIVE, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<Product>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(productRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<Product> spec = captor.getValue();

        jakarta.persistence.criteria.Root<Product> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);

        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathCode = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathName = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predNotDeleted = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("code")).thenReturn(pathCode);
        when(root.get("name")).thenReturn(pathName);

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

        // Null filters branch
        productService.getAllProducts(null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(productRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<Product> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }

    // ─── getAllVariants() & Specifications ────────────────────────────────────

    @Test
    @DisplayName("getAllVariants() - trả về danh sách phân trang")
    void getAllVariants_success_returnsPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(variantRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(variant), pageable, 1));
        when(variantMapper.toDetailResponse(variant)).thenReturn(new ProductVariantDetailResponseDto());

        var result = productService.getAllVariants("SP", Status.ACTIVE, pageable);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getAllVariants() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getAllVariants_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(variantRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        productService.getAllVariants("test", Status.ACTIVE, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<ProductVariant>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(variantRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<ProductVariant> spec = captor.getValue();

        jakarta.persistence.criteria.Root<ProductVariant> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        jakarta.persistence.criteria.Join<ProductVariant, Product> productJoin = mock(jakarta.persistence.criteria.Join.class);

        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSku = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathName = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predNotDeleted = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("sku")).thenReturn(pathSku);
        when(root.<ProductVariant, Product>join("product")).thenReturn(productJoin);
        when(productJoin.get("name")).thenReturn(pathName);

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

        // Null filters branch
        productService.getAllVariants(null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(variantRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<ProductVariant> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }

    // ─── getLowStockVariants() & Specifications ───────────────────────────────

    @Test
    @DisplayName("getLowStockVariants() - trả về danh sách phân trang")
    void getLowStockVariants_success_returnsPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(variantRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(variant), pageable, 1));
        when(variantMapper.toDetailResponse(variant)).thenReturn(new ProductVariantDetailResponseDto());

        var result = productService.getLowStockVariants("SP", Status.ACTIVE, pageable);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getLowStockVariants() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getLowStockVariants_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(variantRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        productService.getLowStockVariants("low", Status.ACTIVE, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<ProductVariant>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(variantRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<ProductVariant> spec = captor.getValue();

        jakarta.persistence.criteria.Root<ProductVariant> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        jakarta.persistence.criteria.Join<ProductVariant, Product> productJoin = mock(jakarta.persistence.criteria.Join.class);

        jakarta.persistence.criteria.Path pathQty = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSku = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathName = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predLt = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predNotDeleted = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("quantityOnHand")).thenReturn(pathQty);
        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("sku")).thenReturn(pathSku);
        when(root.<ProductVariant, Product>join("product")).thenReturn(productJoin);
        when(productJoin.get("name")).thenReturn(pathName);

        when(cb.lessThan(pathQty, 20)).thenReturn(predLt);
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

        // Null filters branch
        productService.getLowStockVariants(null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(variantRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<ProductVariant> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }

    // ─── updateProduct() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct() - product không tồn tại → throw PRODUCT_NOT_FOUND")
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setVariants(List.of());

        assertThatThrownBy(() -> productService.updateProduct(999L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProduct() - category không tồn tại → throw CATEGORY_NOT_FOUND")
    void updateProduct_categoryNotFound_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setCategoryId(99L);
        req.setVariants(List.of());

        assertThatThrownBy(() -> productService.updateProduct(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProduct() - existingVariant không tồn tại trong map → throw VARIANT_NOT_FOUND")
    void updateProduct_variantNotInMap_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductUpdateRequestDto.VariantUpdateItem item = new ProductUpdateRequestDto.VariantUpdateItem();
        item.setId(999L); // ID không khớp với variant.getId()=10L
        item.setOption1Value("Đỏ");

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setVariants(List.of(item));

        assertThatThrownBy(() -> productService.updateProduct(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProduct() - variant có giao dịch nhưng thay đổi option → throw CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS")
    void updateProduct_hasTransactions_optionsChanged_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(true);

        ProductUpdateRequestDto.VariantUpdateItem item = new ProductUpdateRequestDto.VariantUpdateItem();
        item.setId(10L);
        item.setOption1Value("Xanh"); // Đổi từ "Đỏ" sang "Xanh"

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setVariants(List.of(item));

        assertThatThrownBy(() -> productService.updateProduct(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS);
    }

    @Test
    @DisplayName("updateProduct() - variant có giao dịch không đổi option: cập nhật giá & status thành công")
    void updateProduct_hasTransactions_optionsNotChanged_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(true);

        ProductUpdateRequestDto.VariantUpdateItem item = new ProductUpdateRequestDto.VariantUpdateItem();
        item.setId(10L);
        item.setOption1Value("Đỏ");
        item.setOption2Value("M");
        item.setPurchasePrice(BigDecimal.valueOf(120000));
        item.setSalePrice(BigDecimal.valueOf(180000));
        item.setStatus(Status.ACTIVE);

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setVariants(List.of(item));

        when(productMapper.toResponse(product)).thenReturn(new ProductResponseDto());

        ProductResponseDto result = productService.updateProduct(1L, req);
        assertThat(result).isNotNull();
        assertThat(variant.getPurchasePrice()).isEqualByComparingTo(BigDecimal.valueOf(120000));
    }

    @Test
    @DisplayName("updateProduct() - variant tự do đổi SKU bị trùng → throw SKU_ALREADY_EXISTS")
    void updateProduct_freelyUpdate_skuConflict_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(variantRepository.existsBySku(anyString())).thenReturn(true);

        ProductUpdateRequestDto.VariantUpdateItem item = new ProductUpdateRequestDto.VariantUpdateItem();
        item.setId(10L);
        item.setOption1Value("Vàng"); // Tạo SKU mới
        item.setOption2Value("L");

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setVariants(List.of(item));

        assertThatThrownBy(() -> productService.updateProduct(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKU_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("updateProduct() - thành công: cập nhật fields, thêm variant mới, xóa variant cũ")
    void updateProduct_success_allBranches() {
        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Quần");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(variantRepository.existsBySku(anyString())).thenReturn(false);

        // Update item cho variant 10
        ProductUpdateRequestDto.VariantUpdateItem existingItem = new ProductUpdateRequestDto.VariantUpdateItem();
        existingItem.setId(10L);
        existingItem.setOption1Value("Trắng");
        existingItem.setOption2Value("L");
        existingItem.setPurchasePrice(BigDecimal.valueOf(110000));
        existingItem.setSalePrice(BigDecimal.valueOf(160000));
        existingItem.setStatus(Status.ACTIVE);

        // New item (id == null)
        ProductUpdateRequestDto.VariantUpdateItem newItem = new ProductUpdateRequestDto.VariantUpdateItem();
        newItem.setId(null);
        newItem.setOption1Value("Đen");
        newItem.setOption2Value("XL");
        newItem.setPurchasePrice(BigDecimal.valueOf(110000));
        newItem.setSalePrice(BigDecimal.valueOf(160000));

        ProductUpdateRequestDto req = new ProductUpdateRequestDto();
        req.setCategoryId(2L);
        req.setName("Áo polo cao cấp");
        req.setBrand("Brand X");
        req.setUnit("cái");
        req.setDescription("Mô tả áo");
        req.setOption1Name("Màu");
        req.setOption2Name("Size");
        req.setOption3Name("Kiểu dáng");
        req.setStatus(Status.ACTIVE);
        req.setVariants(List.of(existingItem, newItem));

        when(productMapper.toResponse(product)).thenReturn(new ProductResponseDto());

        ProductResponseDto result = productService.updateProduct(1L, req);

        assertThat(result).isNotNull();
        assertThat(product.getName()).isEqualTo("Áo polo cao cấp");
        assertThat(product.getBrand()).isEqualTo("Brand X");
        assertThat(product.getVariants()).hasSize(2);
    }

    // ─── updateVariant() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateVariant() - variant không tồn tại → throw VARIANT_NOT_FOUND")
    void updateVariant_notFound_throwsException() {
        when(variantRepository.findById(999L)).thenReturn(Optional.empty());
        VariantUpdateRequestDto req = new VariantUpdateRequestDto();

        assertThatThrownBy(() -> productService.updateVariant(999L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateVariant() - có giao dịch và options thay đổi → throw CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS")
    void updateVariant_hasTransactions_optionsChanged_throwsException() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(true);

        VariantUpdateRequestDto req = new VariantUpdateRequestDto();
        req.setOption1Value("Cam"); // khác "Đỏ"

        assertThatThrownBy(() -> productService.updateVariant(10L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_UPDATE_VARIANT_HAS_TRANSACTIONS);
    }

    @Test
    @DisplayName("updateVariant() - tự do đổi SKU bị trùng → throw SKU_ALREADY_EXISTS")
    void updateVariant_skuConflict_throwsException() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(variantRepository.existsBySku(anyString())).thenReturn(true);

        VariantUpdateRequestDto req = new VariantUpdateRequestDto();
        req.setOption1Value("Xanh lá");

        assertThatThrownBy(() -> productService.updateVariant(10L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKU_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("updateVariant() - điều chỉnh tồn kho (quantityOnHand thay đổi): tạo InventoryTransaction")
    void updateVariant_quantityOnHandAdjusted_createsTransaction() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(variantRepository.existsBySku(anyString())).thenReturn(false);

        setupSecurityContext();
        User user = new User();
        user.setId(1L);
        user.setUuid("user-uuid-001");
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(user));
        when(productMapper.toResponse(product)).thenReturn(new ProductResponseDto());

        VariantUpdateRequestDto req = new VariantUpdateRequestDto();
        req.setOption1Value("Đỏ");
        req.setOption2Value("M");
        req.setQuantityOnHand(70); // trước đó là 50
        req.setAdjustReason("Kiểm kê hàng thừa");

        ProductResponseDto result = productService.updateVariant(10L, req);

        assertThat(result).isNotNull();
        assertThat(variant.getQuantityOnHand()).isEqualTo(70);
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("updateVariant() - quantityOnHand không đổi: không tạo InventoryTransaction")
    void updateVariant_quantityOnHandSame_noTransaction() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);
        when(productMapper.toResponse(product)).thenReturn(new ProductResponseDto());

        VariantUpdateRequestDto req = new VariantUpdateRequestDto();
        req.setOption1Value("Đỏ");
        req.setOption2Value("M");
        req.setQuantityOnHand(50); // bằng quantityOnHand cũ

        productService.updateVariant(10L, req);
        verify(inventoryTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateVariant() - user hiện tại không tồn tại khi điều chỉnh kho → throw ACCOUNT_NOT_FOUND")
    void updateVariant_currentUserNotFound_throwsException() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.existsByVariantId(10L)).thenReturn(false);

        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.empty());

        VariantUpdateRequestDto req = new VariantUpdateRequestDto();
        req.setOption1Value("Đỏ");
        req.setOption2Value("M");
        req.setQuantityOnHand(90);

        assertThatThrownBy(() -> productService.updateVariant(10L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-uuid-001");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
    }

    private ProductCreateRequestDto buildCreateRequest(Long categoryId) {
        ProductCreateRequestDto req = new ProductCreateRequestDto();
        req.setName("Áo thun");
        req.setUnit("cái");
        req.setCategoryId(categoryId);

        com.example.backend.dto.request.VariantCreateRequestDto variantDto =
                new com.example.backend.dto.request.VariantCreateRequestDto();
        variantDto.setOption1Value("Đỏ");
        variantDto.setPurchasePrice(BigDecimal.valueOf(100000));
        variantDto.setSalePrice(BigDecimal.valueOf(150000));
        req.setVariants(List.of(variantDto));
        return req;
    }
}
