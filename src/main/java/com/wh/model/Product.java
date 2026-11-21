package com.wh.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;
    private Double price;
    private Double oldPrice;
    private Integer width;
    private Integer stock = 10;
    private String kit;
    private String style;
    private String liquidationReason;
    private String badge;
    private String previousBadge;
    private String imageUrl;

    @Column(length = 1000)
    private String description;

    private Boolean isShowroom = false;
}
