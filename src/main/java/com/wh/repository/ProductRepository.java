package com.wh.repository;

import com.wh.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Метод для поиска товаров по названию (title) или категории с игнорированием регистра
    List<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String title, String category);

}