package com.wh.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.model.Product;
import com.wh.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Imports the user's inventory once and keeps later price/stock edits intact. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CatalogImportRunner implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CatalogImportRunner(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        ClassPathResource resource = new ClassPathResource("catalog-import.json");
        if (!resource.exists()) {
            return;
        }

        List<Product> pending = new ArrayList<>();
        int added = 0;
        int photoUpdates = 0;
        Set<String> seenImportKeys = new HashSet<>();

        try (InputStream input = resource.getInputStream()) {
            JsonNode entries = objectMapper.readTree(input);
            if (entries == null || !entries.isArray()) {
                throw new IllegalStateException("catalog-import.json must contain a JSON array");
            }

            for (JsonNode entry : entries) {
                String importKey = text(entry, "importKey");
                String title = text(entry, "title");
                String category = text(entry, "category");
                if (importKey == null || title == null || category == null) {
                    throw new IllegalStateException("Catalog item is missing its key, title, or category");
                }
                if (!seenImportKeys.add(importKey)) {
                    continue;
                }

                Optional<Product> existing = productRepository.findFirstByImportKey(importKey);
                if (existing.isPresent()) {
                    Product product = existing.get();
                    boolean changed = false;
                    boolean stillUnpriced = product.getPrice() == null || product.getPrice() <= 0;
                    if (stillUnpriced) {
                        product.setTitle(title);
                        product.setCategory(category);
                        product.setArticle(text(entry, "article"));
                        product.setBarcode(text(entry, "barcode"));
                        product.setColor(text(entry, "color"));
                        product.setDescription(text(entry, "description"));
                        Double importedPrice = number(entry, "price");
                        if (importedPrice != null) {
                            product.setPrice(importedPrice);
                        }
                        Double importedOldPrice = number(entry, "oldPrice");
                        if (importedOldPrice != null) {
                            product.setOldPrice(importedOldPrice);
                            product.setBadge(text(entry, "badge"));
                        }
                        Double importedStock = number(entry, "stock");
                        if (importedStock != null) {
                            product.setStock(importedStock.intValue());
                        }
                        changed = true;
                    }
                    Integer importedWidth = integer(entry, "width");
                    if (product.getWidth() == null && importedWidth != null) {
                        product.setWidth(importedWidth);
                        changed = true;
                    }
                    String importedStyle = text(entry, "style");
                    if (blank(product.getStyle()) && !blank(importedStyle)) {
                        product.setStyle(importedStyle);
                        changed = true;
                    }
                    String imageUrl = text(entry, "imageUrl");
                    if (blank(product.getImageUrl()) && !blank(imageUrl)) {
                        product.setImageUrl(imageUrl);
                        photoUpdates++;
                        changed = true;
                    }
                    if (changed) {
                        pending.add(product);
                    }
                    continue;
                }

                Product product = new Product();
                product.setImportKey(importKey);
                product.setArticle(text(entry, "article"));
                product.setBarcode(text(entry, "barcode"));
                product.setColor(text(entry, "color"));
                product.setTitle(title);
                product.setCategory(category);
                product.setWidth(integer(entry, "width"));
                product.setStyle(text(entry, "style"));
                product.setDescription(text(entry, "description"));
                product.setImageUrl(text(entry, "imageUrl"));
                product.setPrice(number(entry, "price"));
                product.setOldPrice(number(entry, "oldPrice"));
                product.setBadge(text(entry, "badge"));
                Double importedStock = number(entry, "stock");
                product.setStock(importedStock == null ? 0 : importedStock.intValue());
                product.setIsShowroom(false);
                pending.add(product);
                added++;
            }
        }

        int featuredProductUpdates = updatePlastWashbasin(pending)
                + updateFrapF02632(pending) + updateFrapF027(pending)
                + updateArinBerdCornice(pending) + updateAdditionalCornices(pending)
                + updateKnoisToilet(pending);

        if (!pending.isEmpty()) {
            productRepository.saveAll(pending);
        }
        System.out.printf("Каталог: добавлено %d товаров, добавлено фото %d, обновлено карточек %d.%n",
                added, photoUpdates, featuredProductUpdates);
    }

    private int updatePlastWashbasin(List<Product> pending) {
        return productRepository.findFirstByTitleAndCategory("Раковина «Пласт»", "Раковины")
                .map(product -> {
                    String imageUrl = "/images/catalog/sanita-fest-80.jpg";
                    String description = "Серия: SANITA FEST 80; Форма раковины: прямоугольная; Монтаж раковины: подвесная; "
                            + "Расположение крыла: без крыла; Количество чаш: 1 шт; "
                            + "Для людей с ограниченными возможностями: нет; Расположение смесителя: посередине; "
                            + "Отверстие для перелива: есть; Количество отверстий под смеситель: 1; "
                            + "Цвет раковины: белый; Материал раковины: санфарфор; Поверхность: глянцевая; "
                            + "Покрытие: без покрытия; Мыльница: нет; Полотенцедержатель: нет; Донный клапан: нет; "
                            + "Высота раковины: 220 мм; Ширина раковины: 800 мм; Глубина раковины: 455 мм; "
                            + "Стиль: современный; Артикул производителя: WB.FN/Fest/80-C/WHT.G/S1; "
                            + "Гарантия производителя: 15 лет.";
                    boolean changed = !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || !"00000055861".equals(product.getArticle())
                            || product.getPrice() == null || product.getPrice() != 6649.0
                            || product.getOldPrice() == null || product.getOldPrice() != 7130.0
                            || !"sale".equals(product.getBadge())
                            || !"modern".equals(product.getStyle());

                    if (!changed) {
                        return 0;
                    }

                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setStyle("modern");
                    product.setArticle("00000055861");
                    product.setPrice(6649.0);
                    product.setOldPrice(7130.0);
                    product.setBadge("sale");
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }

    private int updateFrapF02632(List<Product> pending) {
        return productRepository.findFirstByArticle("F026-32")
                .map(product -> {
                    String title = "Верхний душ FRAP F026-32, 320 × 220 мм";
                    String category = "Душевые лейки";
                    String color = "оружейная сталь";
                    String imageUrl = "/images/catalog/frap-f026-32-1.jpg";
                    String description = "Бренд: FRAP; Модель: F026; Артикул: F026-32; "
                            + "Тип: верхний тропический душ; Форма: прямоугольная; Монтаж: потолочный; "
                            + "Длина: 320 мм; Ширина: 220 мм; Материал: пластик; Цвет: оружейная сталь; "
                            + "Количество режимов: 1; Подключение: 1/2 дюйма; "
                            + "Комплектация: верхний душ, упаковка; Страна производства: Китай.";
                    boolean changed = !title.equals(product.getTitle())
                            || !category.equals(product.getCategory())
                            || !color.equals(product.getColor())
                            || !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || product.getPrice() == null || product.getPrice() != 3460.0
                            || product.getStock() == null || product.getStock() != 0;
                    if (!changed) return 0;
                    product.setTitle(title);
                    product.setCategory(category);
                    product.setArticle("F026-32");
                    product.setColor(color);
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setPrice(3460.0);
                    product.setStock(0);
                    product.setIsShowroom(false);
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }

    private int updateFrapF027(List<Product> pending) {
        return productRepository.findFirstByArticle("F027")
                .map(product -> {
                    String title = "Лейка для душа FRAP F027, 3 режима";
                    String category = "Душевые лейки";
                    String color = "хром";
                    String imageUrl = "/images/catalog/frap-f027-5.jpg";
                    String description = "Бренд: FRAP; Модель: F027; Артикул: F027; "
                            + "Тип: ручная лейка для душа; Количество режимов: 3; Форма: круглая; "
                            + "Диаметр головки: 120 мм; Длина: 250 мм; Материал: ABS-пластик; "
                            + "Покрытие: хромированное; Цвет: хром; Поверхность: глянцевая; "
                            + "Переключение режимов: поворотное; Подключение: 1/2 дюйма; "
                            + "Держатель: не входит в комплект; Шланг: не входит в комплект; Фильтр: нет; "
                            + "Легкая очистка: да; Страна производства: Китай.";
                    boolean changed = !title.equals(product.getTitle())
                            || !category.equals(product.getCategory())
                            || !color.equals(product.getColor())
                            || !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || product.getPrice() == null || product.getPrice() != 1161.0
                            || product.getStock() == null || product.getStock() != 0;
                    if (!changed) return 0;
                    product.setTitle(title);
                    product.setCategory(category);
                    product.setArticle("F027");
                    product.setColor(color);
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setPrice(1161.0);
                    product.setStock(0);
                    product.setIsShowroom(false);
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }



    private int updateAdditionalCornices(List<Product> pending) {
        int updated = 0;
        updated += updateCornice(pending, "Карниз д/в Г-обр. 1000/1000", "84938", 1589.63,
                "/images/catalog/arin-berd-cornice-1000-1000.jpg",
                "Бренд: ARIN BERD; Артикул поставщика: 84938; Форма: Г-образная; Ширина: 1000 мм; "
                        + "Глубина: 1000 мм; Материал: нержавеющая сталь; Диаметр трубы: 1/2 дюйма; "
                        + "Крепление к потолку: нет; Гарантия производителя: 1 год; Страна производства: Россия.");
        updated += updateCornice(pending, "Карниз д/в Г-обр. 1800/800", "Щ0000052844", 4320.0,
                "/images/catalog/triton-cornice-1800-800.jpg",
                "Бренд: Triton; Артикул производителя: Щ0000052844; Форма: Г-образная; "
                        + "Длина: 1800 мм; Ширина: 800 мм; Материал: нержавеющая сталь.");
        updated += updateCornice(pending, "Карниз д/в Дуга 1200/1200", "K25R-120x120", 2450.0,
                "/images/catalog/mrkarniz-arc-1200-1200.jpg",
                "Бренд: Mr.Karniz; Модель: K25R-120x120; Форма: дугообразная; Ширина: 1200 мм; "
                        + "Глубина: 1200 мм; Материал: нержавеющая сталь; Цвет: хром; Диаметр трубы: 25 мм; "
                        + "Толщина стенки: 1 мм; Установка: настенная; Крепление: шурупы; "
                        + "Максимальная нагрузка: более 25 кг; Гарантия: 10 лет; Страна производства: Россия.");
        updated += updateCornice(pending, "Карниз д/в Г-обр. 1200/1000", "K25G-120x100", 3728.0,
                "/images/catalog/mrkarniz-g-1200-1000.jpg",
                "Бренд: Mr.Karniz; Модель: K25G-120x100; Форма: Г-образная; Длина: 1200 мм; "
                        + "Ширина: 1000 мм; Материал: нержавеющая сталь; Цвет: хром; Диаметр трубы: 25 мм; "
                        + "Толщина стенки: 1 мм; Крепление: настенное; Страна производства: Россия.");
        return updated;
    }


    private int updateKnoisToilet(List<Product> pending) {
        return productRepository.findFirstByArticle("KN6184000")
                .map(product -> {
                    String title = "Унитаз моноблок напольный, с сидением";
                    String category = "Унитазы";
                    String color = "белый";
                    String imageUrl = "/images/catalog/knois-KN6184000-1000x1000.jpg";
                    String description = "Бренд: KNOIS; Артикул: KN6184000; Тип: напольный унитаз-моноблок; "
                            + "Комплектация: сиденье; Материал: фаянс; Форма: прямоугольная с острыми углами; "
                            + "Цвет: белый; Тип подключения: нижнее; "
                            + "Размеры: 695 × 380 × 770 мм (глубина × ширина × высота).";
                    boolean changed = !title.equals(product.getTitle())
                            || !category.equals(product.getCategory())
                            || !color.equals(product.getColor())
                            || !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || product.getPrice() == null || product.getPrice() != 16500.0
                            || product.getStock() == null || product.getStock() != 0
                            || product.getWidth() == null || product.getWidth() != 38;
                    if (!changed) return 0;
                    product.setTitle(title);
                    product.setCategory(category);
                    product.setColor(color);
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setPrice(16500.0);
                    product.setStock(0);
                    product.setWidth(38);
                    product.setIsShowroom(false);
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }

    private int updateCornice(List<Product> pending, String title, String article, double price,
                              String imageUrl, String description) {
        return productRepository.findFirstByTitleAndCategory(title, "Карнизы для душа")
                .map(product -> {
                    boolean changed = !article.equals(product.getArticle())
                            || !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || product.getPrice() == null || product.getPrice() != price
                            || product.getStock() == null || product.getStock() != 0;
                    if (!changed) return 0;
                    product.setArticle(article);
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setPrice(price);
                    product.setStock(0);
                    product.setIsShowroom(false);
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }

    private int updateArinBerdCornice(List<Product> pending) {
        return productRepository.findFirstByTitleAndCategory("Карниз д/в Г-обр. 900/900", "Карнизы для душа")
                .map(product -> {
                    String imageUrl = "/images/catalog/arin-berd-cornice-900-900.jpg";
                    String description = "Бренд: ARIN BERD; Модель: карниз для душевого поддона; "
                            + "Артикул поставщика: 84937; Форма: Г-образная; Ширина: 900 мм; Глубина: 900 мм; "
                            + "Материал: нержавеющая сталь; Диаметр трубы: 1/2 дюйма; Крепление к потолку: нет; "
                            + "Гарантия производителя: 1 год; Страна производства: Россия.";
                    boolean changed = !imageUrl.equals(product.getImageUrl())
                            || !description.equals(product.getDescription())
                            || !"84937".equals(product.getArticle())
                            || product.getPrice() == null || product.getPrice() != 1400.63
                            || product.getStock() == null || product.getStock() != 0;
                    if (!changed) return 0;
                    product.setArticle("84937");
                    product.setImageUrl(imageUrl);
                    product.setDescription(description);
                    product.setPrice(1400.63);
                    product.setStock(0);
                    product.setIsShowroom(false);
                    pending.add(product);
                    return 1;
                })
                .orElse(0);
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asDouble();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asInt();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String result = value.asText().trim();
        return result.isEmpty() ? null : result;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
