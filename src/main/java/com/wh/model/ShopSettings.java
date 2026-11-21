package com.wh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "shop_settings")
@Getter
@Setter
public class ShopSettings {
    @Id
    private Long id = 1L;

    @Column(length = 120)
    private String storeName = "Белый Дом";
    @Column(length = 32)
    private String phone = "+7 999 555-20-20";
    @Column(length = 160)
    private String email = "info@wh.ru";
    @Column(length = 300)
    private String showroomAddress = "Москва";
    private boolean courierEnabled = true;
    private boolean pickupEnabled = true;
    @Column(precision = 12, scale = 2)
    private BigDecimal deliveryPrice = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    private BigDecimal freeDeliveryThreshold = BigDecimal.ZERO;
    @Column(length = 120)
    private String deliveryTime = "1–3 рабочих дня";
    // Заказы фиксируют выбранный способ оплаты, но эквайринг в проекте не подключён.
    private boolean onlinePaymentEnabled = false;
    private boolean paymentOnDeliveryEnabled = true;
}
