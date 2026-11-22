package com.wh.controller;

import com.wh.model.Order;
import com.wh.model.Product;
import com.wh.model.ShopSettings;
import com.wh.model.User;
import com.wh.repository.OrderRepository;
import com.wh.repository.ProductRepository;
import com.wh.repository.ShopSettingsRepository;
import com.wh.repository.UserRepository;
import com.wh.service.PasswordPolicy;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
public class AdminController {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter ORDER_NUM_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    private static final Map<String, String> ORDER_STATUS_LABELS = Map.of(
            "NEW", "Новый",
            "CONFIRMED", "Подтверждён",
            "SHIPPED", "Отправлен",
            "DELIVERED", "Доставлен",
            "CANCELLED", "Отменён"
    );

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShopSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminController(ProductRepository productRepository,
                           OrderRepository orderRepository,
                           ShopSettingsRepository settingsRepository,
                           UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    public record CategorySummary(String name, long count) {
    }

    public record LiquidationRow(
            Long id,
            String title,
            String category,
            String imageUrl,
            double price,
            Double oldPrice,
            int discountPercent,
            String stockLabel,
            String reasonLabel
    ) {
    }

    public record ClientOrderSummary(
            Long id,
            String number,
            String date,
            String totalFormatted,
            String statusLabel
    ) {
    }

    public record ClientSummary(
            String key,
            String initials,
            String name,
            String phone,
            String email,
            String address,
            int ordersCount,
            BigDecimal totalSpent,
            String totalSpentFormatted,
            String avgCheckFormatted,
            String lastOrderDate,
            String searchKey,
            List<ClientOrderSummary> recentOrders
    ) {
    }

    private boolean isNotStaff(HttpSession session) {
        return session == null
                || !Boolean.TRUE.equals(session.getAttribute("isStaff"));
    }

    private boolean isAdmin(HttpSession session) {
        return session != null
                && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private List<CategorySummary> buildCategories(String filter) {
        String q = filter == null ? "" : filter.trim().toLowerCase();
        Map<String, Long> counts = new LinkedHashMap<>();

        for (Product p : productRepository.findAll()) {
            if (p.getCategory() == null) {
                continue;
            }
            String cat = p.getCategory().trim();
            if (cat.isEmpty()) {
                continue;
            }
            counts.merge(cat, 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .filter(e -> q.isEmpty() || e.getKey().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .map(e -> new CategorySummary(e.getKey(), e.getValue()))
                .toList();
    }

    private static Double parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9.,]", "").replace(',', '.').trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            double val = Double.parseDouble(cleaned);
            return val > 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isSaleProduct(Product p) {
        if (p == null) {
            return false;
        }
        if (p.getBadge() != null && p.getBadge().trim().equalsIgnoreCase("sale")) {
            return true;
        }
        return p.getPrice() != null && p.getOldPrice() != null && p.getOldPrice() > p.getPrice();
    }

    private static boolean isLiquidationProduct(Product p) {
        if (p == null) {
            return false;
        }
        return p.getBadge() != null && p.getBadge().trim().equalsIgnoreCase("liquidation");
    }

    private static int calcDiscountPercent(Product p) {
        if (p.getPrice() != null && p.getOldPrice() != null && p.getOldPrice() > p.getPrice() && p.getOldPrice() > 0) {
            return (int) Math.round(((p.getOldPrice() - p.getPrice()) / p.getOldPrice()) * 100.0);
        }
        return 0;
    }

    private static String extractStockLabel(Product p) {
        return (p.getStock() == null ? 10 : p.getStock()) + " шт.";
    }

    private static String extractReasonLabel(Product p) {
        if (p.getLiquidationReason() != null && !p.getLiquidationReason().isBlank()) {
            return p.getLiquidationReason().trim();
        }
        return "Вывод из коллекции";
    }

    private LiquidationRow toLiquidationRow(Product p) {
        return new LiquidationRow(
                p.getId(),
                p.getTitle() == null ? "Товар" : p.getTitle(),
                p.getCategory() == null ? "" : p.getCategory(),
                p.getImageUrl(),
                p.getPrice(),
                p.getOldPrice(),
                calcDiscountPercent(p),
                extractStockLabel(p),
                extractReasonLabel(p)
        );
    }

    private static String formatRub(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", sym).format(v) + " ₽";
    }

    private static String clientIdentityKey(Order o) {
        if (o.getEmail() != null && !o.getEmail().isBlank()) {
            return "email:" + o.getEmail().trim().toLowerCase(Locale.ROOT);
        }
        String digits = o.getPhone() == null ? "" : o.getPhone().replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            return "phone:" + digits;
        }
        if (o.getCustomerName() != null && !o.getCustomerName().isBlank()) {
            return "name:" + o.getCustomerName().trim().toLowerCase(Locale.ROOT);
        }
        return "order:" + o.getId();
    }

    private static String makeInitials(String name) {
        if (name == null || name.isBlank()) {
            return "КЛ";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        String s = parts[0];
        return s.substring(0, Math.min(2, s.length())).toUpperCase(Locale.ROOT);
    }

    private static String formatOrderNumber(Order o) {
        OffsetDateTime createdAt = o.getCreatedAt();
        LocalDateTime dt = createdAt == null ? null : createdAt.atZoneSameInstant(MOSCOW_ZONE).toLocalDateTime();
        String seq = String.format("%03d", o.getId() == null ? 0 : o.getId());
        return dt == null ? "БД-" + seq : "БД-" + dt.format(ORDER_NUM_FMT) + "-" + seq;
    }

    private static String formatOrderDate(Order o) {
        OffsetDateTime createdAt = o.getCreatedAt();
        if (createdAt == null) {
            return "—";
        }
        return createdAt.atZoneSameInstant(MOSCOW_ZONE).toLocalDateTime().format(ORDER_DATE_FMT);
    }

    private List<ClientSummary> buildClients(List<Order> sortedOrders) {
        record MutableClient(
                String key,
                String name,
                String phone,
                String email,
                String address,
                int ordersCount,
                BigDecimal totalSpent,
                String lastOrderDate,
                List<ClientOrderSummary> orders
        ) {}

        Map<String, MutableClient> map = new LinkedHashMap<>();

        for (Order o : sortedOrders) {
            String key = clientIdentityKey(o);
            BigDecimal orderTotal = o.getTotal() == null ? BigDecimal.ZERO : o.getTotal();
            String statusCode = o.getStatus() == null ? "" : o.getStatus().name();
            String statusLabel = ORDER_STATUS_LABELS.getOrDefault(statusCode, statusCode.isEmpty() ? "—" : statusCode);

            ClientOrderSummary item = new ClientOrderSummary(
                    o.getId(),
                    formatOrderNumber(o),
                    formatOrderDate(o),
                    formatRub(orderTotal),
                    statusLabel
            );

            MutableClient existing = map.get(key);
            if (existing == null) {
                String name = (o.getCustomerName() == null || o.getCustomerName().isBlank()) ? "Покупатель" : o.getCustomerName().trim();
                String phone = o.getPhone() == null ? "" : o.getPhone().trim();
                String email = o.getEmail() == null ? "" : o.getEmail().trim();
                String address = o.getAddress() == null ? "" : o.getAddress().trim();

                List<ClientOrderSummary> list = new ArrayList<>();
                list.add(item);

                map.put(key, new MutableClient(
                        key,
                        name,
                        phone,
                        email,
                        address,
                        1,
                        orderTotal,
                        formatOrderDate(o),
                        list
                ));
            } else {
                String name = existing.name();
                if ((name.equals("Покупатель") || name.isBlank()) && o.getCustomerName() != null && !o.getCustomerName().isBlank()) {
                    name = o.getCustomerName().trim();
                }
                String phone = existing.phone().isBlank() && o.getPhone() != null ? o.getPhone().trim() : existing.phone();
                String email = existing.email().isBlank() && o.getEmail() != null ? o.getEmail().trim() : existing.email();
                String address = existing.address().isBlank() && o.getAddress() != null ? o.getAddress().trim() : existing.address();

                List<ClientOrderSummary> list = new ArrayList<>(existing.orders());
                if (list.size() < 5) {
                    list.add(item);
                }

                map.put(key, new MutableClient(
                        key,
                        name,
                        phone,
                        email,
                        address,
                        existing.ordersCount() + 1,
                        existing.totalSpent().add(orderTotal),
                        existing.lastOrderDate(),
                        list
                ));
            }
        }

        List<ClientSummary> result = new ArrayList<>();
        for (MutableClient mc : map.values()) {
            BigDecimal avg = mc.ordersCount() > 0
                    ? mc.totalSpent().divide(BigDecimal.valueOf(mc.ordersCount()), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String searchKey = !mc.phone().isBlank() ? mc.phone() : (!mc.email().isBlank() ? mc.email() : mc.name());

            result.add(new ClientSummary(
                    mc.key(),
                    makeInitials(mc.name()),
                    mc.name(),
                    mc.phone(),
                    mc.email(),
                    mc.address(),
                    mc.ordersCount(),
                    mc.totalSpent(),
                    formatRub(mc.totalSpent()),
                    formatRub(avg),
                    mc.lastOrderDate(),
                    searchKey,
                    mc.orders()
            ));
        }

        result.sort(Comparator.comparing(ClientSummary::totalSpent).reversed());
        return result;
    }

    // Обзор / Главная админки
    @GetMapping("/admin")
    public String adminPanel(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<Product> products = keyword != null && !keyword.trim().isEmpty()
                ? productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrArticleContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), keyword.trim())
                : productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        addOverviewMetrics(model);

        model.addAttribute("newProduct", new Product());
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "overview");

        return "admin";
    }

    // Вкладка «Товары»
    @GetMapping("/admin/products")
    public String adminProducts(@RequestParam(value = "keyword", required = false) String keyword,
                                Model model, HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<Product> products = keyword != null && !keyword.trim().isEmpty()
                ? productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrArticleContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), keyword.trim())
                : productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "products");

        return "admin";
    }

    private void addOverviewMetrics(Model model) {
        List<Product> products = productRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("productCount", products.size());
        model.addAttribute("newOrderCount", orders.stream().filter(o -> o.getStatus() == com.wh.model.OrderStatus.NEW).count());
        model.addAttribute("promotionCount", products.stream().filter(AdminController::isSaleProduct).count());
        model.addAttribute("showroomCount", products.stream().filter(p -> Boolean.TRUE.equals(p.getIsShowroom())).count());
    }

    @GetMapping("/admin/products/new")
    public String newProductForm(Model model, HttpSession session) {
        if (isNotStaff(session)) return "redirect:/login";
        model.addAttribute("product", new Product());
        model.addAttribute("isNew", true);
        model.addAttribute("isAdmin", isAdmin(session));
        return "admin-edit";
    }

    // Вкладка «Категории»
    @GetMapping("/admin/categories")
    public String adminCategories(
            @RequestParam(value = "q", required = false) String q,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<CategorySummary> allCategories = buildCategories(null);
        List<CategorySummary> filteredCategories = buildCategories(q);
        long totalProducts = productRepository.count();

        model.addAttribute("categories", filteredCategories);
        model.addAttribute("totalCategoriesCount", allCategories.size());
        model.addAttribute("totalProductsCount", totalProducts);
        model.addAttribute("categoryQuery", q == null ? "" : q);
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "categories");

        return "admin";
    }

    // Переименование категории у всех товаров
    @PostMapping("/admin/categories/rename")
    public String renameCategory(
            @RequestParam("oldName") String oldName,
            @RequestParam("newName") String newName,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        String from = oldName == null ? "" : oldName.trim();
        String to = newName == null ? "" : newName.trim();

        if (!from.isEmpty() && !to.isEmpty() && !from.equals(to)) {
            List<Product> matching = productRepository.findAll().stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().trim().equals(from))
                    .toList();
            for (Product p : matching) {
                p.setCategory(to);
            }
            if (!matching.isEmpty()) {
                productRepository.saveAll(matching);
            }
        }

        return "redirect:/admin/categories";
    }

    // Вкладка «Акции»
    @GetMapping("/admin/promotions")
    public String adminPromotions(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "saved", required = false) String saved,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<Product> allProducts = productRepository.findAll();
        List<Product> saleProducts = allProducts.stream()
                .filter(AdminController::isSaleProduct)
                .toList();

        Product selectedProduct = null;
        if (productId != null) {
            selectedProduct = allProducts.stream()
                    .filter(p -> productId.equals(p.getId()))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedProduct == null && !allProducts.isEmpty()) {
            selectedProduct = allProducts.get(0);
        }

        model.addAttribute("products", allProducts);
        model.addAttribute("saleProducts", saleProducts);
        model.addAttribute("selectedProduct", selectedProduct);
        model.addAttribute("promoSaved", "1".equals(saved));
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "promotions");

        return "admin";
    }

    // Запуск / сохранение акции для товара
    @PostMapping("/admin/promotions/launch")
    public String launchPromotion(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "productTitle", required = false) String productTitle,
            @RequestParam(value = "newPrice", required = false) String newPriceRaw,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        Optional<Product> found = Optional.empty();
        if (productId != null) {
            found = productRepository.findById(productId);
        }
        if (found.isEmpty() && productTitle != null && !productTitle.isBlank()) {
            String titleTrim = productTitle.trim();
            found = productRepository.findAll().stream()
                    .filter(p -> p.getTitle() != null && p.getTitle().equalsIgnoreCase(titleTrim))
                    .findFirst();
        }

        if (found.isEmpty()) return "redirect:/admin/promotions?error=product";
        Product product = found.get();
        if ("liquidation".equalsIgnoreCase(product.getBadge())) {
            return "redirect:/admin/promotions?productId=" + product.getId() + "&error=campaign";
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            return "redirect:/admin/promotions?productId=" + product.getId() + "&error=price";
        }
        Double parsedNew = parseMoney(newPriceRaw);
        double baseline = "sale".equalsIgnoreCase(product.getBadge())
                && product.getPrice() != null && product.getOldPrice() != null && product.getOldPrice() > product.getPrice()
                ? product.getOldPrice() : product.getPrice();
        if (parsedNew == null || parsedNew >= baseline) {
            return "redirect:/admin/promotions?productId=" + product.getId() + "&error=price";
        }
        if (!"sale".equalsIgnoreCase(product.getBadge())) product.setPreviousBadge(product.getBadge());
        product.setOldPrice(baseline);
        product.setPrice(parsedNew);
        product.setBadge("sale");
        productRepository.saveAndFlush(product);
        return "redirect:/admin/promotions?productId=" + product.getId() + "&saved=1";
    }

    // Завершение акции для товара
    @PostMapping("/admin/promotions/finish")
    public String finishPromotion(
            @RequestParam("productId") Long productId,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        productRepository.findById(productId).ifPresent(product -> {
            if ("sale".equalsIgnoreCase(product.getBadge())) {
                if (product.getPrice() != null && product.getOldPrice() != null && product.getOldPrice() > product.getPrice()) {
                    product.setPrice(product.getOldPrice());
                }
                product.setBadge(product.getPreviousBadge());
                product.setPreviousBadge(null);
                product.setOldPrice(null);
            }
            productRepository.saveAndFlush(product);
        });

        return "redirect:/admin/promotions?saved=1";
    }

    // Вкладка «Ликвидация»
    @GetMapping("/admin/liquidation")
    public String adminLiquidation(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "saved", required = false) String saved,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<Product> allProducts = productRepository.findAll();
        List<Product> allLiquidation = allProducts.stream()
                .filter(AdminController::isLiquidationProduct)
                .toList();

        String needle = q == null ? "" : q.trim().toLowerCase();
        String catFilter = category == null ? "" : category.trim();

        List<LiquidationRow> filteredRows = allLiquidation.stream()
                .filter(p -> catFilter.isEmpty()
                        || (p.getCategory() != null && p.getCategory().trim().equalsIgnoreCase(catFilter)))
                .filter(p -> needle.isEmpty()
                        || ((p.getTitle() != null && p.getTitle().toLowerCase().contains(needle))
                        || (p.getCategory() != null && p.getCategory().toLowerCase().contains(needle))))
                .map(this::toLiquidationRow)
                .toList();

        int avgDiscount = 0;
        if (!allLiquidation.isEmpty()) {
            int sumPct = allLiquidation.stream()
                    .mapToInt(AdminController::calcDiscountPercent)
                    .sum();
            avgDiscount = Math.round((float) sumPct / allLiquidation.size());
        }

        List<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Product selectedProduct = null;
        if (productId != null) {
            selectedProduct = allProducts.stream()
                    .filter(p -> productId.equals(p.getId()))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedProduct == null && !allLiquidation.isEmpty()) {
            selectedProduct = allLiquidation.get(0);
        }
        if (selectedProduct == null && !allProducts.isEmpty()) {
            selectedProduct = allProducts.get(0);
        }

        model.addAttribute("products", allProducts);
        model.addAttribute("liquidationRows", filteredRows);
        model.addAttribute("totalLiquidationCount", allLiquidation.size());
        model.addAttribute("avgLiquidationDiscount", avgDiscount);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedProduct", selectedProduct);
        model.addAttribute("selectedRow", selectedProduct != null ? toLiquidationRow(selectedProduct) : null);
        model.addAttribute("liqQuery", q == null ? "" : q);
        model.addAttribute("liqCategory", catFilter);
        model.addAttribute("liqSaved", "1".equals(saved));
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "liquidation");

        return "admin";
    }

    // Сохранение или добавление товара в ликвидацию
    @PostMapping("/admin/liquidation/save")
    public String saveLiquidation(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "newPrice", required = false) String newPriceRaw,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "reason", required = false) String reasonRaw,
            @RequestParam(value = "showOnSite", required = false) String showOnSite,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }
        if (stock != null && stock < 0) return "redirect:/admin/liquidation?error=stock";

        Optional<Product> selected = productRepository.findById(productId);
        if (selected.isEmpty()) return "redirect:/admin/liquidation?error=product";
        Product product = selected.get();
        boolean activeOnSite = showOnSite != null;
        if (activeOnSite) {
            if (product.getPrice() == null || product.getPrice() <= 0) {
                return "redirect:/admin/liquidation?productId=" + productId + "&error=price";
            }
            if ("sale".equalsIgnoreCase(product.getBadge())) {
                return "redirect:/admin/liquidation?productId=" + productId + "&error=campaign";
            }
            Double parsedNew = parseMoney(newPriceRaw);
            double baseline = "liquidation".equalsIgnoreCase(product.getBadge())
                    && product.getPrice() != null && product.getOldPrice() != null && product.getOldPrice() > product.getPrice()
                    ? product.getOldPrice() : product.getPrice();
            if (parsedNew == null || parsedNew >= baseline) {
                return "redirect:/admin/liquidation?productId=" + productId + "&error=price";
            }
            if (!"liquidation".equalsIgnoreCase(product.getBadge())) product.setPreviousBadge(product.getBadge());
            product.setOldPrice(baseline);
            product.setPrice(parsedNew);
            if (stock != null) product.setStock(stock);
            if (reasonRaw != null && !reasonRaw.isBlank()) product.setLiquidationReason(reasonRaw.trim());
            product.setBadge("liquidation");
        } else if ("liquidation".equalsIgnoreCase(product.getBadge())) {
            if (product.getPrice() != null && product.getOldPrice() != null && product.getOldPrice() > product.getPrice()) {
                product.setPrice(product.getOldPrice());
            }
            product.setBadge(product.getPreviousBadge());
            product.setPreviousBadge(null);
            product.setOldPrice(null);
            product.setLiquidationReason(null);
        }
        productRepository.saveAndFlush(product);

        return "redirect:/admin/liquidation?productId=" + productId + "&saved=1";
    }

    // Удаление товара из ликвидации
    @PostMapping("/admin/liquidation/remove")
    public String removeLiquidation(
            @RequestParam("productId") Long productId,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        productRepository.findById(productId).ifPresent(product -> {
            if ("liquidation".equalsIgnoreCase(product.getBadge())) {
                if (product.getPrice() != null && product.getOldPrice() != null && product.getOldPrice() > product.getPrice()) {
                    product.setPrice(product.getOldPrice());
                }
                product.setBadge(product.getPreviousBadge());
                product.setPreviousBadge(null);
                product.setOldPrice(null);
                product.setLiquidationReason(null);
            }
            productRepository.saveAndFlush(product);
        });

        return "redirect:/admin/liquidation?saved=1";
    }

    // Вкладка «Клиенты»
    @GetMapping("/admin/clients")
    public String adminClients(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "segment", required = false) String segment,
            @RequestParam(value = "ordersFilter", required = false) String ordersFilter,
            @RequestParam(value = "amountFilter", required = false) String amountFilter,
            @RequestParam(value = "clientKey", required = false) String clientKey,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        List<Order> allOrders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<ClientSummary> allClients = buildClients(allOrders);

        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String seg = segment == null ? "" : segment.trim().toUpperCase(Locale.ROOT);
        String ordF = ordersFilter == null ? "" : ordersFilter.trim();
        String amtF = amountFilter == null ? "" : amountFilter.trim();

        List<ClientSummary> filtered = allClients.stream()
                .filter(c -> needle.isEmpty()
                        || (c.name().toLowerCase(Locale.ROOT).contains(needle)
                        || c.phone().toLowerCase(Locale.ROOT).contains(needle)
                        || c.email().toLowerCase(Locale.ROOT).contains(needle)))
                .filter(c -> {
                    if ("REPEAT".equals(seg)) {
                        return c.ordersCount() >= 2;
                    }
                    if ("VIP".equals(seg)) {
                        return c.totalSpent().compareTo(BigDecimal.valueOf(100_000)) >= 0;
                    }
                    return true;
                })
                .filter(c -> {
                    if ("1".equals(ordF)) {
                        return c.ordersCount() == 1;
                    }
                    if ("2+".equals(ordF)) {
                        return c.ordersCount() >= 2;
                    }
                    if ("3+".equals(ordF)) {
                        return c.ordersCount() >= 3;
                    }
                    return true;
                })
                .filter(c -> {
                    if ("50K+".equals(amtF)) {
                        return c.totalSpent().compareTo(BigDecimal.valueOf(50_000)) >= 0;
                    }
                    if ("100K+".equals(amtF)) {
                        return c.totalSpent().compareTo(BigDecimal.valueOf(100_000)) >= 0;
                    }
                    return true;
                })
                .toList();

        long repeatClients = allClients.stream().filter(c -> c.ordersCount() >= 2).count();
        int repeatPercent = allClients.isEmpty() ? 0 : Math.round((repeatClients * 100.0f) / allClients.size());

        BigDecimal totalRevenue = allOrders.stream()
                .map(o -> o.getTotal() == null ? BigDecimal.ZERO : o.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgCheck = allOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(allOrders.size()), 0, RoundingMode.HALF_UP);

        ClientSummary selectedClient = null;
        if (clientKey != null && !clientKey.isBlank()) {
            selectedClient = allClients.stream()
                    .filter(c -> clientKey.equals(c.key()))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedClient == null && !filtered.isEmpty()) {
            selectedClient = filtered.get(0);
        }

        model.addAttribute("clients", filtered);
        model.addAttribute("totalClientsCount", allClients.size());
        model.addAttribute("repeatPercent", repeatPercent);
        model.addAttribute("avgCheckFormatted", formatRub(avgCheck));
        model.addAttribute("selectedClient", selectedClient);
        model.addAttribute("clientQuery", q == null ? "" : q);
        model.addAttribute("clientSegment", seg);
        model.addAttribute("clientOrdersFilter", ordF);
        model.addAttribute("clientAmountFilter", amtF);
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "clients");

        return "admin";
    }

    // Вкладка «Настройки»
    @GetMapping("/admin/settings")
    public String adminSettings(Model model, HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        model.addAttribute("settings", getOrCreateSettings());
        model.addAttribute("staffUsers", userRepository.findAll(Sort.by(Sort.Direction.ASC, "username")).stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()) || "STAFF".equalsIgnoreCase(u.getRole()))
                .toList());
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("activeSection", "settings");

        return "admin";
    }

    private ShopSettings getOrCreateSettings() {
        return settingsRepository.findById(1L).orElseGet(() -> settingsRepository.save(new ShopSettings()));
    }

    @PostMapping("/admin/settings")
    public String saveSettings(@ModelAttribute ShopSettings form, HttpSession session) {
        if (isNotStaff(session)) return "redirect:/login";
        String storeEmail = form.getEmail() == null ? "" : form.getEmail().trim();
        if (form.getDeliveryPrice() == null || form.getDeliveryPrice().signum() < 0
                || form.getFreeDeliveryThreshold() == null || form.getFreeDeliveryThreshold().signum() < 0
                || (!form.isCourierEnabled() && !form.isPickupEnabled())
                || !form.isPaymentOnDeliveryEnabled()
                || form.getStoreName() == null || form.getStoreName().isBlank()
                || (!storeEmail.isBlank() && !storeEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            return "redirect:/admin/settings?error=invalid";
        }
        ShopSettings settings = getOrCreateSettings();
        settings.setStoreName(clean(form.getStoreName(), 120));
        settings.setPhone(clean(form.getPhone(), 32));
        settings.setEmail(clean(form.getEmail(), 160));
        settings.setShowroomAddress(clean(form.getShowroomAddress(), 300));
        settings.setCourierEnabled(form.isCourierEnabled());
        settings.setPickupEnabled(form.isPickupEnabled());
        settings.setDeliveryPrice(form.getDeliveryPrice());
        settings.setFreeDeliveryThreshold(form.getFreeDeliveryThreshold());
        settings.setDeliveryTime(clean(form.getDeliveryTime(), 120));
        settings.setOnlinePaymentEnabled(false);
        settings.setPaymentOnDeliveryEnabled(form.isPaymentOnDeliveryEnabled());
        settingsRepository.save(settings);
        return "redirect:/admin/settings?saved=1";
    }

    private static String clean(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    @PostMapping("/admin/staff/add")
    public String addStaff(@RequestParam String username,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           HttpSession session) {
        if (isNotStaff(session)) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/admin/settings?staffError=forbidden";
        String login = username.trim();
        String mail = email.trim().toLowerCase(Locale.ROOT);
        if (login.length() < 3 || fullName.isBlank() || !mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
                ) || userRepository.findByUsername(login).isPresent() || userRepository.findByEmail(mail).isPresent()) {
            return "redirect:/admin/settings?staffError=invalid";
        }
        String passwordError = PasswordPolicy.validate(password);
        if (passwordError != null) return "redirect:/admin/settings?staffError=password";
        User user = new User();
        user.setUsername(login);
        user.setFullName(clean(fullName, 160));
        user.setEmail(mail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STAFF");
        userRepository.save(user);
        return "redirect:/admin/settings?staffSaved=1";
    }

    @PostMapping("/admin/staff/{id}/role")
    public String updateStaffRole(@PathVariable Long id, @RequestParam String role, HttpSession session) {
        if (isNotStaff(session)) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/admin/settings?staffError=forbidden";
        String nextRole = role.trim().toUpperCase(Locale.ROOT);
        if (!"ADMIN".equals(nextRole) && !"STAFF".equals(nextRole)) return "redirect:/admin/settings?staffError=role";
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/settings?staffError=missing";
        if (java.util.Objects.equals(user.getUsername(), session.getAttribute("username")) && "STAFF".equals(nextRole)) {
            return "redirect:/admin/settings?staffError=self";
        }
        boolean removingAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) && "STAFF".equals(nextRole);
        long adminCount = userRepository.findAll().stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        if (removingAdmin && adminCount <= 1) return "redirect:/admin/settings?staffError=last-admin";
        user.setRole(nextRole);
        userRepository.save(user);
        return "redirect:/admin/settings?staffSaved=1";
    }

    @PostMapping("/admin/staff/{id}/delete")
    public String deleteStaff(@PathVariable Long id, HttpSession session) {
        if (isNotStaff(session)) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/admin/settings?staffError=forbidden";
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/settings?staffError=missing";
        if (java.util.Objects.equals(user.getUsername(), session.getAttribute("username"))) {
            return "redirect:/admin/settings?staffError=self";
        }
        long adminCount = userRepository.findAll().stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        if ("ADMIN".equalsIgnoreCase(user.getRole()) && adminCount <= 1) {
            return "redirect:/admin/settings?staffError=last-admin";
        }
        userRepository.delete(user);
        return "redirect:/admin/settings?staffSaved=1";
    }

    // Добавление товара
    @PostMapping("/admin/products/add")
    public String addProduct(
            @ModelAttribute Product product,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        if (!validProduct(product)) return "redirect:/admin/products/new?error=invalid";

        normalizeProduct(product);
        productRepository.save(product);

        return "redirect:/admin";
    }

    private static void normalizeProduct(Product product) {
        product.setTitle(product.getTitle() == null ? "" : product.getTitle().trim());
        product.setCategory(product.getCategory() == null ? "" : product.getCategory().trim());
        product.setArticle(blankToNull(product.getArticle()));
        product.setBarcode(blankToNull(product.getBarcode()));
        product.setColor(blankToNull(product.getColor()));
        product.setImageUrl(product.getImageUrl() == null ? "" : product.getImageUrl().trim());
        product.setDescription(product.getDescription() == null ? "" : product.getDescription().trim());
        product.setIsShowroom(Boolean.TRUE.equals(product.getIsShowroom()));
        if (product.getBadge() != null && product.getBadge().isBlank()) product.setBadge(null);
        if (product.getKit() != null && product.getKit().isBlank()) product.setKit(null);
        if (product.getStyle() != null && product.getStyle().isBlank()) product.setStyle(null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean validProduct(Product product) {
        return product != null
                && product.getTitle() != null && !product.getTitle().isBlank() && product.getTitle().length() <= 200
                && product.getCategory() != null && !product.getCategory().isBlank() && product.getCategory().length() <= 120
                && product.getPrice() != null && Double.isFinite(product.getPrice()) && product.getPrice() > 0
                && (product.getOldPrice() == null || (Double.isFinite(product.getOldPrice()) && product.getOldPrice() > 0))
                && (product.getWidth() == null || product.getWidth() > 0)
                && (product.getStock() == null || product.getStock() >= 0);
    }

    // Форма редактирования товара
    @GetMapping("/admin/products/edit/{id}")
    public String editProductForm(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        Product product = productRepository.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/admin";
        }

        model.addAttribute("product", product);
        model.addAttribute("isNew", false);
        model.addAttribute("isAdmin", isAdmin(session));

        return "admin-edit";
    }

    // Обновление товара
    @PostMapping("/admin/products/update/{id}")
    public String updateProduct(
            @PathVariable("id") Long id,
            @ModelAttribute Product formProduct,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        Product product = productRepository.findById(id).orElse(null);

        if (product != null) {
            if (!validProduct(formProduct)) return "redirect:/admin/products/edit/" + id + "?error=invalid";
            boolean campaignActive = "sale".equalsIgnoreCase(product.getBadge())
                    || "liquidation".equalsIgnoreCase(product.getBadge());
            product.setTitle(formProduct.getTitle());
            product.setCategory(formProduct.getCategory());
            product.setArticle(formProduct.getArticle());
            product.setBarcode(formProduct.getBarcode());
            product.setColor(formProduct.getColor());
            if (!campaignActive) {
                product.setPrice(formProduct.getPrice());
                product.setOldPrice(formProduct.getOldPrice());
                product.setBadge(formProduct.getBadge());
            }
            product.setWidth(formProduct.getWidth());
            product.setStock(formProduct.getStock());
            product.setKit(formProduct.getKit());
            product.setStyle(formProduct.getStyle());
            product.setLiquidationReason(formProduct.getLiquidationReason());
            product.setImageUrl(formProduct.getImageUrl());
            product.setDescription(formProduct.getDescription());
            product.setIsShowroom(Boolean.TRUE.equals(formProduct.getIsShowroom()));

            normalizeProduct(product);

            productRepository.save(product);
        }

        return "redirect:/admin";
    }

    // Удаление товара (POST)
    @PostMapping("/admin/products/delete/{id}")
    public String deleteProductPost(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        if (!isAdmin(session)) {
            return "redirect:/admin";
        }

        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
        }

        return "redirect:/admin";
    }

    // Старый GET-маршрут для совместимости
    @GetMapping("/admin/products/delete/{id}")
    public String deleteProductGet(
            @PathVariable("id") Long id,
            HttpSession session) {

        if (isNotStaff(session)) return "redirect:/login";
        return "redirect:/admin/products";
    }
}
