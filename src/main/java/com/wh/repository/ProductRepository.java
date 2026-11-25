package com.wh.repository;

import com.wh.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Метод для поиска товаров по названию (title) или категории с игнорированием регистра
    List<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrArticleContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
            String title, String category, String article, String barcode);

    Optional<Product> findFirstByImportKey(String importKey);

    Optional<Product> findFirstByTitleAndCategory(String title, String category);

}
