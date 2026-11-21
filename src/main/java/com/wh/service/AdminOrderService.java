package com.wh.service;

import com.wh.model.Order;
import com.wh.model.OrderItem;
import com.wh.model.OrderStatus;
import com.wh.model.Product;
import com.wh.repository.OrderRepository;
import com.wh.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter NUM_FMT =
            DateTimeFormatter.ofPattern("yyMMdd");

    private static final int LIMIT = 500;

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "NEW", "Новый",
            "CONFIRMED", "Подтверждён",
            "SHIPPED", "Отправлен",
            "DELIVERED", "Доставлен",
            "CANCELLED", "Отменён"
    );

    private final OrderRepository orders;
    private final ProductRepository products;

    public AdminOrderService(OrderRepository orders,
                             ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    public record StatusOption(String code, String label) {}

    public record Row(
            Long id,
            String number,
            String date,
            String customer,
            String phone,
            String total,
            String status,
            String statusLabel,
            int itemsCount
    ) {}

    public record Line(
            String title,
            String image,
            int quantity,
            String unitPrice,
            String sum
    ) {}

    public record Details(
            Long id,
            String number,
            String date,
            String status,
            String statusLabel,
            String customer,
            String phone,
            String email,
            String address,
            String deliveryType,
            String paymentMethod,
            String deliveryCost,
            String comment,
            List<Line> lines,
            String total
    ) {}

    public record Page(
            List<Row> rows,
            Details selected,
            List<StatusOption> statuses,
            int totalCount,
            int newCount,
            String sumAll,
            String avg
    ) {}

    @Transactional(readOnly = true)
    public Page load(String q, String statusFilter, Long selectedId) {

        List<Order> all = orders.findAll(
                        Sort.by(Sort.Direction.DESC, "id")
                ).stream()
                .limit(LIMIT)
                .toList();

        BigDecimal sum = BigDecimal.ZERO;
        int newCount = 0;

        for (Order o : all) {
            if (o.getTotal() != null) {
                sum = sum.add(o.getTotal());
            }

            if ("NEW".equals(statusCode(o))) {
                newCount++;
            }
        }

        String avg = all.isEmpty()
                ? "0 ₽"
                : money(sum.divide(
                BigDecimal.valueOf(all.size()),
                0,
                RoundingMode.HALF_UP
        ));

        String needle = q == null
                ? ""
                : q.trim().toLowerCase(Locale.ROOT);

        String statusNeedle = statusFilter == null
                ? ""
                : statusFilter.trim().toUpperCase(Locale.ROOT);

        List<Row> rows = all.stream()
                .filter(o -> statusNeedle.isEmpty()
                        || statusNeedle.equalsIgnoreCase(statusCode(o)))
                .filter(o -> needle.isEmpty() || matches(o, needle))
                .map(this::toRow)
                .toList();

        Details selected = null;

        if (selectedId != null) {
            selected = all.stream()
                    .filter(o -> Objects.equals(o.getId(), selectedId))
                    .findFirst()
                    .map(this::toDetails)
                    .orElse(null);
        }

        List<StatusOption> options = Arrays.stream(OrderStatus.values())
                .map(st -> new StatusOption(
                        st.name(),
                        statusLabel(st.name())
                ))
                .toList();

        return new Page(
                rows,
                selected,
                options,
                all.size(),
                newCount,
                money(sum),
                avg
        );
    }

    @Transactional
    public void updateStatus(Long id, String status) {

        if (id == null || status == null || status.isBlank()) {
            return;
        }

        OrderStatus st;

        try {
            st = OrderStatus.valueOf(
                    status.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            return;
        }

        orders.findById(id).ifPresent(o -> {
            o.setStatus(st);
            orders.save(o);
        });
    }

    private Row toRow(Order o) {
        String s = statusCode(o);

        int count = o.getItems() == null
                ? 0
                : o.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new Row(
                o.getId(),
                number(o),
                date(o),
                nz(o.getCustomerName()),
                nz(o.getPhone()),
                money(o.getTotal()),
                s.toLowerCase(Locale.ROOT),
                statusLabel(s),
                count
        );
    }

    private Details toDetails(Order o) {
        String s = statusCode(o);

        List<OrderItem> items = o.getItems() == null
                ? List.of()
                : o.getItems();

        Set<Long> ids = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Product> byId = products.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p,
                        (a, b) -> a
                ));

        List<Line> lines = new ArrayList<>();

        for (OrderItem it : items) {
            Product p = it.getProductId() == null
                    ? null
                    : byId.get(it.getProductId());

            BigDecimal unit = it.getUnitPrice() == null
                    ? BigDecimal.ZERO
                    : it.getUnitPrice();

            lines.add(new Line(
                    nz(it.getProductTitle()),
                    p != null ? p.getImageUrl() : null,
                    it.getQuantity(),
                    money(unit),
                    money(unit.multiply(
                            BigDecimal.valueOf(it.getQuantity())
                    ))
            ));
        }

        return new Details(
                o.getId(),
                number(o),
                date(o),
                s.toLowerCase(Locale.ROOT),
                statusLabel(s),
                nz(o.getCustomerName()),
                nz(o.getPhone()),
                nz(o.getEmail()),
                nz(o.getAddress()),
                nz(o.getDeliveryType()),
                nz(o.getPaymentMethod()),
                money(o.getDeliveryCost()),
                nz(o.getComment()),
                lines,
                money(o.getTotal())
        );
    }

    private boolean matches(Order o, String needle) {
        return (o.getId() + " "
                + number(o) + " "
                + nz(o.getCustomerName()) + " "
                + nz(o.getPhone()) + " "
                + nz(o.getEmail()))
                .toLowerCase(Locale.ROOT)
                .contains(needle);
    }

    private String statusCode(Order o) {
        return o.getStatus() == null
                ? ""
                : o.getStatus().name();
    }

    private String statusLabel(String code) {
        return STATUS_LABELS.getOrDefault(
                code,
                code.isEmpty() ? "—" : code
        );
    }

    private String number(Order o) {
        LocalDateTime dt = created(o);

        String seq = String.format(
                "%03d",
                o.getId() == null ? 0 : o.getId()
        );

        return dt == null
                ? "БД-" + seq
                : "БД-" + dt.format(NUM_FMT) + "-" + seq;
    }

    private String date(Order o) {
        LocalDateTime dt = created(o);

        return dt == null
                ? "—"
                : dt.format(DATE_FMT);
    }

    private LocalDateTime created(Order o) {
        OffsetDateTime createdAt = o.getCreatedAt();

        if (createdAt == null) {
            return null;
        }

        return createdAt
                .atZoneSameInstant(ZONE)
                .toLocalDateTime();
    }

    private static String money(BigDecimal v) {
        if (v == null) {
            v = BigDecimal.ZERO;
        }

        DecimalFormatSymbols sym =
                new DecimalFormatSymbols(Locale.ROOT);

        sym.setGroupingSeparator(' ');

        return new DecimalFormat("#,##0", sym).format(v) + " ₽";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
