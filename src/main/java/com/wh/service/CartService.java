
package com.wh.service;

import com.wh.model.CartItem;
import com.wh.repository.ProductRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

@Service
@SessionScope
public class CartService {

    private final ProductRepository productRepository;

    @Getter
    private final List<CartItem> items = new ArrayList<>();

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Добавить товар или увеличить количество, если он уже есть
    public void addProduct(Long productId) {
        if (productId == null) {
            return;
        }

        productRepository.findById(productId).ifPresent(product -> {
            if (product.getPrice() == null || product.getPrice() <= 0
                    || product.getStock() == null || product.getStock() <= 0) {
                return;
            }
            for (CartItem item : items) {
                if (item.getProduct().getId().equals(productId)) {
                    if (item.getQuantity() < product.getStock()) {
                        item.setQuantity(item.getQuantity() + 1);
                    }
                    return;
                }
            }

            items.add(new CartItem(product, 1));
        });
    }

    // Увеличить количество на 1
    public void increaseQuantity(Long productId) {
        if (productId == null) {
            return;
        }

        for (CartItem item : items) {
            if (item.getProduct().getId().equals(productId)) {
                Integer stock = item.getProduct().getStock();
                if (stock != null && item.getQuantity() < stock) {
                    item.setQuantity(item.getQuantity() + 1);
                }
                return;
            }
        }
    }

    // Уменьшить количество на 1; если оно станет 0, удалить товар
    public void decreaseQuantity(Long productId) {
        if (productId == null) {
            return;
        }

        items.removeIf(item -> {
            if (item.getProduct().getId().equals(productId)) {
                item.setQuantity(item.getQuantity() - 1);
                return item.getQuantity() <= 0;
            }
            return false;
        });
    }

    // Полностью удалить товар из корзины
    public void removeItem(Long productId) {
        if (productId == null) {
            return;
        }

        items.removeIf(item ->
                item.getProduct().getId().equals(productId)
        );
    }

    // Рассчитать общую стоимость корзины
    public double getTotalPrice() {
        double total = 0;

        for (CartItem item : items) {
            Double price = item.getProduct().getPrice();
            if (price != null && price > 0) {
                total += price * item.getQuantity();
            }
        }

        return total;
    }
}
