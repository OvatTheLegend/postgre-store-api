package com.devpulse.postgrestore.controller;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.dto.UpdateProductRequest;
import com.devpulse.postgrestore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    // Constructor Injection
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) Boolean activeOnly) {
        Page<ProductResponse> page = productService.getAllProducts(pageable, activeOnly);
        return ResponseEntity.ok(page);
    }

    // 5. GET: Paginated Search by keyword -> 200 OK
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String query,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<ProductResponse> results = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(results);
    }

    // 6. PUT: Full Update -> 200 OK
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    // 7. PATCH: Stock Adjustment -> 200 OK
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> adjustStock(
            @PathVariable UUID id,
            @RequestParam int delta) {
        ProductResponse updated = productService.adjustStock(id, delta);
        return ResponseEntity.ok(updated);
    }

    // 8. DELETE: Delete Product -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}