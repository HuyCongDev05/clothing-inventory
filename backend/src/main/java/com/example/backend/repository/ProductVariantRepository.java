package com.example.backend.repository;

import com.example.backend.model.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(pv.quantityOnHand), 0) FROM ProductVariant pv")
    Long sumAllQuantityOnHand();

    // Đếm SKU theo phân khúc tồn kho: low < 20, safe 20-99, over >= 100
    @Query(value = """
            SELECT
                CASE
                    WHEN pv.quantity_on_hand < 20   THEN 'low'
                    WHEN pv.quantity_on_hand >= 100  THEN 'over'
                    ELSE                                  'safe'
                END                 AS segment,
                COUNT(*)            AS skuCount
            FROM product_variants pv
            WHERE pv.status != 'DELETED'
            GROUP BY segment
            """, nativeQuery = true)
    List<Object[]> countSkuByStockHealthSegment();

    // Top N biến thể có giá trị tồn kho cao nhất (quantity_on_hand * purchase_price)
    @Query(value = """
            SELECT
                pv.id                                              AS variantId,
                p.name                                             AS productName,
                pv.sku                                             AS sku,
                COALESCE(c.name, 'Chưa phân loại')                AS categoryName,
                pv.quantity_on_hand                                AS quantityOnHand,
                pv.purchase_price                                  AS purchasePrice,
                (pv.quantity_on_hand * pv.purchase_price)          AS totalValue,
                p.id                                               AS productId
            FROM product_variants pv
            INNER JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.status != 'DELETED'
              AND p.status  != 'DELETED'
              AND pv.quantity_on_hand > 0
            ORDER BY totalValue DESC
            LIMIT :topN
            """, nativeQuery = true)
    List<Object[]> findTopValueVariants(@Param("topN") int topN);
}
