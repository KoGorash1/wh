package com.wh.service;

import com.wh.dto.OrderRequest;
import com.wh.dto.OrderResponse;
import com.wh.exception.BadOrderException;
import com.wh.model.Order;
import com.wh.model.OrderItem;
import com.wh.model.Product;
import com.wh.model.ShopSettings;
import com.wh.repository.OrderRepository;
import com.wh.repository.ProductRepository;
import com.wh.repository.ShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ShopSettingsRepository settingsRepository;

    public OrderService(OrderRepository orders, ProductRepository products, ShopSettingsRepository settingsRepository) {
        this.orders = orders;
        this.products = products;
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest req) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        req.items().forEach(i -> quantities.merge(i.productId(), i.quantity(), Integer::sum));

        Map<Long, Product> found = products.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (found.size() != quantities.size()) {
            throw new BadOrderException("Некоторые товары больше недоступны. Обновите страницу и повторите заказ.");
        }

        String delivery = req.delivery() == null || req.delivery().isBlank()
                ? "pickup"
                : req.delivery().trim();

        ShopSettings settings = settingsRepository.findById(1L).orElseGet(ShopSettings::new);
        if ("courier".equals(delivery) && !settings.isCourierEnabled()) {
            throw new BadOrderException("Курьерская доставка сейчас недоступна");
        }
        if ("pickup".equals(delivery) && !settings.isPickupEnabled()) {
            throw new BadOrderException("Самовывоз сейчас недоступен");
        }

        String paymentMethod = req.paymentMethod() == null || req.paymentMethod().isBlank()
                ? "card_on_delivery"
                : req.paymentMethod();
        if ("online".equals(paymentMethod) && !settings.isOnlinePaymentEnabled()) {
            throw new BadOrderException("Онлайн-оплата пока недоступна");
        }
        if ("card_on_delivery".equals(paymentMethod) && !settings.isPaymentOnDeliveryEnabled()) {
            throw new BadOrderException("Оплата при получении сейчас недоступна");
        }

        String rawAddress = blankToNull(req.address());
        if ("courier".equals(delivery) && rawAddress == null) {
            throw new BadOrderException("Укажите адрес доставки");
        }
        String showroomAddress = settings.getShowroomAddress() == null || settings.getShowroomAddress().isBlank()
                ? "Самовывоз из шоурума" : settings.getShowroomAddress().trim();
        String finalAddress = rawAddress != null ? rawAddress : showroomAddress;

        Order order = new Order();
        order.setCustomerName(req.customerName().trim());
        order.setPhone(req.phone().trim());
        order.setEmail(blankToNull(req.email()));
        order.setAddress(finalAddress);
        order.setDeliveryType(delivery);
        order.setPaymentMethod(paymentMethod);
        order.setComment(blankToNull(req.comment()));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            Product p = found.get(e.getKey());
            if (p.getPrice() == null || p.getPrice() <= 0 || p.getStock() == null || p.getStock() <= 0) {
                throw new BadOrderException("Товар «" + p.getTitle() + "» пока недоступен к заказу");
            }
            BigDecimal unitPrice = BigDecimal.valueOf(p.getPrice());
            int qty = e.getValue();
            if (qty > 99) {
                throw new BadOrderException("Слишком большое количество товара «" + p.getTitle() + "»");
            }

            OrderItem item = new OrderItem();
            item.setProductId(p.getId());
            item.setProductTitle(p.getTitle());
            item.setUnitPrice(unitPrice);
            item.setQuantity(qty);
            order.addItem(item);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }
        BigDecimal deliveryCost = BigDecimal.ZERO;
        if ("courier".equals(delivery)) {
            BigDecimal threshold = settings.getFreeDeliveryThreshold() == null
                    ? BigDecimal.ZERO : settings.getFreeDeliveryThreshold();
            if (threshold.signum() <= 0 || total.compareTo(threshold) < 0) {
                deliveryCost = settings.getDeliveryPrice() == null
                        ? BigDecimal.ZERO : settings.getDeliveryPrice();
            }
        }
        order.setDeliveryCost(deliveryCost);
        order.setTotal(total.add(deliveryCost));

        Order saved = orders.save(order);
        return new OrderResponse(saved.getId(), saved.getStatus().name(), saved.getTotal(),
                saved.getDeliveryCost(), saved.getPaymentMethod(), saved.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
