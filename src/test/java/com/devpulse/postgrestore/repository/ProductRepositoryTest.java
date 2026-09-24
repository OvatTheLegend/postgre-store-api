package com.devpulse.postgrestore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.devpulse.postgrestore.config.JpaConfig;
import com.devpulse.postgrestore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find product by SKU when it exists in database")
    void shouldFindProductBySku() {
        Product laptop = new Product(
                "PROD-LAPTOP-001",
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("1299.99"),
                15);
        entityManager.persistAndFlush(laptop);

        Optional<Product> found = productRepository.findBySku("PROD-LAPTOP-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Gaming Laptop");
        assertThat(found.get().getPrice()).isEqualByComparingTo("1299.99");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty optional when SKU does not exist")
    void shouldReturnEmptyWhenSkuNotFound() {
        Optional<Product> found = productRepository.findBySku("NON-EXISTENT-SKU");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true for existing SKU and false for non-existing SKU")
    void shouldCheckSkuExistence() {
        Product phone = new Product("PROD-PHONE-001", "Smartphone Pro", "OLED display", new BigDecimal("899.99"), 30);
        entityManager.persistAndFlush(phone);

        assertThat(productRepository.existsBySku("PROD-PHONE-001")).isTrue();
        assertThat(productRepository.existsBySku("FAKE-SKU-999")).isFalse();
    }

    @Test
    @DisplayName("Should filter active products only with pagination")
    void shouldFilterActiveProductsWithPagination() {

        Product activeProduct = new Product("PROD-ACTIVE-001", "Active Laptop", "Available", new BigDecimal("1000.00"),
                10);
        Product inactiveProduct = new Product("PROD-INACTIVE-001", "Old Phone", "Discontinued",
                new BigDecimal("300.00"), 0);
        inactiveProduct.deactivate();

        entityManager.persist(activeProduct);
        entityManager.persist(inactiveProduct);
        entityManager.flush();

        Page<Product> activePage = productRepository.findByActiveTrue(PageRequest.of(0, 10));

        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().getFirst().getSku()).isEqualTo("PROD-ACTIVE-001");
        assertThat(activePage.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should filter products by price range and sort by price ascending")
    void shouldFilterByPriceRangeAndSort() {
        // --- ARRANGE: 3 products with different prices ---
        Product budget = new Product("SKU-BUDGET", "Budget Mouse", "Accessory", new BigDecimal("25.00"), 50);
        Product mid = new Product("SKU-MID", "Mid-range Keyboard", "Accessory", new BigDecimal("75.00"), 20);
        Product premium = new Product("SKU-PREMIUM", "Premium Monitor", "Display", new BigDecimal("450.00"), 5);

        entityManager.persist(budget);
        entityManager.persist(mid);
        entityManager.persist(premium);
        entityManager.flush();

        // --- ACT: Find products between $20 and $100, sorted by price ASC ---
        Page<Product> results = productRepository.findByPriceBetween(
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                PageRequest.of(0, 10, Sort.by("price").ascending()));

        // --- ASSERT: Budget ($25) and Mid ($75) are found in exact ascending order ---
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent().get(0).getPrice()).isEqualByComparingTo("25.00");
        assertThat(results.getContent().get(1).getPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Should search products by keyword in name or description case-insensitively")
    void shouldSearchByKeywordCaseInsensitively() {
        // --- ARRANGE: 2 products with keywords in different fields ---
        Product keyboard = new Product("SKU-KEYBOARD", "Mechanical Keyboard", "RGB backlit keys",
                new BigDecimal("120.00"), 10);
        Product headphones = new Product("SKU-HEADPHONES", "Studio Headphones",
                "Active noise cancelling with deep bass", new BigDecimal("200.00"), 15);

        entityManager.persist(keyboard);
        entityManager.persist(headphones);
        entityManager.flush();

        // --- ACT 1: Search keyword in name (lowercase search "mech") ---
        Page<Product> nameMatch = productRepository.searchByKeyword("mech", PageRequest.of(0, 10));

        // --- ACT 2: Search keyword in description (uppercase search "NOISE") ---
        Page<Product> descMatch = productRepository.searchByKeyword("NOISE", PageRequest.of(0, 10));

        // --- ASSERT ---
        assertThat(nameMatch.getContent()).hasSize(1);
        assertThat(nameMatch.getContent().getFirst().getSku()).isEqualTo("SKU-KEYBOARD");

        assertThat(descMatch.getContent()).hasSize(1);
        assertThat(descMatch.getContent().getFirst().getSku()).isEqualTo("SKU-HEADPHONES");
    }
}