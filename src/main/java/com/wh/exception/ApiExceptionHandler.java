package com.wh.exception;

import com.wh.controller.OrderController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Единый формат ошибок для фронтенда: {"message": "..."}
 * Обработчик действует только для OrderController.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = OrderController.class)
@SuppressWarnings("unused")
public class ApiExceptionHandler {

    /**
     * Ошибки валидации данных заказа.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> onValidation(
            MethodArgumentNotValidException ex) {

        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Некорректные данные заказа");

        log.warn("Order validation error: {}", msg);

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", msg));
    }

    /**
     * Ошибки чтения JSON и некорректного формата запроса.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> onUnreadable(
            HttpMessageNotReadableException ex) {

        log.warn("Invalid order request body: {}", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "Некорректный формат запроса"));
    }

    /**
     * Ошибки бизнес-логики заказа.
     */
    @ExceptionHandler(BadOrderException.class)
    public ResponseEntity<Map<String, String>> onBadOrder(
            BadOrderException ex) {

        log.warn("Bad order: {}", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Обработка остальных исключений.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> onOther(Exception ex) {

        log.error("Order API error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message",
                        "Не удалось оформить заказ. Попробуйте позже."
                ));
    }
}