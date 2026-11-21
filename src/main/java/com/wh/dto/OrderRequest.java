package com.wh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "Укажите имя")
        @Size(max = 120, message = "Имя слишком длинное")
        String customerName,

        @NotBlank(message = "Укажите телефон")
        @Pattern(regexp = "^[+0-9()\\-\\s]{10,20}$", message = "Некорректный номер телефона")
        String phone,

        @Email(message = "Некорректный e-mail")
        @Size(max = 160, message = "E-mail слишком длинный")
        String email,

        @Pattern(regexp = "^(pickup|courier)$", message = "Выберите способ доставки")
        String delivery,

        @Pattern(regexp = "^(card_on_delivery|online)$", message = "Выберите способ оплаты")
        String paymentMethod,

        @Size(max = 300, message = "Адрес слишком длинный")
        String address,

        @Size(max = 1000, message = "Комментарий слишком длинный")
        String comment,

        @NotEmpty(message = "Корзина пуста")
        @Size(max = 50, message = "Слишком много позиций в заказе")
        @Valid
        List<Item> items
) {
        // цену клиент не присылает: сервер берёт её из БД
        public record Item(
                @NotNull(message = "Не указан товар") Long productId,
                @Min(value = 1, message = "Количество должно быть не меньше 1")
                @Max(value = 99, message = "Количество должно быть не больше 99")
                int quantity
        ) {}
}
