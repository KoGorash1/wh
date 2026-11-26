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
                "Напольный или настенный ёршик для унитаза; настенный держатель с крышкой; отделка — сатиновое золото; материалы: латунь и пластик; высота около 49 см, диаметр около 8 см; гарантия 1 год.");
        updated += updateBathroomAccessory(pending, "G904-9", "Ёршик для унитаза GAPPO G904-9",
                "оружейная сталь", 8809, "/images/catalog/g904-9-1.png",
                "Напольный или настенный ёршик для унитаза; держатель с крышкой; отделка — оружейная сталь; металлическое основание и ручка, пластиковая щётка; комплект крепления для монтажа на стену.");
        updated += updateBathroomAccessory(pending, "G906-3", "Многофункциональная стойка GAPPO G906-3",
                "сатиновое золото", 14958, "/images/catalog/g906-3-1.png",
                "Напольная многофункциональная стойка для ванной комнаты; держатель туалетной бумаги, полка и ёршик; отделка — сатиновое золото; материалы: латунь и пластик.");
        updated += updateBathroomAccessory(pending, "G906-9", "Многофункциональная стойка GAPPO G906-9",
                "оружейная сталь", 14224, "/images/catalog/g906-9-1.png",
                "Напольная многофункциональная стойка для ванной комнаты; держатель туалетной бумаги, полка и ёршик; отделка — оружейная сталь; сочетание металлических и пластиковых деталей.");
        updated += updateBathroomAccessory(pending, "R340.02", "Полка RAGLO R340.02 с крючками",
                "хром", 3884, "/images/catalog/r340-02-1.jpg",
                "Прямая настенная полка для ванны и душа с двумя боковыми крючками; 29×11,5×5,5 см; нержавеющая сталь SUS304, хром; скрытый монтаж; допустимая нагрузка до 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.02.09", "Полка RAGLO R340.02.09 с крючками",
                "графит", 4472, "/images/catalog/r340-02-09-1.jpg",
                "Прямая настенная полка для ванны и душа с двумя боковыми крючками; 29×11,5×5,5 см; нержавеющая сталь SUS304, графитовое покрытие; скрытый монтаж; допустимая нагрузка до 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.03.03", "Полка RAGLO R340.03.03",
                "сатиновое золото", 5207, "/images/catalog/r340-03-03-1.jpg",
                "Прямая настенная полка для ванной комнаты; 40×10×8 см; нержавеющая сталь SUS304, сатиновое золотое покрытие; скрытый монтаж; допустимая нагрузка до 5 кг.");
        updated += updateBathroomAccessory(pending, "R340.03.09", "Полка RAGLO R340.03.09",
                "графит", 5295, "/images/catalog/r340-03-09-1.jpg",
                "Прямая настенная полка для ванной комнаты; 40×10×8 см; нержавеющая сталь SUS304, графитовое покрытие; скрытый монтаж; допустимая нагрузка до 5 кг.");
        return updated;
    }

    private int updateBathroomAccessory(List<Product> pending, String article, String title, String color,
                                        double price, String imageUrl, String details) {
        String description = "Бренд: " + (article.startsWith("R340") ? "RAGLO" : "GAPPO")
                + "; Артикул: " + article + "; " + details + "; Цена по каталогу производителя.";
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
