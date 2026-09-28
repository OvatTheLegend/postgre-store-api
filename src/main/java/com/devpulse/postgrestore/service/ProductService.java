package com.devpulse.postgrestore.service;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(UUID id);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getAllProducts(Pageable pageable, Boolean activeOnly);

    Page<ProductResponse> searchProducts(String query, Pageable pageable);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    ProductResponse adjustStock(UUID id, int quantityDelta);

    void deleteProduct(UUID id);
}