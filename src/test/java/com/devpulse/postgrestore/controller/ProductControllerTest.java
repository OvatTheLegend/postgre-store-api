package com.devpulse.postgrestore.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.exception.GlobalExceptionHandler;
import com.devpulse.postgrestore.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("POST /api/v1/products - Should return 201 Created with Location header on valid request")
    void shouldCreateProductSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                "PROD-LAPTOP-001",
                "Gaming Laptop",
                "High performance specs",
                new BigDecimal("1499.99"),
                10);

        ProductResponse response = new ProductResponse(
                id,
                "PROD-LAPTOP-001",
                "Gaming Laptop",
                "High performance specs",
                new BigDecimal("1499.99"),
                10,
                true,
                Instant.now(),
                Instant.now(),
                0L);

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/products/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("PROD-LAPTOP-001"))
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1499.99));
    }

    @Test
    @DisplayName("POST /api/v1/products - Should return 400 Bad Request ProblemDetail when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CreateProductRequest invalidRequest = new CreateProductRequest(
                "",
                "",
                "Description",
                new BigDecimal("-50.00"),
                -5);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request - Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Should return 200 OK when product exists")
    void shouldGetProductByIdWhenExists() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                id, "PROD-001", "Monitor", "4K", new BigDecimal("399.99"), 10, true, Instant.now(), Instant.now(), 0L);

        when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Should return 404 ProblemDetail when product not found")
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductById(id))
                .thenThrow(new com.devpulse.postgrestore.exception.ResourceNotFoundException(
                        "Product not found with id: " + id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("Product not found with id")));
    }

    @Test
    @DisplayName("GET /api/v1/products - Should return paginated products")
    void shouldGetAllProductsPaginated() throws Exception {
        ProductResponse product = new ProductResponse(
                UUID.randomUUID(), "SKU-1", "Item", "Desc", new BigDecimal("50.00"), 5, true, Instant.now(),
                Instant.now(), 0L);
        Page<ProductResponse> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productService.getAllProducts(any(Pageable.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Should update product and return 200 OK")
    void shouldUpdateProductSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        com.devpulse.postgrestore.dto.UpdateProductRequest updateRequest = new com.devpulse.postgrestore.dto.UpdateProductRequest(
                "Updated Laptop", "New Desc", new BigDecimal("1599.99"), 25, true);

        ProductResponse response = new ProductResponse(
                id, "PROD-001", "Updated Laptop", "New Desc", new BigDecimal("1599.99"), 25, true, Instant.now(),
                Instant.now(), 1L);

        when(productService.updateProduct(eq(id), any(com.devpulse.postgrestore.dto.UpdateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(1599.99));
    }

    @Test
    @DisplayName("PATCH /api/v1/products/{id}/stock - Should adjust stock and return 200 OK")
    void shouldAdjustStockSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                id, "PROD-001", "Item", "Desc", new BigDecimal("50.00"), 15, true, Instant.now(), Instant.now(), 1L);

        when(productService.adjustStock(id, 5)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/products/{id}/stock", id)
                .param("delta", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Should delete product and return 204 No Content")
    void shouldDeleteProductSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(productService, org.mockito.Mockito.times(1)).deleteProduct(id);
    }
}