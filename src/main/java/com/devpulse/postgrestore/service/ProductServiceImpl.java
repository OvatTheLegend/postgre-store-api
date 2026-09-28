package com.devpulse.postgrestore.service;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.dto.UpdateProductRequest;
import com.devpulse.postgrestore.entity.Product;
import com.devpulse.postgrestore.exception.DuplicateResourceException;
import com.devpulse.postgrestore.exception.InsufficientStockException;
import com.devpulse.postgrestore.exception.ResourceNotFoundException;
import com.devpulse.postgrestore.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.sku() + "' already exists");
        }

        Product product = new Product(
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity());

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(product -> ProductResponse.fromEntity(product))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable, Boolean activeOnly) {
        Page<Product> products = Boolean.TRUE.equals(activeOnly)
                ? productRepository.findByActiveTrue(pageable)
                : productRepository.findAll(pageable);

        return products.map(product -> ProductResponse.fromEntity(product));
    }

    @Override
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.searchByKeyword(query, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity());
        product.setActive(request.active());

        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(UUID id, int quantityDelta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (product.getStockQuantity() + quantityDelta < 0) {
            throw new InsufficientStockException(
                    "Cannot deduct " + Math.abs(quantityDelta) + " items. Available stock: "
                            + product.getStockQuantity());
        }

        product.adjustStock(quantityDelta);
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}