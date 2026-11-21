
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
            for (CartItem item : items) {
                if (item.getProduct().getId().equals(productId)) {
                    item.setQuantity(item.getQuantity() + 1);
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
                item.setQuantity(item.getQuantity() + 1);
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
            total += item.getProduct().getPrice() * item.getQuantity();
        }

        return total;
    }
}
