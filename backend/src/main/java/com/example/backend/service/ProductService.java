package com.example.backend.service;

import com.example.backend.dto.request.ProductCreateRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.ProductResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.model.Category;
import com.example.backend.model.Product;
import com.example.backend.model.ProductVariant;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public PageResponseDto<ProductResponseDto> getAllProducts(String keyword, Pageable pageable) {
        Page<Product> productPage;
        if (StringUtils.hasText(keyword)) {
            productPage = productRepository.search(keyword, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        Page<ProductResponseDto> dtoPage = productPage.map(productMapper::toResponse);
        return PageResponseDto.from(dtoPage);
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new InvalidException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request) {
        if (productRepository.existsByCode(request.getCode())) {
            throw new InvalidException(ErrorCode.CONFLICT_PRODUCT_CODE); // Cần thêm mã lỗi này
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new InvalidException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setStatus(Status.ACTIVE);

        // Tạo các phiên bản
        List<ProductVariant> variants = request.getVariants().stream().map(variantDto -> {
            if (variantRepository.existsBySku(variantDto.getSku())) {
                throw new InvalidException(ErrorCode.SKU_ALREADY_EXISTS, "SKU: " + variantDto.getSku());
            }
            return ProductVariant.builder()
                    .product(product)
                    .sku(variantDto.getSku())
                    .option1Value(variantDto.getOption1Value())
                    .option2Value(variantDto.getOption2Value())
                    .option3Value(variantDto.getOption3Value())
                    .purchasePrice(variantDto.getPurchasePrice())
                    .salePrice(variantDto.getSalePrice())
                    .quantityOnHand(0)
                    .status(Status.ACTIVE)
                    .build();
        }).collect(Collectors.toList());

        product.setVariants(variants);

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }
}