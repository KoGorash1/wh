package com.wh.exception;

/** Ошибка в данных заказа (например, несуществующий товар). Отдаётся клиенту как HTTP 400. */
public class BadOrderException extends RuntimeException {
    public BadOrderException(String message) {
        super(message);
    }
}