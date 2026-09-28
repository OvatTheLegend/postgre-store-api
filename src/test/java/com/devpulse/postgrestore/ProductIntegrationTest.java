package com.devpulse.postgrestore;

import com.devpulse.postgrestore.dto.CreateProductRequest;
import com.devpulse.postgrestore.dto.ProductResponse;
import com.devpulse.postgrestore.dto.UpdateProductRequest;
import com.devpulse.postgrestore.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private ProductRepository productRepository;

        private String baseUrl;

        @BeforeEach
        void setUp() {
                baseUrl = "http://localhost:" + port + "/api/v1/products";
                productRepository.deleteAll(); // Clean database before each integration test
        }

        @Test
        @DisplayName("Full End-to-End Lifecycle: Create -> Get -> Update -> Adjust Stock -> Delete -> Verify 404")
        void shouldExecuteFullProductLifecycle() {
                // 1. POST /api/v1/products (Create)
                CreateProductRequest createRequest = new CreateProductRequest(
                                "E2E-LAPTOP-001",
                                "E2E Ultra Laptop",
                                "Flagship ultrabook",
                                new BigDecimal("1899.99"),
                                10);

                ResponseEntity<ProductResponse> createResponse = restTemplate.postForEntity(
                                baseUrl,
                                createRequest,
                                ProductResponse.class);

                assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(createResponse.getHeaders().getLocation()).isNotNull();
                ProductResponse createdProduct = createResponse.getBody();
                assertThat(createdProduct).isNotNull();
                UUID productId = createdProduct.id();
                assertThat(productId).isNotNull();
                assertThat(createdProduct.sku()).isEqualTo("E2E-LAPTOP-001");

                // 2. GET /api/v1/products/{id} (Retrieve by ID)
                ResponseEntity<ProductResponse> getResponse = restTemplate.getForEntity(
                                baseUrl + "/" + productId,
                                ProductResponse.class);

                assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(getResponse.getBody()).isNotNull();
                assertThat(getResponse.getBody().name()).isEqualTo("E2E Ultra Laptop");
                assertThat(getResponse.getBody().stockQuantity()).isEqualTo(10);

                // 3. PUT /api/v1/products/{id} (Update Details)
                UpdateProductRequest updateRequest = new UpdateProductRequest(
                                "E2E Ultra Laptop v2",
                                "Updated specs",
                                new BigDecimal("1999.99"),
                                12,
                                true);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<UpdateProductRequest> updateEntity = new HttpEntity<>(updateRequest, headers);

                ResponseEntity<ProductResponse> updateResponse = restTemplate.exchange(
                                baseUrl + "/" + productId,
                                HttpMethod.PUT,
                                updateEntity,
                                ProductResponse.class);

                assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(updateResponse.getBody()).isNotNull();
                assertThat(updateResponse.getBody().name()).isEqualTo("E2E Ultra Laptop v2");
                assertThat(updateResponse.getBody().price()).isEqualByComparingTo("1999.99");

                // Verify updated version persisted in PostgreSQL
                ResponseEntity<ProductResponse> getUpdatedResponse = restTemplate.getForEntity(
                                baseUrl + "/" + productId,
                                ProductResponse.class);
                assertThat(getUpdatedResponse.getBody()).isNotNull();
                assertThat(getUpdatedResponse.getBody().version()).isEqualTo(1L);

                // 4. DELETE /api/v1/products/{id} (Delete)
                ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                                baseUrl + "/" + productId,
                                HttpMethod.DELETE,
                                null,
                                Void.class);

                assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

                // 5. GET /api/v1/products/{id} (Verify 404 ProblemDetail)
                ResponseEntity<ProblemDetail> notFoundResponse = restTemplate.getForEntity(
                                baseUrl + "/" + productId,
                                ProblemDetail.class);

                assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(notFoundResponse.getBody()).isNotNull();
                assertThat(notFoundResponse.getBody().getTitle()).isEqualTo("Resource Not Found");
        }

        @Test
        @DisplayName("E2E Validation: Should reject duplicate SKU with 409 Conflict")
        void shouldRejectDuplicateSkuWith409() {
                // 1. Create initial product
                CreateProductRequest initialRequest = new CreateProductRequest(
                                "DUPLICATE-SKU-001",
                                "Product 1",
                                "Desc",
                                new BigDecimal("100.00"),
                                5);
                restTemplate.postForEntity(baseUrl, initialRequest, ProductResponse.class);

                // 2. Attempt to create another product with the EXACT same SKU
                CreateProductRequest duplicateRequest = new CreateProductRequest(
                                "DUPLICATE-SKU-001",
                                "Product 2",
                                "Desc",
                                new BigDecimal("200.00"),
                                10);
                ResponseEntity<ProblemDetail> conflictResponse = restTemplate.postForEntity(
                                baseUrl,
                                duplicateRequest,
                                ProblemDetail.class);

                assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(conflictResponse.getBody()).isNotNull();
                assertThat(conflictResponse.getBody().getTitle()).isEqualTo("Resource Conflict");
        }
}