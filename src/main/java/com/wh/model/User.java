package com.wh.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Защита от дубликатов в БД
    private String username;

    private String password;
    private String role; // "ADMIN", "STAFF" или "USER"

    private String fullName; // ФИО
    private String phone;    // телефон в виде +79991234567

    @Column(unique = true)
    private String email;    // почта (хранится в нижнем регистре)
}