package com.example.backend.repository;

import com.example.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByCode(String code);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p")
    Long sumAllProduct();

    // Danh sách các mặt hàng sản phẩm tổng hợp kèm số lượng tồn kho và ảnh sản phẩm
    @Query(value = """
            SELECT
                p.id                                   AS productId,
                p.name                                 AS productName,
                p.code                                 AS productCode,
                COALESCE(c.name, 'Chưa phân loại')     AS categoryName,
                COALESCE(SUM(pv.quantity_on_hand), 0)  AS totalQuantity,
                p.image_url                            AS imageUrl
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id AND c.status != 'DELETED'
            LEFT JOIN product_variants pv ON pv.product_id = p.id AND pv.status != 'DELETED'
            WHERE p.status != 'DELETED'
            GROUP BY p.id, p.name, p.code, c.name, p.image_url
            ORDER BY totalQuantity DESC
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> findProductStockSummaries();

    // Tìm kiếm sản phẩm theo tên, danh mục hoặc mã sản phẩm kèm ảnh sản phẩm
    @Query(value = """
            SELECT
                p.id                                   AS productId,
                p.name                                 AS productName,
                p.code                                 AS productCode,
                COALESCE(c.name, 'Chưa phân loại')     AS categoryName,
                COALESCE(SUM(pv.quantity_on_hand), 0)  AS totalQuantity,
                p.image_url                            AS imageUrl
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id AND c.status != 'DELETED'
            LEFT JOIN product_variants pv ON pv.product_id = p.id AND pv.status != 'DELETED'
            WHERE p.status != 'DELETED'
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
            GROUP BY p.id, p.name, p.code, c.name, p.image_url
            ORDER BY totalQuantity DESC
            LIMIT 15
            """, nativeQuery = true)
    List<Object[]> searchProductStockSummaries(@Param("keyword") String keyword);
}
