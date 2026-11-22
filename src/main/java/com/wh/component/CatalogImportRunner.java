package com.wh.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.model.Product;
import com.wh.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Imports the user's inventory once and keeps later price/stock edits intact. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CatalogImportRunner implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CatalogImportRunner(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        ClassPathResource resource = new ClassPathResource("catalog-import.json");
        if (!resource.exists()) {
            return;
        }

        List<Product> pending = new ArrayList<>();
        int added = 0;
        int photoUpdates = 0;

        try (InputStream input = resource.getInputStream()) {
            JsonNode entries = objectMapper.readTree(input);
            if (entries == null || !entries.isArray()) {
                throw new IllegalStateException("catalog-import.json must contain a JSON array");
            }

            for (JsonNode entry : entries) {
                String importKey = text(entry, "importKey");
                String title = text(entry, "title");
                String category = text(entry, "category");
                if (importKey == null || title == null || category == null) {
                    throw new IllegalStateException("Catalog item is missing its key, title, or category");
                }

                Optional<Product> existing = productRepository.findFirstByImportKey(importKey);
                if (existing.isPresent()) {
                    Product product = existing.get();
                    boolean changed = false;
                    boolean stillUnpriced = product.getPrice() == null || product.getPrice() <= 0;
                    if (stillUnpriced) {
                        product.setTitle(title);
                        product.setCategory(category);
                        product.setArticle(text(entry, "article"));
                        product.setBarcode(text(entry, "barcode"));
                        product.setColor(text(entry, "color"));
                        product.setDescription(text(entry, "description"));
                        changed = true;
                    }
                    String imageUrl = text(entry, "imageUrl");
                    if (blank(product.getImageUrl()) && !blank(imageUrl)) {
                        product.setImageUrl(imageUrl);
                        photoUpdates++;
                        changed = true;
                    }
                    if (changed) {
                        pending.add(product);
                    }
                    continue;
                }

                Product product = new Product();
                product.setImportKey(importKey);
                product.setArticle(text(entry, "article"));
                product.setBarcode(text(entry, "barcode"));
                product.setColor(text(entry, "color"));
                product.setTitle(title);
                product.setCategory(category);
                product.setDescription(text(entry, "description"));
                product.setImageUrl(text(entry, "imageUrl"));
                product.setPrice(null);
                product.setOldPrice(null);
                product.setStock(0);
                product.setIsShowroom(false);
                pending.add(product);
                added++;
            }
        }

        if (!pending.isEmpty()) {
            productRepository.saveAll(pending);
        }
        System.out.printf("Каталог: добавлено %d товаров, добавлено фото %d.%n", added, photoUpdates);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String result = value.asText().trim();
        return result.isEmpty() ? null : result;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
