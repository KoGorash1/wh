
package com.wh.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Setter
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Снимок на момент заказа: цена и название в каталоге потом могут измениться
    @Setter
    @Column(name = "product_title", nullable = false, length = 200)
    private String productTitle;

    @Setter
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Setter
    @Column(nullable = false)
    private int quantity;
}
