package com.devpulse.postgrestore.dto;

import com.devpulse.postgrestore.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version) {

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion());
    }
}