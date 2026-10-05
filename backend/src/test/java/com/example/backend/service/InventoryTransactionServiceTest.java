package com.example.backend.service;

import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.TransactionResponseDto;
import com.example.backend.mapper.InventoryTransactionMapper;
import com.example.backend.model.InventoryTransaction;
import com.example.backend.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventoryTransactionService Unit Tests")
class InventoryTransactionServiceTest {

    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private InventoryTransactionMapper transactionMapper;

    @InjectMocks
    private InventoryTransactionService inventoryTransactionService;

    // ─── searchTransactions() ─────────────────────────────────────────────────

    @Test
    @DisplayName("searchTransactions() - keyword null → dùng spec rỗng, trả về page")
    void searchTransactions_nullKeyword_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        InventoryTransaction tx = new InventoryTransaction();
        Page<InventoryTransaction> page = new PageImpl<>(List.of(tx), pageable, 1);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        TransactionResponseDto dto = new TransactionResponseDto();
        when(transactionMapper.toResponse(tx)).thenReturn(dto);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.searchTransactions(null, pageable);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("searchTransactions() - keyword blank → không thêm predicate, trả về all")
    void searchTransactions_blankKeyword_returnsAll() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<InventoryTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.searchTransactions("   ", pageable);

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("searchTransactions() - keyword có giá trị → gọi spec với keyword")
    void searchTransactions_withKeyword_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        InventoryTransaction tx = new InventoryTransaction();
        Page<InventoryTransaction> page = new PageImpl<>(List.of(tx), pageable, 1);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        TransactionResponseDto dto = new TransactionResponseDto();
        when(transactionMapper.toResponse(tx)).thenReturn(dto);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.searchTransactions("áo thun", pageable);

        assertThat(result.items()).hasSize(1);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("searchTransactions() - kết quả trống → trả về items rỗng")
    void searchTransactions_emptyResult_returnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.searchTransactions("nonexistent", pageable);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // ─── getHistoryByVariantId() ──────────────────────────────────────────────

    @Test
    @DisplayName("getHistoryByVariantId() - trả về page lịch sử của variant")
    void getHistoryByVariantId_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        InventoryTransaction tx = new InventoryTransaction();
        Page<InventoryTransaction> page = new PageImpl<>(List.of(tx), pageable, 1);

        when(transactionRepository.findByVariantIdOrderByCreatedAtDesc(10L, pageable)).thenReturn(page);
        TransactionResponseDto dto = new TransactionResponseDto();
        when(transactionMapper.toResponse(tx)).thenReturn(dto);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.getHistoryByVariantId(10L, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHistoryByVariantId() - variant không có lịch sử → trả về rỗng")
    void getHistoryByVariantId_noHistory_returnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(transactionRepository.findByVariantIdOrderByCreatedAtDesc(99L, pageable)).thenReturn(page);

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.getHistoryByVariantId(99L, pageable);

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("getHistoryByVariantId() - page metadata đúng (page, size, totalPages)")
    void getHistoryByVariantId_pageMetadataCorrect() {
        Pageable pageable = PageRequest.of(1, 5);
        InventoryTransaction tx = new InventoryTransaction();
        Page<InventoryTransaction> page = new PageImpl<>(List.of(tx), pageable, 11); // 11 total → 3 pages

        when(transactionRepository.findByVariantIdOrderByCreatedAtDesc(5L, pageable)).thenReturn(page);
        when(transactionMapper.toResponse(tx)).thenReturn(new TransactionResponseDto());

        PageResponseDto<TransactionResponseDto> result =
                inventoryTransactionService.getHistoryByVariantId(5L, pageable);

        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    @DisplayName("searchTransactions() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void searchTransactions_specificationPredicate_executed() {
        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        // Case 1: with keyword
        inventoryTransactionService.searchTransactions("test", pageable);
        org.mockito.ArgumentCaptor<Specification<InventoryTransaction>> captor =
                org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(transactionRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        Specification<InventoryTransaction> specWithKeyword = captor.getValue();

        jakarta.persistence.criteria.Root<InventoryTransaction> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        jakarta.persistence.criteria.Path pathVariant = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSku = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathProduct = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathCreatedAt = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Expression expLowerSku = mock(jakarta.persistence.criteria.Expression.class);
        jakarta.persistence.criteria.Expression expLowerName = mock(jakarta.persistence.criteria.Expression.class);
        jakarta.persistence.criteria.Predicate predSku = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predName = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Order order = mock(jakarta.persistence.criteria.Order.class);

        when(root.get("variant")).thenReturn(pathVariant);
        when(pathVariant.get("sku")).thenReturn(pathSku);
        when(pathVariant.get("product")).thenReturn(pathProduct);
        when(pathProduct.get("name")).thenReturn(pathName);
        when(root.get("createdAt")).thenReturn(pathCreatedAt);
        when(cb.lower(pathSku)).thenReturn(expLowerSku);
        when(cb.lower(pathName)).thenReturn(expLowerName);
        when(cb.like(eq(expLowerSku), anyString())).thenReturn(predSku);
        when(cb.like(eq(expLowerName), anyString())).thenReturn(predName);
        when(cb.or(predSku, predName)).thenReturn(predOr);
        when(cb.desc(pathCreatedAt)).thenReturn(order);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predAnd);

        jakarta.persistence.criteria.Predicate resultPred = specWithKeyword.toPredicate(root, query, cb);
        assertThat(resultPred).isNotNull();
        verify(query).orderBy(order);

        // Case 2: without keyword
        inventoryTransactionService.searchTransactions(null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(transactionRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        Specification<InventoryTransaction> specNoKeyword = captor.getValue();
        jakarta.persistence.criteria.Predicate resultPred2 = specNoKeyword.toPredicate(root, query, cb);
        assertThat(resultPred2).isNotNull();
    }
}

