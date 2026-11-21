package com.wh.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderResponse(
        Long id,
        String status,
        BigDecimal total,
        BigDecimal deliveryCost,
        String paymentMethod,
        OffsetDateTime createdAt
) {}
