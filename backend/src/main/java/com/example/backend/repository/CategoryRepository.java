package com.example.backend.repository;

import com.example.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    Optional<Category> findByName(String name);

    // Phân bổ tồn kho theo danh mục (chỉ tính product và variant chưa xóa)
    @Query(value = """
            SELECT
                c.id                                               AS categoryId,
                c.name                                             AS categoryName,
                COALESCE(SUM(pv.quantity_on_hand), 0)             AS totalQuantity,
                COALESCE(SUM(pv.quantity_on_hand * pv.purchase_price), 0) AS totalValue
            FROM categories c
            INNER JOIN products p   ON p.category_id = c.id   AND p.status  != 'DELETED'
            INNER JOIN product_variants pv ON pv.product_id = p.id AND pv.status != 'DELETED'
            WHERE c.status != 'DELETED'
            GROUP BY c.id, c.name
            HAVING COALESCE(SUM(pv.quantity_on_hand), 0) > 0
            ORDER BY totalQuantity DESC
            """, nativeQuery = true)
    List<Object[]> findCategoryStockDistribution();
}
