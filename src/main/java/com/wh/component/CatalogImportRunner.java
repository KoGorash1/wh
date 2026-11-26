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
                + updateKnoisToilet(pending)
                + updateKitchenSinks(pending)
                + updateDrainValves(pending)
                + updateBathroomAccessories(pending)
                + updateShowerSystems(pending)
                + updateMixer(pending, "G1018", "Смеситель для раковины GAPPO G1018", "/images/catalog/mixer-G1018.jpg",
                        "Бренд: GAPPO; Артикул: G1018; Серия: G18; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 131 мм; Высота смесителя: 158 мм; Аэратор: Neoperl; Подводка: гибкая 1/2, 500 мм; Гарантия: 5 лет. Источник: GAPPO." )
                + updateMixer(pending, "G1050", "Смеситель для раковины GAPPO G1050", "/images/catalog/mixer-G1050.png",
                        "Бренд: GAPPO; Артикул: G1050; Серия: Aventador; Назначение: для раковины; Материал: латунь; Цвет: чёрный; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 134 мм; Высота излива: 127 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: до 5 лет. Источники: GAPPO, ВсеИнструменты.")
                + updateMixer(pending, "F1073", "Смеситель для раковины FRAP F1073", "/images/catalog/mixer-F1073.jpg",
                        "Бренд: FRAP; Артикул: F1073; Коллекция: H03; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Аэратор: F72-1 M24; Подводка: гибкая M10×1–G1/2, 45 см. Источник: StroyCity.")
                + updateMixer(pending, "F1087-9", "Смеситель для раковины FRAP F1087-9", "/images/catalog/mixer-F1087-9.jpg",
                        "Бренд: FRAP; Артикул: F1087-9; Назначение: для раковины; Материал: латунь; Цвет: оружейная сталь; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 134 мм; Высота смесителя: 156 мм; Высота излива: 94 мм; Аэратор: есть; Подводка: гибкая. Источник: Dush.kz.")
                + updateMixer(pending, "F10202-6", "Смеситель для раковины FRAP F10202-6", "/images/catalog/mixer-F10202-6.jpg",
                        "Бренд: FRAP; Артикул: F10202-6; Серия: H202; Назначение: для раковины; Цвет: чёрный сатин; Материал корпуса: алюминий; Управление: однорычажное; Картридж: керамический 25 мм; Монтаж: на гайке; Аэратор: F72-1-6 M24; Подводка: гибкая M10×1–G1/2, 450 мм. Источники: каталог FRAP, Vivon.")
                + updateMixer(pending, "F1090-3", "Смеситель для раковины FRAP F1090-3", "/images/catalog/mixer-F1090-3.jpg",
                        "Бренд: FRAP; Артикул: F1090-3; Назначение: для раковины; Материал: латунь; Цвет: золотой сатин; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 30 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 142 мм; Высота излива: 95 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: 3 года на корпус. Источник: FRAP.")
                + updateMixer(pending, "F10806-9", "Смеситель для раковины FRAP F10806-9", "/images/catalog/mixer-F10806-9.jpg",
                        "Бренд: FRAP; Артикул: F10806-9; Серия: H806-9; Назначение: для раковины; Материал: нержавеющая сталь SUS304; Цвет: оружейная сталь; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: стационарный, длина 126 мм; Высота: 151 мм; Аэратор: есть; Подводка: гибкая 1/2, 450 мм. Источник: FRAP.")
                + updateMixer(pending, "F10802", "Смеситель для раковины FRAP F10802", "/images/catalog/mixer-F10802.jpg",
                        "Бренд: FRAP; Артикул: F10802; Серия: H802; Назначение: для раковины; Материал: нержавеющая сталь; Цвет: сатин; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: традиционный, длина 125 мм; Высота: 185 мм; Высота излива: 106 мм; Аэратор: есть; Подводка: гибкая. Источник: FRAP Shop.")
                + updateMixer(pending, "G1007-7", "Смеситель для раковины GAPPO JACOB G1007-7", "/images/catalog/mixer-G1007-7.webp",
                        "Бренд: GAPPO; Артикул: G1007-7; Серия: JACOB; Назначение: для раковины; Материал: латунь; Цвет: белый/хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 124 мм; Высота смесителя: 164 мм; Высота излива: 106 мм; Аэратор: есть; Подводка: гибкая 1/2. Источник: GAPPO Store.")
                + updateMixer(pending, "F1085-9", "Смеситель для раковины FRAP F1085-9", "/images/catalog/mixer-F1085-9.jpg",
                        "Бренд: FRAP; Артикул: F1085-9; Серия: H85; Назначение: для раковины; Материал: латунь; Цвет: оружейная сталь; Поверхность: матовая; Управление: рычажное; Картридж: керамический 26 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 125 мм; Высота излива: 88 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: 3 года на корпус. Источник: FRAP.")
                + updateMixer(pending, "F1319", "Смеситель для раковины FRAP F1319", "/images/catalog/mixer-F1319.jpg",
                        "Бренд: FRAP; Артикул: F1319; Серия: H19; Назначение: для раковины; Материал: силумин; Цвет: хром; Управление: двухвентильное; Кран-букса: керамическая; Монтаж: на шпильке, 1 отверстие; Излив: высокий, поворотный; Длина излива: 130 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: 1 год. Источники: FRAP, ВсеИнструменты.")
                + updateMixer(pending, "G1007-13", "Смеситель для раковины GAPPO G1007-13", "/images/catalog/mixer-G1007-13.webp",
                        "Бренд: GAPPO; Артикул: G1007-13; Серия: G07-3; Назначение: для раковины; Материал: латунь; Цвет: золото; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Длина излива: 112 мм; Высота смесителя: 164 мм; Высота излива: 106 мм; Аэратор: есть; Подводка: гибкая 1/2, 500 мм. Источник: GAPPO.")
                + updateMixer(pending, "G1007-8", "Смеситель для раковины GAPPO G1007-8", "/images/catalog/mixer-G1007-8.webp",
                        "Бренд: GAPPO; Артикул: G1007-8; Серия: G07-8; Назначение: для раковины; Материал: латунь; Цвет: белый/хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 120 мм; Высота смесителя: 165 мм; Высота излива: 101 мм; Аэратор: есть; Подводка: гибкая 1/2. Источник: GAPPO.")
                + updateMixer(pending, "G1050-8", "Смеситель для раковины GAPPO G1050-8", "/images/catalog/mixer-G1050-8.jpg",
                        "Бренд: GAPPO; Артикул: G1050-8; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: G59-87; Монтаж: на гайке; Аэратор: G72-3 M24; Подводка: гибкая M10×1–G1/2, 600 мм. Источник: Sanmarket.")
                + updateMixer(pending, "G1083", "Смеситель для раковины GAPPO G1083", "/images/catalog/mixer-G1083.jpg",
                        "Бренд: GAPPO; Артикул: G1083; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: литой, фиксированный; Длина излива: 138 мм; Высота смесителя: 180 мм; Высота излива: 127 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: 5 лет. Источник: GAPPO.")
                + updateMixer(pending, "FS8701A", "Смеситель для раковины Fmark FS8701A", "/images/catalog/mixer-FS8701A.webp",
                        "Бренд: Fmark; Артикул: FS8701A; Назначение: для раковины; Материал: латунь; Цвет: хром; Поверхность: глянцевая; Управление: рычажное; Картридж: керамический 35 мм; Монтаж: на шпильку, 1 отверстие; Излив: фиксированный, длина 127 мм; Высота: 173 мм; Высота излива: 120 мм; Аэратор: есть; Подводка: гибкая 1/2; Гарантия: 5 лет. Источники: Fmark, ВсеИнструменты.")
                + updateMixer(pending, "S-FXQ007L", "Смеситель для гигиенического душа Savol S-FXQ007L", "/images/catalog/mixer-S-FXQ007L.webp",
                        "Бренд: Savol; Артикул: S-FXQ007L; Назначение: для гигиенического душа; Материал: нержавеющая сталь SUS304; Цвет: сатин; Поверхность: матовая; Управление: рычажное; Картридж: керамический 25 мм; Монтаж: настенный, 2 отверстия; Излив: отсутствует; Ориентация: левая; Шланг: 125 см; Гарантия: 5 лет. Источник: Savol.")
                + updateMixer(pending, "FS8701Q", "Смеситель для раковины Fmark FS8701Q", "/images/catalog/mixer-FS8701Q.webp",
                        "Бренд: Fmark; Артикул: FS8701Q; Назначение: для раковины; Материал: нержавеющая сталь; Цвет: графит; Управление: рычажное; Картридж: керамический; Монтаж: на раковину, 1 отверстие; Излив: традиционный, фиксированный; Подводка: гибкая 1/2. Источник: Fmark.")
                + updateMixer(pending, "FS8201Q", "Смеситель для раковины Fmark FS8201Q", "/images/catalog/mixer-FS8201Q.webp",
                        "Бренд: Fmark; Артикул: FS8201Q; Серия: 82; Назначение: для раковины; Материал: нержавеющая сталь SUS304; Цвет: графит; Поверхность: глянцевая; Управление: рычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 128 мм; Высота излива: 64 мм; Подводка: гибкая 1/2, 450 мм; Гарантия: 5 лет. Источник: Fmark.")
                + updateMixer(pending, "FS8201C", "Смеситель для раковины Fmark FS8201C", "/images/catalog/mixer-FS8201C.webp",
                        "Бренд: Fmark; Артикул: FS8201C; Серия: 82C; Назначение: для раковины; Материал: нержавеющая сталь SUS304; Цвет: матовое золото; Покрытие: PVD; Управление: рычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный, длина 128 мм; Высота излива: 64 мм; Подводка: гибкая 1/2, 450 мм; Гарантия: 5 лет. Источник: Fmark.")
                + updateMixer(pending, "S-FXQ009Q", "Смеситель для гигиенического душа Savol S-FXQ009Q", "/images/catalog/mixer-S-FXQ009Q.webp",
                        "Бренд: Savol; Артикул: S-FXQ009Q; Назначение: для биде и гигиенического душа; Материал: нержавеющая сталь; Цвет: графит; Поверхность: матовая; Управление: вентильное; Механизм: керамическая кран-букса; Монтаж: настенный; Подключение: 1/2; Шланг: 120 см; Гарантия: 5 лет. Источник: Savol.")
                + updateMixer(pending, "043-352", "Смеситель для умывальника aQuaDv 043-352", "/images/catalog/mixer-043-352.jpg",
                        "Бренд: aQuaDv; Артикул: 043-352; Назначение: для умывальника; Монтаж: на гайке. Источник: СКФООПТ.")
                + updateMixer(pending, "041", "Смеситель для умывальника aQuaDv 041", "/images/catalog/mixer-041.jpg",
                        "Бренд: aQuaDv; Артикул: 041; Назначение: для умывальника; Материал: латунь; Картридж: керамический 35 мм; Монтаж: на шпильке. Источник: СКФООПТ.")
                + updateMixer(pending, "CN10523", "Смеситель для раковины Cron CN10523", "/images/catalog/mixer-CN10523.jpg",
                        "Бренд: Cron; Артикул: CN10523; Серия: CN523; Назначение: для раковины; Цвет: хром; Управление: рычажное; Материал: металлический сплав; Картридж: керамический 35 мм; Монтаж: на раковину, 1 отверстие; Излив: фиксированный, длина 123 мм; Высота излива: 106 мм; Подводка: гибкая 1/2; Гарантия: 1 год. Источники: Entero, Bravax.")
                + updateMixer(pending, "F1046", "Смеситель для раковины FRAP F1046", "/images/catalog/mixer-F1046.jpg",
                        "Бренд: FRAP; Артикул: F1046; Серия: H46; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: короткий, фиксированный; Аэратор: F72-5 M24; Подводка: гибкая M10×1–G1/2, 45 см; Гарантия: 1 год. Источники: FRAP, StroyCity.")
                + updateMixer(pending, "F1348", "Смеситель для кухни FRAP F1348", null,
                        "Бренд: FRAP; Артикул: F1348; Серия: H48; Назначение: для кухни; Излив: высокий, J-образный, поворотный; Цвет: сатин. Источник: Sanriks.")
                + updateMixer(pending, "F1049", "Смеситель для раковины FRAP F1049", "/images/catalog/F1049.webp",
                        "Бренд: FRAP; Артикул: F1049; Серия: H49; Назначение: для раковины; Материал: латунь; Цвет: белый/хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Высота смесителя: 178 мм; Длина излива: 138 мм; Аэратор: есть; Подводка: гибкая 1/2. Источник: FRAP.")
                + updateMixer(pending, "F1061-3", "Смеситель для раковины высокий FRAP F1061-3", "/images/catalog/F1061-3.webp",
                        "Бренд: FRAP; Артикул: F1061-3; Назначение: для раковины; Материал: латунь; Цвет: сатин золотой; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке; Аэратор: F72-5 M24; Подводка: гибкая M10×1–G1/2, 450 мм. Источник: каталог FRAP.")
                + updateMixer(pending, "F10703-B", "Смеситель для раковины FRAP F10703-B", "/images/catalog/F10703-B.webp",
                        "Бренд: FRAP; Артикул: F10703-B; Серия: H703; Назначение: для раковины; Материал: силумин; Цвет: хром; Управление: однорычажное; Картридж: керамический 40 мм; Монтаж: на шпильках, 1 отверстие; Излив: фиксированный; Длина излива: 112 мм; Высота излива: 71 мм; Аэратор: есть; Подводка: гибкая 1/2. Источник: FRAP.")
                + updateMixer(pending, "F1086-3", "Смеситель для раковины FRAP F1086-3", "/images/catalog/F1086-3.webp",
                        "Бренд: FRAP; Артикул: F1086-3; Серия: H86; Назначение: для раковины; Материал: латунь; Цвет: сатин золотой; Управление: однорычажное; Картридж: керамический; Монтаж: на гайке; Аэратор: есть; Подводка: гибкая. Источник: каталог FRAP.")
                + updateMixer(pending, "F1089", "Смеситель для раковины FRAP F1089", "/images/catalog/F1089.webp",
                        "Бренд: FRAP; Артикул: F1089; Серия: H89; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 30 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Высота смесителя: 160 мм; Длина излива: 146 мм; Высота излива: 104 мм; Аэратор: есть; Подводка: гибкая 1/2. Источник: FRAP.")
                + updateMixer(pending, "F1089-9", "Смеситель для раковины FRAP F1089-9", "/images/catalog/F1089-9.webp",
                        "Бренд: FRAP; Артикул: F1089-9; Серия: H89; Назначение: для раковины; Материал: латунь; Цвет: оружейная сталь; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 30 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Высота смесителя: 160 мм; Длина излива: 146 мм; Высота излива: 104 мм; Аэратор: есть; Подводка: гибкая. Источник: FRAP.")
                + updateMixer(pending, "F20801", "Смеситель для душа FRAP F20801 с гигиенической лейкой", "/images/catalog/F20801.webp",
                        "Бренд: FRAP; Артикул: F20801; Назначение: для душа с гигиенической лейкой; Материал: нержавеющая сталь SUS304; Цвет: сатин; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: настенный; Шланг: 1,2 м; Лейка: нержавеющая сталь, 1 режим; Эксцентрики: в комплекте. Источник: каталог FRAP.")
                + updateMixer(pending, "F7504-9", "Смеситель для душа FRAP F7504-9 с гигиенической лейкой", "/images/catalog/F7504-9.jpg",
                        "Бренд: FRAP; Артикул: F7504-9; Назначение: для душа с гигиенической лейкой; Материал: латунь; Цвет: графит; Управление: однорычажное; Картридж: керамический 25 мм; Монтаж: настенный; Шланг: 1,2 м; Лейка: F21-1-9; Эксцентрики: в комплекте. Источник: каталог FRAP.")
                + updateMixer(pending, "F1061", "Смеситель для раковины FRAP F1061", "/images/catalog/F1061.webp",
                        "Бренд: FRAP; Артикул: F1061; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Аэратор: есть; Подводка: гибкая 1/2. Источник: каталог FRAP.")
                + updateMixer(pending, "F1061-23", "Смеситель для раковины высокий FRAP F1061-23", "/images/catalog/F1061-23.webp",
                        "Бренд: FRAP; Артикул: F1061-23; Назначение: для раковины; Материал: латунь; Цвет: сатин золотой; Управление: однорычажное; Картридж: керамический 35 мм; Монтаж: на гайке; Излив: высокий; Подводка: гибкая 1/2. Источник: каталог FRAP.")
                + updateMixer(pending, "F1085", "Смеситель для раковины FRAP F1085", "/images/catalog/F1085.webp",
                        "Бренд: FRAP; Артикул: F1085; Серия: H85; Назначение: для раковины; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж: керамический; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Аэратор: есть; Подводка: гибкая. Источник: каталог FRAP.")
                + updateMixer(pending, "F1089-3", "Смеситель для раковины FRAP F1089-3", "/images/catalog/F1089-3.webp",
                        "Бренд: FRAP; Артикул: F1089-3; Серия: H89; Назначение: для раковины; Материал: латунь; Цвет: сатин золотой; Поверхность: матовая; Управление: однорычажное; Картридж: керамический 30 мм; Монтаж: на гайке, 1 отверстие; Излив: фиксированный; Высота смесителя: 160 мм; Длина излива: 146 мм; Высота излива: 104 мм; Аэратор: есть; Подводка: гибкая. Источник: FRAP.")
                + removeUnarticledShowerSystems()
                + removeUnarticledMixers()
                + updateBathMixers(pending)
                + removeUnarticledBathtubs()
                + updateBathtubs(pending)
                + removeUnarticledBathroomFurniture()
                + updateBathroomFurniture(pending)
                + updateMirrorCategories(pending)
                + updateToilet(pending, "KN61710BW", "Унитаз-моноблок KNOIS с биде",
                        18990.0, 38, "/images/catalog/knois-KN61710BW-1000x1000.jpg",
                        "Бренд: KNOIS; Артикул: KN61710BW; Тип: напольный унитаз-моноблок с функцией биде; "
                                + "Материал: фаянс; Форма: округлая; Цвет: белый; Подвод воды: нижний; "
                                + "Сиденье в комплекте. Источник: ПроТепло33.",
                        "/images/catalog/knois-KN61710BW-specs.png")
                + updateToilet(pending, "KN6279000", "Унитаз-моноблок KNOIS",
                        16990.0, 38, "/images/catalog/knois-KN6279000-1000x1000.jpg",
                        "Бренд: KNOIS; Артикул: KN6279000; Тип: напольный унитаз-моноблок; "
                                + "Материал: фаянс; Форма: округлая; Цвет: белый; Подвод воды: нижний; "
                                + "Сиденье в комплекте. Источник: ПроТепло33.",
                        "/images/catalog/knois-KN6279000-specs.jpg")
                + updateToilet(pending, "KN6291000", "Унитаз-моноблок Stella Polar Толедо",
                        15990.0, 40, "/images/catalog/knois-KN6291000-1000x1000.jpg",
                        "Бренд: Stella Polar; Коллекция: Толедо; Артикул: KN6291000; Тип: напольный унитаз-моноблок; "
                                + "Материал: фаянс; Форма: округлая; Цвет: белый; Подвод воды: нижний; "
                                + "Размеры: 685 × 400 × 700 мм (глубина × ширина × высота); сиденье с микролифтом. "
                                + "Источник: ПроТепло33 и Santehnica.",
                        "/images/catalog/knois-KN6291000-specs.jpg")
                + updateToilet(pending, "KN6831000", "Унитаз-моноблок Stella Polar Куэнка",
                        17990.0, 38, "/images/catalog/stella-polar-kuenka-KN6831000-1000x1000.jpg",
                        "Бренд: Stella Polar; Коллекция: Куэнка; Артикул: KN6831000; Тип: напольный унитаз-моноблок; "
                                + "Материал: фаянс; Форма чаши: овальная; Цвет: белый; Подвод воды: нижний; "
                                + "Смыв: душевой, двухрежимный; антивсплеск; сиденье из дюропласта с микролифтом; "
                                + "Размеры: 685 × 380 × 775 мм (длина × ширина × высота). "
                                + "Источник цены и фото: ПроТепло33; характеристики: Stella Polar / Santehnica.",
                        "/images/catalog/stella-polar-kuenka-KN6831000-2.jpg")
                + updateToiletFloorFixing(pending)
                + updateShowerEnclosure(pending, "1228C/100MATT",
                        "Душевое ограждение REDO 1228C Chrome Matt 100×100",
                        "матовое стекло, хромированный профиль", 25200.0,
                        "/images/catalog/redo-1228c-100-matt-main.png",
                        "Бренд: REDO; Серия: 1228C Chrome Matt; Артикул: 1228C/100MATT; Форма: квадратная; "
                                + "Монтаж: универсальный, на пол или поддон; Размеры: 1000 × 1000 × 1950 мм; "
                                + "Стекло: закалённое, матовое, толщиной 6 мм; Двери: 2 раздвижные; "
                                + "Профиль: анодированный алюминий, серебристый; Поддон в комплекте: нет; "
                                + "Безсиликоновая сборка; Гарантия: 1 год. Цена найдена в REDO82.",
                        "/images/catalog/redo-1228c-100-matt-2.png")
                + updateShowerEnclosure(pending, "RD-JK.100G",
                        "Душевое ограждение REDO Joy Grey 100×100",
                        "серое тонированное стекло", 31634.0,
                        "/images/catalog/redo-joy-grey-100-100-main.jpeg",
                        "Бренд: REDO; Серия: Joy Grey; Артикул: RD-JK.100G; Форма: квадратная; "
                                + "Размеры: 1000 × 1000 × 1950 мм; Стекло: закалённое, серое тонированное, "
                                + "толщиной 6 мм; Двери: 2 раздвижные; Профиль: алюминий; "
                                + "Установка: на пол или поддон; Поддон в комплекте: нет; "
                                + "Страна производства: Китай. Цена найдена в ФО-ОПТТОРГ.",
                        null);

        if (!pending.isEmpty()) {
            productRepository.saveAll(pending);
        }
        featuredProductUpdates += updateMixerCategories(pending);
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
                    if (!changed) {
                        return 0;
                    }
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

    private int updateToilet(List<Product> pending, String article, String title, double price, int width,
                             String imageUrl, String description, String galleryImage) {
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Унитазы".equals(product.getCategory())
                    || !"белый".equals(product.getColor())
                    || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription())
                    || product.getPrice() == null || product.getPrice() != price
                    || product.getStock() == null || product.getStock() != 0
                    || product.getWidth() == null || product.getWidth() != width;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Унитазы");
            product.setColor("белый");
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(0);
            product.setWidth(width);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private int updateToiletFloorFixing(List<Product> pending) {
        return productRepository.findFirstByArticle("6-0001").map(product -> {
            String title = "Крепление унитаза к полу Симтек";
            String imageUrl = "/images/catalog/simtek-toilet-floor-fixing-6-0001.png";
            String description = "Бренд: Симтек; Артикул: 6-0001; Тип: комплект для напольного крепления унитаза; "
                    + "Материал: сталь и пластик; Длина крепежа: 70 мм; Комплектация: 2 шурупа, 2 дюбеля, "
                    + "шайбы и белые декоративные колпачки; Цена: 62,22 ₽ за комплект. Источник: Lunda.";
            boolean changed = !title.equals(product.getTitle())
                    || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription())
                    || product.getPrice() == null || product.getPrice() != 62.22
                    || product.getStock() == null || product.getStock() != 0;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Унитазы");
            product.setColor("белый");
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(62.22);
            product.setStock(0);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private int updateShowerEnclosure(List<Product> pending, String article, String title, String color,
                                      double price, String imageUrl, String description, String galleryImage) {
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Душевые ограждения".equals(product.getCategory())
                    || !color.equals(product.getColor())
                    || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription())
                    || product.getPrice() == null || product.getPrice() != price
                    || product.getStock() == null || product.getStock() != 0
                    || product.getWidth() == null || product.getWidth() != 100;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Душевые ограждения");
            product.setColor(color);
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(0);
            product.setWidth(100);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private int updateKitchenSinks(List<Product> pending) {
        int updated = 0;
        updated += updateKitchenSink(pending, "T76050H-1", "Мойка OUTE 60×50 см с декором", "чёрный", 8490.0,
                "/images/catalog/oute-T76050H-1.jpg", 60,
                "Бренд: OUTE; Артикул: T76050H-1; Размер: 600×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: чёрный/графит; Покрытие: PVD; Чаша: одна, глубина 200 мм; Установка: врезная; "
                        + "В комплекте сифон. Цена: Colorlon.");
        updated += updateKitchenSink(pending, "T75050H-1", "Мойка OUTE 50×50 см с декором", "чёрный", 6489.0,
                "/images/catalog/oute-T75050H-1.jpg", 50,
                "Бренд: OUTE; Артикул: T75050H-1; Размер: 500×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: чёрный/графит; Покрытие: PVD; Чаша: одна, глубина 200 мм; Установка: врезная; "
                        + "В комплекте сифон. Цена: Яндекс Маркет.");
        updated += updateKitchenSink(pending, "T75050L", "Мойка OUTE 50×50 см сатин", "сатин", 5499.0,
                "/images/catalog/oute-T75050L.jpg", 50,
                "Бренд: OUTE; Артикул: T75050L; Размер: 500×500×200 мм; Материал: нержавеющая сталь SUS304; "
                        + "Цвет/покрытие: сатин PVD; Чаша: одна; Толщина основания: 3 мм, чаши: 1 мм; "
                        + "Шумоизоляция; отверстие слива 90 мм; отверстия под смеситель и дозатор; сифон в комплекте; "
                        + "Гарантия: 2 года на мойку и покрытие. Цена: ВсеИнструменты.");
        updated += updateKitchenSink(pending, "T76050L-1+TD1", "Мойка OUTE 60×50 см сатин с декором", "сатин", null,
                "/images/catalog/oute-T76050L-1.jpg", 60,
                "Бренд: OUTE; Артикул: T76050L-1+TD1; Размер: 600×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: сатин; Декоративная обработка чаши; Покрытие PVD; Одна чаша; Врезной монтаж; "
                        + "Цена для этой комплектации не подтверждена — уточняется.");
        updated += updateKitchenSink(pending, "T76050H+TD1", "Мойка OUTE 60×50 см чёрная", "чёрный", null,
                "/images/catalog/oute-T76050H.jpg", 60,
                "Бренд: OUTE; Артикул: T76050H+TD1; Размер: 600×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: чёрный; Покрытие PVD; Одна чаша; Врезной монтаж; Сифон в комплекте. "
                        + "Цена для этой комплектации не подтверждена — уточняется.");
        updated += updateKitchenSink(pending, "T75050L-1+TD1", "Мойка OUTE 50×50 см сатин с декором", "сатин", null,
                "/images/catalog/oute-T75050L-1.jpg", 50,
                "Бренд: OUTE; Артикул: T75050L-1+TD1; Размер: 500×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: сатин; Декоративная обработка чаши; Покрытие PVD; Одна чаша; Врезной монтаж; "
                        + "Цена для этой комплектации не подтверждена — уточняется.");
        updated += updateKitchenSink(pending, "T75050J", "Мойка OUTE 50×50 см матовое золото", "матовое золото", 7856.0,
                "/images/catalog/oute-T75050J.jpg", 50,
                "Бренд: OUTE; Артикул: T75050J; Размер: 500×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: матовое золото; PVD-покрытие; Одна чаша, глубина 200 мм; Сифон в комплекте; "
                        + "В комплекте также тропическая лейка 30 см. Цена: ВсеИнструменты.");
        updated += updateKitchenSink(pending, "T75050H+TD1", "Мойка OUTE 50×50 см чёрная", "чёрный", null,
                "/images/catalog/oute-T75050H.jpg", 50,
                "Бренд: OUTE; Артикул: T75050H+TD1; Размер: 500×500×200 мм; Материал: нержавеющая сталь; "
                        + "Цвет: чёрный; Декоративная обработка; PVD-покрытие; Одна чаша; Врезной монтаж; "
                        + "Цена для этой комплектации не подтверждена — уточняется.");

        updated += updateKitchenSink(pending, "R700.5050.03", "Мойка RAGLO R700 50×50×20,8 см", "сатин золотой", 22420.0,
                "/images/catalog/raglo-R700-5050-03.jpg", 50, ragloDescription("R700.5050.03", "сатин золотой", 50));
        updated += updateKitchenSink(pending, "R700.5050.05", "Мойка RAGLO R700 50×50×20,8 см", "сатин", 20840.0,
                "/images/catalog/raglo-R700-5050-05.jpg", 50, ragloDescription("R700.5050.05", "сатин", 50));
        updated += updateKitchenSink(pending, "R700.5050.09", "Мойка RAGLO R700 50×50×20,8 см", "графит", 22420.0,
                "/images/catalog/raglo-R700-5050-09.jpg", 50, ragloDescription("R700.5050.09", "графит", 50));
        updated += updateKitchenSink(pending, "R701.6050.03", "Мойка RAGLO R701 60×50×20,8 см", "сатин золотой", 24168.0,
                "/images/catalog/raglo-R701-6050-03.jpg", 60, ragloDescription("R701.6050.03", "сатин золотой", 60));
        updated += updateKitchenSink(pending, "R701.6050.05", "Мойка RAGLO R701 60×50×20,8 см", "сатин", 22518.0,
                "/images/catalog/raglo-R701-6050-05.jpg", 60, ragloDescription("R701.6050.05", "сатин", 60));
        return updated;
    }

    private int updateMirrorCategories(List<Product> pending) {
        int updated = 0;
        for (String article : List.of("Б725", "В706", "ЗЛП5196", "ЗЛП5193", "ЗЛП5195", "ЗЛП5194")) {
            updated += productRepository.findFirstByArticle(article).map(product -> {
                if ("Зеркала".equals(product.getCategory())) return 0;
                product.setCategory("Зеркала");
                pending.add(product);
                return 1;
            }).orElse(0);
        }
        return updated;
    }

    private int updateDrainValves(List<Product> pending) {
        int updated = 0;
        updated += updateDrainValve(pending, "RD-UN3850C", "Донный клапан REDO универсальный",
                "хром", 1308.0, "/images/catalog/rd-un3850c.jpg", "нержавеющая сталь");
        updated += updateDrainValve(pending, "RD-UN385MB", "Донный клапан REDO универсальный",
                "матовый черный", 1439.0, "/images/catalog/rd-un385mb.jpg", "нержавеющая сталь");
        updated += updateDrainValve(pending, "RD-UN3850G", "Донный клапан REDO универсальный",
                "золотой", 1439.0, "/images/catalog/rd-un3850g.jpg", "нержавеющая сталь");
        updated += updateDrainValve(pending, "RD-UNK385MB", "Донный клапан REDO универсальный керамический",
                "матовый черный", 1253.0, "/images/catalog/rd-unk385mb.jpg", "керамика");
        updated += updateDrainValve(pending, "RD-UNK385MW", "Донный клапан REDO универсальный керамический",
                "матовый белый", 1253.0, "/images/catalog/rd-unk385mw.jpg", "керамика");
        updated += updateDrainValve(pending, "RD-UNK385GW", "Донный клапан REDO универсальный керамический",
                "глянцевый белый", 1253.0, "/images/catalog/rd-unk385gw.jpg", "керамика");
        return updated;
    }

    private int updateDrainValve(List<Product> pending, String article, String title, String color,
                                 double price, String imageUrl, String material) {
        String description = "Бренд: REDO; Артикул: " + article + "; Тип: универсальный донный клапан click-clack; "
                + "Материал: " + material + "; Цвет: " + color + "; Диаметр слива: 32 мм; "
                + "Универсальное подключение; подходит для раковин без перелива; Страна производства: Китай; "
                + "Вес в упаковке: 0,3 кг. Цена и фото: ФО-ОПТТОРГ.";
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Сливы и донные клапаны".equals(product.getCategory())
                    || !color.equals(product.getColor())
                    || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription())
                    || product.getPrice() == null || product.getPrice() != price
                    || product.getStock() == null || product.getStock() != 0;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Сливы и донные клапаны");
            product.setColor(color);
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(0);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private int updateBathroomAccessories(List<Product> pending) {
        int updated = 0;
        updated += updateBathroomAccessory(pending, "G904-3", "Ёршик для унитаза GAPPO G904-3",
                "сатиновое золото", 8809, "/images/catalog/g904-3-1.png",
                "Тип: ёршик для унитаза; Установка: напольная или настенная; Конструкция: держатель с крышкой; Материал: латунь, пластик; Цвет: сатиновое золото; Высота: около 49 см; Диаметр: около 8 см; Гарантия: 1 год.");
        updated += updateBathroomAccessory(pending, "G904-9", "Ёршик для унитаза GAPPO G904-9",
                "оружейная сталь", 8809, "/images/catalog/g904-9-1.png",
                "Тип: ёршик для унитаза; Установка: напольная или настенная; Конструкция: держатель с крышкой; Материал: металл, пластик; Цвет: оружейная сталь; Комплектация: ёршик, держатель, крепления для настенного монтажа.");
        updated += updateBathroomAccessory(pending, "G906-3", "Многофункциональная стойка GAPPO G906-3",
                "сатиновое золото", 14958, "/images/catalog/g906-3-1.png",
                "Тип: многофункциональная стойка для ванной; Установка: напольная; Комплектация: держатель туалетной бумаги, полка, ёршик; Материал: латунь, пластик; Цвет: сатиновое золото.");
        updated += updateBathroomAccessory(pending, "G906-9", "Многофункциональная стойка GAPPO G906-9",
                "оружейная сталь", 14224, "/images/catalog/g906-9-1.png",
                "Тип: многофункциональная стойка для ванной; Установка: напольная; Комплектация: держатель туалетной бумаги, полка, ёршик; Материал: металл, пластик; Цвет: оружейная сталь.");
        updated += updateBathroomAccessory(pending, "R340.02", "Полка RAGLO R340.02 с крючками",
                "хром", 3884, "/images/catalog/r340-02-1.jpg",
                "Тип: прямая полка для ванны и душа; Установка: настенная, скрытый монтаж; Размеры: 29×11,5×5,5 см; Материал: нержавеющая сталь SUS304; Цвет: хром; Крючки: 2 боковых; Максимальная нагрузка: 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.02.09", "Полка RAGLO R340.02.09 с крючками",
                "графит", 4472, "/images/catalog/r340-02-09-1.jpg",
                "Тип: прямая полка для ванны и душа; Установка: настенная, скрытый монтаж; Размеры: 29×11,5×5,5 см; Материал: нержавеющая сталь SUS304; Цвет: графит; Крючки: 2 боковых; Максимальная нагрузка: 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.03.03", "Полка RAGLO R340.03.03",
                "сатиновое золото", 5207, "/images/catalog/r340-03-03-1.jpg",
                "Тип: прямая полка для ванной; Установка: настенная, скрытый монтаж; Размеры: 40×10×8 см; Материал: нержавеющая сталь SUS304; Цвет: сатиновое золото; Максимальная нагрузка: 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.03.09", "Полка RAGLO R340.03.09",
                "графит", 5295, "/images/catalog/r340-03-09-1.jpg",
                "Тип: прямая полка для ванной; Установка: настенная, скрытый монтаж; Размеры: 40×10×8 см; Материал: нержавеющая сталь SUS304; Цвет: графит; Максимальная нагрузка: 5 кг.");
        return updated;
    }

    private int updateShowerSystems(List<Product> pending) {
        int updated = 0;
        updated += updateShowerSystem(pending, "G2407", "Душевая система GAPPO G2407 с каскадным изливом",
                "хром", 23575, "/images/catalog/g2407-1.jpg",
                "Бренд: GAPPO; Артикул: G2407; Тип: душевая система с верхним и ручным душем; Установка: настенная; Материал корпуса: латунь; Управление: однорычажное; Излив: каскадный, поворотный, служит переключателем душа; Высота стойки: регулируется от 100 до 150 см; Картридж: керамический SEDAL; Цвет: хром; Гарантия: 5 лет на корпус и покрытие, 1 год на комплектующие.");
        updated += updateShowerSystem(pending, "G2407-8", "Душевая система GAPPO G2407-8 белый/хром",
                "белый/хром", 40939, "/images/catalog/g2407-8-1.png",
                "Бренд: GAPPO; Артикул: G2407-8; Тип: душевая система с верхним и ручным душем; Установка: настенная; Материал корпуса: латунь; Цвет: белый/хром; Размер верхнего душа: 200×300 мм; Высота стойки: 157 см; Длина излива: 16,5 см; Режимы ручной лейки: 2; Длина шланга: 150 см; Картридж: керамический, Sedal 35 мм; Гарантия: 5 лет на корпус и покрытие, 1 год на комплектующие.");
        updated += updateShowerSystem(pending, "F2416", "Душевая система FRAP F2416 с изливом",
                "хром", 18470, "/images/catalog/f2416-1.jpg",
                "Бренд: FRAP; Артикул: F2416; Тип: душевая стойка со смесителем, верхним и ручным душем; Установка: настенная; Материал: латунь; Цвет: хром; Управление: рычажное; Длина излива: 180 мм; Длина шланга: 1500 мм; Режимы ручной лейки: 3; Картридж: керамический.");
        updated += updateShowerSystem(pending, "F2461", "Душевая система FRAP F2461 с тропическим душем",
                "хром", 21220, "/images/catalog/F2461.jpg",
                "Бренд: FRAP; Артикул: F2461; Серия: H61; Тип: душевая стойка со смесителем, тропическим и ручным душем; Установка: настенная, на два отверстия; Материал корпуса: латунь; Цвет: хром; Размер верхнего душа: 290×190 мм; Режимы ручной лейки: 3; Длина шланга: 150 см; Излив: поворотный; Картридж: керамический, 35 мм.");
        updated += updateShowerSystem(pending, "F24803-1", "Душевая система FRAP F24803-1 сатин",
                "сатин", 29900, "/images/catalog/f24803-1-1.webp",
                "Бренд: FRAP; Артикул: F24803-1; Серия: H803; Тип: душевая стойка со смесителем, верхним и ручным душем; Установка: настенная; Цвет: сатин; Длина излива: 187 мм; Длина шланга: 1500 мм.");
        updated += updateShowerSystem(pending, "F2490", "Душевая система FRAP F2490 хром",
                "хром", 26510, "/images/catalog/f2490-1.jpg",
                "Бренд: FRAP; Артикул: F2490; Серия: H90; Тип: душевая стойка со смесителем, верхним и ручным душем; Установка: настенная; Материал корпуса: латунь; Цвет: хром; Высота: около 143 см; Излив: поворотный; Запорный механизм: керамический картридж.");
        updated += updateShowerSystem(pending, "F2491-23", "Душевая система FRAP F2491-23 золотой сатин",
                "золотой сатин", 17454, "/images/catalog/f2491-23-1.jpg",
                "Бренд: FRAP; Артикул: F2491-23; Серия: H91-3; Тип: душевая стойка со смесителем, верхним и ручным душем; Установка: настенная; Материал корпуса: латунь; Цвет: золотой сатин; Поверхность: матовая; Излив: поворотный, длина 22 см; Режимы ручной лейки: 3; Длина шланга: 150 см; Запорный механизм: керамический картридж.");
        updated += updateShowerSystem(pending, "F2491-9", "Душевая система FRAP F2491-9 оружейная сталь",
                "оружейная сталь", 23657, "/images/catalog/f2491-9-1.jpg",
                "Бренд: FRAP; Артикул: F2491-9; Серия: H91-9; Тип: душевая стойка со смесителем, верхним и ручным душем; Установка: настенная; Материал корпуса: латунь; Цвет: оружейная сталь; Верхний душ: 320×220 мм; Высота собранной стойки: около 1703 мм; Длина излива: около 239 мм; Режимы ручной лейки: 3; Длина шланга: 150 см; Запорный механизм: керамический картридж.");
        return updated;
    }

    private int updateShowerSystem(List<Product> pending, String article, String title, String color,
                                   double price, String imageUrl, String description) {
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Душевые системы".equals(product.getCategory())
                    || !color.equals(product.getColor()) || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription()) || product.getPrice() == null
                    || product.getPrice() != price || product.getStock() == null || product.getStock() != 0;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Душевые системы");
            product.setColor(color);
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(0);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private int removeUnarticledShowerSystems() {
        List<Product> withoutArticle = productRepository.findAll().stream()
                .filter(product -> "Душевые системы".equals(product.getCategory()))
                .filter(product -> blank(product.getArticle()))
                .toList();
        productRepository.deleteAll(withoutArticle);
        return withoutArticle.size();
    }

    private int removeUnarticledMixers() {
        List<Product> withoutArticle = productRepository.findAll().stream()
                .filter(product -> product.getCategory() != null
                        && product.getCategory().startsWith("Смесители"))
                .filter(product -> blank(product.getArticle()))
                .toList();
        productRepository.deleteAll(withoutArticle);
        return withoutArticle.size();
    }

    private int updateBathMixers(List<Product> pending) {
        int updated = 0;
        updated += updateMixer(pending, "FS8124", "Смеситель для ванны Fmark FS8124 сатин",
                "/images/catalog/mixer-bath-FS8124.jpg", "Бренд: Fmark; Артикул: FS8124; Назначение: ванна и душ; Материал: нержавеющая сталь; Цвет: сатин; Монтаж: настенный; Излив: поворотный, 255 мм; Механизм: керамический картридж; Переключение ванна/душ: поворотное; Гарантия: 5 лет. Источник: Fmark.");
        updated += updateMixer(pending, "FS8224", "Смеситель для ванны Fmark FS8224 сатин",
                "/images/catalog/mixer-bath-FS8224.jpg", "Бренд: Fmark; Артикул: FS8224; Назначение: ванна и душ; Материал: нержавеющая сталь; Цвет: сатин; Монтаж: настенный; Длинный поворотный излив; Душевой шланг и лейка в комплекте. Источник: Fmark.");
        updated += updateMixer(pending, "F2124", "Смеситель для ванны FRAP F2124", "/images/catalog/mixer-bath-F2124.jpg",
                "Бренд: FRAP; Артикул: F2124; Серия: H24; Назначение: ванна и душ; Материал: латунь; Цвет: хром; Управление: двухвентильное; Монтаж: настенный, 2 отверстия; Излив: поворотный, 30 см; Механизм: керамические кран-буксы; Переключатель на душ; Шланг 150 см, лейка, держатель, эксцентрики в комплекте. Источник: FRAP.");
        updated += updateMixer(pending, "F2225", "Смеситель для ванны FRAP F2225", "/images/catalog/mixer-bath-F2225.jpg",
                "Бренд: FRAP; Артикул: F2225; Серия: H25; Назначение: ванна и душ; Материал: латунь; Цвет: хром; Управление: двухвентильное; Монтаж: настенный; Излив: длинный, поворотный; переключатель на душ расположен в корпусе. Источник: FRAP.");
        updated += updateMixer(pending, "F2245", "Смеситель для ванны FRAP F2245 белый/хром", "/images/catalog/mixer-bath-F2245.jpg",
                "Бренд: FRAP; Артикул: F2245; Серия: H45; Назначение: ванна и душ; Материал: латунь; Цвет: белый/хром; Управление: однорычажное; Монтаж: настенный, 2 отверстия; Излив: поворотный, 35 см; Керамический картридж; шланг 150 см, лейка, держатель в комплекте. Источник: FRAP.");
        updated += updateMixer(pending, "F2258", "Смеситель для ванны FRAP F2258 белый/хром", "/images/catalog/mixer-bath-F2258.jpg",
                "Бренд: FRAP; Артикул: F2258; Назначение: ванна и душ; Материал: латунь; Цвет: белый/хром; Управление: однорычажное; Монтаж: настенный; Излив: поворотный; Керамический картридж; душевой шланг и лейка в комплекте. Источник: FRAP.");
        updated += updateMixer(pending, "F2260", "Смеситель для ванны FRAP F2260 хром", "/images/catalog/mixer-bath-F2260.jpg",
                "Бренд: FRAP; Артикул: F2260; Серия: H60; Назначение: ванна и душ; Материал: латунь; Цвет: хром; Управление: однорычажное; Монтаж: настенный; Излив: поворотный, 35 см; Картридж 35 мм; шланг 150 см, лейка, эксцентрики в комплекте. Источник: FRAP.");
        updated += updateMixer(pending, "G3218", "Смеситель для ванны GAPPO FUTURA G3218", "/images/catalog/mixer-bath-G3218.jpg",
                "Бренд: GAPPO; Артикул: G3218; Серия: FUTURA; Назначение: ванна и душ; Материал: латунь; Цвет: хром; Управление: однорычажное; Картридж 35 мм; Излив 192 мм поворотный, в изливе переключатель на лейку; настенный монтаж; шланг 150 см, лейка и держатель в комплекте. Источник: GAPPO.");
        return updated;
    }

    private int removeUnarticledBathtubs() {
        List<Product> withoutArticle = productRepository.findAll().stream()
                .filter(product -> "Ванны".equals(product.getCategory()))
                .filter(product -> blank(product.getArticle()))
                .toList();
        productRepository.deleteAll(withoutArticle);
        return withoutArticle.size();
    }

    private int updateBathtubs(List<Product> pending) {
        int updated = 0;
        updated += updateMixer(pending, "OV-521/180/2PL", "Ванна OVO CLARISE LINE 180×76×58 см, левая",
                "/images/catalog/bathtub-clarise.jpg",
                "Бренд: OVO; Артикул: OV-521/180/2PL; Коллекция: CLARISE LINE; Тип: отдельностоящая ванна на каркасе; Ориентация: левая; Размеры: 180×76×58 см; Цвет: белый. Источник фото и размеров: ФО-ОПТТОРГ.");
        updated += updateMixer(pending, "OV-521/180/2PR", "Ванна OVO CLARISE LINE 180×76×58 см, правая",
                "/images/catalog/bathtub-clarise.jpg",
                "Бренд: OVO; Артикул: OV-521/180/2PR; Коллекция: CLARISE LINE; Тип: отдельностоящая ванна на каркасе; Ориентация: правая; Размеры: 180×76×58 см; Цвет: белый. Источник фото и размеров: ФО-ОПТТОРГ.");
        updated += updateMixer(pending, "OV-521/170R", "Ванна OVO CLARISE 170×76×58 см, правая",
                "/images/catalog/bathtub-clarise.jpg",
                "Бренд: OVO; Артикул: OV-521/170R; Коллекция: CLARISE; Тип: отдельностоящая акриловая ванна на каркасе; Ориентация: правая; Размеры: 170×76×58 см. Источник фото и размеров: Muzaev.");
        updated += updateMixer(pending, "OV-526", "Ванна SONATA 170×80×60 см",
                "/images/catalog/bathtub-sonata.jpg",
                "Артикул: OV-526; Коллекция: SONATA; Тип: отдельностоящая ванна на каркасе; Размеры: 170×80×60 см; Цвет: белый. Характеристики взяты из исходной карточки товара.");
        return updated;
    }

    private int removeUnarticledBathroomFurniture() {
        List<Product> withoutArticle = productRepository.findAll().stream()
                .filter(product -> "Мебель для ванной".equals(product.getCategory()))
                .filter(product -> blank(product.getArticle())).toList();
        productRepository.deleteAll(withoutArticle);
        return withoutArticle.size();
    }

    private int updateBathroomFurniture(List<Product> pending) {
        int updated = 0;
        updated += updateMixer(pending, "tpASTER80.2Y4", "Тумба EQUIL Aster 80.2Y Амаретти с раковиной Палермо 80",
                "/images/catalog/bathroom-furniture-aster-80.jpg",
                "Бренд: EQUIL; Артикул: tpASTER80.2Y4; Коллекция: Aster; Тип: подвесная тумба с раковиной; Цвет: Амаретти; Размер тумбы: 792×472×540 мм; Размер комплекта с раковиной: 800×480×562 мм; Материалы: влагостойкий ЛДСП, МДФ, ПВХ-плёнка Soft Touch; Хранение: 2 выдвижных ящика с доводчиками; Раковина Палермо из кварцевого композита; сифон в комплекте. Источник: EQUIL.");
        updated += updateMixer(pending, "RD-031-100-WM SINK", "Столешница-моноблок 1000×500×150 мм с кронштейнами",
                "/images/catalog/bathroom-furniture-rd031.jpg",
                "Артикул: RD-031-100-WM SINK; Тип: столешница-моноблок с кронштейнами; Размеры: 1000×500×150 мм. Подробные сведения изготовителя в доступном каталоге не найдены.");
        return updated;
    }

    private int updateMixerCategories(List<Product> pending) {
        List<Product> products = new ArrayList<>(productRepository.findAll());
        for (Product product : pending) {
            if (product.getId() == null) {
                products.add(product);
            }
        }

        int updated = 0;
        List<Product> persistedUpdates = new ArrayList<>();
        for (Product product : products) {
            String category = product.getCategory();
            if (category == null || !category.startsWith("Смесители")) {
                continue;
            }

            String title = product.getTitle() == null ? "" : product.getTitle().toLowerCase();
            String targetCategory = title.contains("кухн") || title.contains("мойк")
                    ? "Смесители для кухни"
                    : title.contains("ванн") ? "Смесители для ванны" : "Смесители";
            if (!targetCategory.equals(category)) {
                product.setCategory(targetCategory);
                if (product.getId() == null) {
                    pending.add(product);
                } else {
                    persistedUpdates.add(product);
                }
                updated++;
            }
        }
        if (!persistedUpdates.isEmpty()) {
            productRepository.saveAll(persistedUpdates);
        }
        return updated;
    }

    private int updateMixer(List<Product> pending, String article, String title, String imageUrl, String description) {
        return productRepository.findFirstByArticle(article).map(product -> {
            Product target = pending.stream()
                    .filter(candidate -> article.equals(candidate.getArticle()))
                    .findFirst().orElse(product);
            target.setTitle(title);
            target.setImageUrl(imageUrl);
            target.setDescription(description);
            if (!pending.contains(target)) {
                productRepository.save(target);
            }
            return 1;
        }).orElse(0);
    }

    private int updateBathroomAccessory(List<Product> pending, String article, String title, String color,
                                        double price, String imageUrl, String details) {
        String description = "Бренд: " + (article.startsWith("R340") ? "RAGLO" : "GAPPO")
                + "; Артикул: " + article + "; " + details;
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Аксессуары для ванной".equals(product.getCategory())
                    || !color.equals(product.getColor()) || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription()) || product.getPrice() == null
                    || product.getPrice() != price || product.getStock() == null || product.getStock() != 0;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Аксессуары для ванной");
            product.setColor(color);
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(0);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
    }

    private String ragloDescription(String article, String color, int base) {
        return "Бренд: RAGLO; Артикул: " + article + "; Размер: " + base + "×50×20,8 см; "
                + "Материал: нержавеющая сталь SUS304; Цвет: " + color + "; Покрытие: PVD; "
                + "Форма: прямоугольная; Чаша: одна, глубина 208 мм; Толщина борта: 3 мм, чаши: 0,8 мм; "
                + "Два готовых отверстия (35 и 28 мм); Врезной монтаж или установка в один уровень; "
                + "Шумоизоляция и защита от вибрации; В комплекте крепления, слив 90 мм, сифон с подключением "
                + "посудомоечной машины, донный клапан, перелив, коландер и шаблон. Цена: официальный каталог RAGLO.";
    }

    private int updateKitchenSink(List<Product> pending, String article, String title, String color,
                                  Double price, String imageUrl, int width, String description) {
        return productRepository.findFirstByArticle(article).map(product -> {
            boolean changed = !title.equals(product.getTitle())
                    || !"Кухонные мойки".equals(product.getCategory())
                    || !color.equals(product.getColor()) || !imageUrl.equals(product.getImageUrl())
                    || !description.equals(product.getDescription())
                    || (price == null ? product.getPrice() != null : !price.equals(product.getPrice()))
                    || product.getWidth() == null || product.getWidth() != width;
            if (!changed) return 0;
            product.setTitle(title);
            product.setCategory("Кухонные мойки");
            product.setColor(color);
            product.setPrice(price);
            product.setImageUrl(imageUrl);
            product.setDescription(description);
            product.setWidth(width);
            product.setStock(0);
            product.setIsShowroom(false);
            pending.add(product);
            return 1;
        }).orElse(0);
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
