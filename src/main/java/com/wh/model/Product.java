package com.wh.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String article;

    @Column(length = 32)
    private String barcode;

    @Column(length = 200)
    private String color;

    @JsonIgnore
    @Column(length = 512, unique = true)
    private String importKey;

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
