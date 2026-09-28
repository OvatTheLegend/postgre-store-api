package com.devpulse.postgrestore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "SKU is required") @Size(max = 50, message = "SKU must not exceed 50 characters") String sku,

        @NotBlank(message = "Product name is required") @Size(max = 255, message = "Product name must not exceed 255 characters") String name,

        String description,

        @NotNull(message = "Price is required") @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative") BigDecimal price,

        @NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock quantity cannot be negative") Integer stockQuantity) {
}