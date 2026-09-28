package com.devpulse.postgrestore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.entity.Product;
import com.devpulse.postgrestore.exception.DuplicateResourceException;
import com.devpulse.postgrestore.exception.InsufficientStockException;
import com.devpulse.postgrestore.exception.ResourceNotFoundException;
import com.devpulse.postgrestore.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Should create product successfully when SKU is unique")
    void shouldCreateProductSuccessfully() {
        CreateProductRequest request = new CreateProductRequest(
                "PROD-LAPTOP-001",
                "Gaming Laptop",
                "High-end specs",
                new BigDecimal("1499.99"),
                20);

        when(productRepository.existsBySku("PROD-LAPTOP-001")).thenReturn(false);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0);
            productToSave.setId(UUID.randomUUID());
            return productToSave;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.sku()).isEqualTo("PROD-LAPTOP-001");
        assertThat(response.name()).isEqualTo("Gaming Laptop");
        assertThat(response.stockQuantity()).isEqualTo(20);

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when SKU already exists")
    void shouldThrowExceptionWhenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "EXISTING-SKU",
                "Another Product",
                "Description",
                new BigDecimal("99.99"),
                5);

        when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get product by ID when it exists")
    void shouldGetProductByIdWhenExists() {
        UUID id = UUID.randomUUID();
        Product product = new Product("PROD-001", "Monitor", "4K Display", new BigDecimal("399.99"), 10);
        product.setId(id);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(id);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Monitor");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product ID is not found")
    void shouldThrowExceptionWhenProductIdNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id");
    }

    @Test
    @DisplayName("Should adjust stock quantity successfully")
    void shouldAdjustStockSuccessfully() {
        UUID id = UUID.randomUUID();
        Product product = new Product("PROD-001", "Mouse", "Wireless", new BigDecimal("49.99"), 20);
        product.setId(id);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = productService.adjustStock(id, -5);

        assertThat(response.stockQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when deducting more stock than available")
    void shouldThrowExceptionWhenDeductingMoreStockThanAvailable() {
        UUID id = UUID.randomUUID();
        Product product = new Product("PROD-001", "Mouse", "Wireless", new BigDecimal("49.99"), 5);
        product.setId(id);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.adjustStock(id, -10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Cannot deduct 10 items. Available stock: 5");
    }

    @Test
    @DisplayName("Should delete product successfully when it exists")
    void shouldDeleteProductWhenExists() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(productRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent product")
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any(UUID.class));
    }
}