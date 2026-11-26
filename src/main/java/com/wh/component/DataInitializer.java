
package com.wh.component;

import com.wh.model.Product;
import com.wh.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация для первоначального заполнения каталога тестовыми товарами.
 * <p>
 * Значения width, kit, style, oldPrice и badge используются как тестовые данные
 * для проверки работы фильтров. После получения реального прайс-листа их следует
 * заменить данными заказчика.
 * <p>
 * Допустимые значения:
 * <ul>
 *     <li>width — 60, 70, 80, 90, 100, 120 см;</li>
 *     <li>kit — tumba, mirror, penal, full;</li>
 *     <li>style — modern, classic, loft, minimal;</li>
 *     <li>badge — new, sale.</li>
 * </ul>
 */
@Configuration
public class DataInitializer {

    /**
     * Временные изображения товаров. В дальнейшем их следует заменить
     * фотографиями, подготовленными дизайнером.
     */
    private static final String IMG_SINK =
            "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=80";

    private static final String IMG_SINK_PLAST = "/images/catalog/sanita-fest-80.jpg";

    private static final String IMG_MIX =
            "https://images.unsplash.com/photo-1584622749010-6811282666f3?auto=format&fit=crop&w=1200&q=80";

    private static final String IMG_SHOWER =
            "https://images.unsplash.com/photo-1620626011761-996317b8d101?auto=format&fit=crop&w=1200&q=80";

    private static final String IMG_WC =
            "https://images.unsplash.com/photo-1507652313519-d4e9174996dd?auto=format&fit=crop&w=1200&q=80";

    /**
     * Создаёт и сохраняет начальный набор товаров при запуске приложения.
     *
     * @param productRepository репозиторий для удаления и сохранения товаров
     * @return обработчик запуска приложения, выполняющий заполнение каталога
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CommandLineRunner loadData(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                System.out.println("Каталог уже заполнен; начальные товары не перезаписывались.");
                return;
            }

            List<Product> products = buildProducts();
            productRepository.saveAll(products);

            System.out.println("Загружено товаров в каталог: " + products.size());
        };
    }

    /**
     * Формирует список тестовых товаров для основных категорий каталога.
     *
     * @return список подготовленных товаров
     */
    private List<Product> buildProducts() {
        List<P> l = new ArrayList<>();

        // ---- Раковины ----
        l.add(p("Раковина «Пласт»", "Раковины", 6649, IMG_SINK_PLAST)
                .showroom().w(80).style("modern").old(7130).badge("sale")
                .article("00000055861")
                .description("Серия: SANITA FEST 80; Форма раковины: прямоугольная; Монтаж раковины: подвесная; "
                        + "Расположение крыла: без крыла; Количество чаш: 1 шт; "
                        + "Для людей с ограниченными возможностями: нет; Расположение смесителя: посередине; "
                        + "Отверстие для перелива: есть; Количество отверстий под смеситель: 1; "
                        + "Цвет раковины: белый; Материал раковины: санфарфор; Поверхность: глянцевая; "
                        + "Покрытие: без покрытия; Мыльница: нет; Полотенцедержатель: нет; Донный клапан: нет; "
                        + "Высота раковины: 220 мм; Ширина раковины: 800 мм; Глубина раковины: 455 мм; "
                        + "Стиль: современный; Артикул производителя: WB.FN/Fest/80-C/WHT.G/S1; "
                        + "Гарантия производителя: 15 лет."));
        l.add(p("Раковина Sanita Аттика 48", "Раковины", 2424, "/images/catalog/sanita-attica-48.jpeg")
                .w(48).style("modern").old(3244).badge("sale").article("WB.PD/Attica/48-C/WHT.G/S1")
                .description("Серия: SANITA Аттика 48; Форма раковины: овальная; Монтаж: подвесной; Количество чаш: 1; "
                        + "Расположение крыла: без крыла; Расположение смесителя: по центру; "
                        + "Готовые отверстия под смеситель: 1; Перелив: есть; Цвет: белый; Материал: санфарфор; "
                        + "Поверхность: глянцевая; Покрытие: без покрытия; Донный клапан: нет; "
                        + "Полотенцедержатель: нет; Пьедестал: не входит в комплект; "
                        + "Высота: 150 мм; Ширина: 480 мм; Глубина: 415 мм; Стиль: современный; Вес: 10,6 кг; "
                        + "Артикул: WB.PD/Attica/48-C/WHT.G/S1; Гарантия: 5 лет."));
        l.add(p("Раковина Sanita Самарская 57", "Раковины", 2781, "/images/catalog/sanita-samarsky-57-1.jpeg")
                .style("modern").old(3911).badge("sale").article("WB.PD/Samarsky/57-C/WHT.G/S1")
                .description("Серия: SANITA Самарское; Форма раковины: прямоугольная; Монтаж: подвесной или на пьедестал; "
                        + "Количество чаш: 1; Ориентация: универсальная; Расположение крыла: без крыла; "
                        + "Расположение смесителя: по центру; Готовые отверстия под смеситель: 1; Перелив: есть; "
                        + "Цвет: белый; Материал: санфарфор; Поверхность: глянцевая; Покрытие: без покрытия; "
                        + "Донный клапан: нет; Полотенцедержатель: нет; Пьедестал: не входит в комплект; "
                        + "Высота: 220 мм; Ширина: 575 мм; Глубина: 445 мм; Стиль: современный; Вес: 20 кг; "
                        + "Артикул: WB.PD/Samarsky/57-C/WHT.G/S1; Гарантия: 5 лет."));
        l.add(p("Раковина Next Luxe 60", "Раковины", 3182, "/images/catalog/sanita-next-luxe-60-1.jpeg")
                .style("modern").old(4320).badge("sale").article("WB.PD/Next/60-C/WHT.G/S1")
                .description("Серия: SANITA LUXE Next 60; Форма: прямоугольная; Монтаж: подвесной или на пьедестал; "
                        + "Количество чаш: 1; Ориентация: универсальная; Расположение крыла: без крыла; "
                        + "Расположение смесителя: по центру; Готовые отверстия под смеситель: 1; Перелив: есть; "
                        + "Цвет: белый; Материал: санфарфор; Поверхность: глянцевая; Донный клапан: нет; "
                        + "Полотенцедержатель: нет; Пьедестал: не входит в комплект; Высота: 214 мм; "
                        + "Ширина: 610 мм; Глубина: 454 мм; Стиль: современный; Вес: 27,8 кг; "
                        + "Артикул: WB.PD/Next/60-C/WHT.G/S1; Гарантия: 15 лет; Наличие у поставщика: под заказ."));
        l.add(p("Раковина Sanita Classic Luxe 80", "Раковины", 4680, "/images/catalog/sanita-classic-luxe-80-1.webp")
                .w(82).style("modern").old(5184).badge("sale").article("WB.FN/Classic/80-C/WHT.G/S1")
                .description("Серия: Sanita Classic Luxe; Форма: прямоугольная; Монтаж: на тумбу; "
                        + "Количество чаш: 1; Расположение крыла: с обеих сторон; Для людей с ограниченными возможностями: нет; "
                        + "Расположение смесителя: посередине; Отверстие для перелива: есть; Отверстий под смеситель: 1; "
                        + "Цвет: белый; Материал: санфарфор; Поверхность: глянцевая; Покрытие: без покрытия; "
                        + "Мыльница: нет; Полотенцедержатель: нет; Донный клапан: нет; Высота: 210 мм; "
                        + "Ширина: 825 мм; Глубина: 515 мм; Стиль: современный; Вес в упаковке: 25 кг; "
                        + "Комплектация: умывальник, заглушка для перелива, инструкция, упаковка; "
                        + "Артикул: WB.FN/Classic/80-C/WHT.G/S1; Гарантия: 15 лет."));
        l.add(p("Раковина «Круг»", "Раковины", 32000, IMG_SINK).w(60).style("modern"));
        l.add(p("Раковина «Квадрат»", "Раковины", 41000, IMG_SINK).w(70).style("minimal"));
        l.add(p("Раковина «Капля»", "Раковины", 55000, IMG_SINK).w(90).style("modern").badge("new"));
        l.add(p("Раковина «Слон»", "Раковины", 28000, IMG_SINK).w(60).style("classic").old(33000).badge("sale"));
        l.add(p("Раковина «Мини»", "Раковины", 19500, IMG_SINK).w(60).style("modern"));

        // ---- Смесители ----
        l.add(p("Смеситель «Арка»", "Смесители", 12400, IMG_MIX).style("modern"));
        l.add(p("Смеситель «Волна»", "Смесители", 15800, IMG_MIX).style("modern"));
        l.add(p("Смеситель «Классик»", "Смесители", 11200, IMG_MIX).style("classic"));
        l.add(p("Смеситель «Термо»", "Смесители", 28500, IMG_MIX).style("modern"));
        l.add(p("Смеситель «Водопад»", "Смесители", 22000, IMG_MIX).style("minimal"));
        l.add(p("Смеситель «Эко»", "Смесители", 13500, IMG_MIX).style("modern").old(15900).badge("sale"));

        // ---- Смесители для кухни ----
        l.add(p("Кухонный смеситель «Высокий»", "Смесители для кухни", 18500, IMG_MIX).style("modern"));
        l.add(p("Кухонный смеситель «С выдвижным»", "Смесители для кухни", 24000, IMG_MIX).style("modern"));
        l.add(p("Кухонный смеситель «Термо»", "Смесители для кухни", 32000, IMG_MIX).style("modern"));
        l.add(p("Кухонный смеситель «Мини»", "Смесители для кухни", 12500, IMG_MIX).style("minimal"));
        l.add(p("Кухонный смеситель «Профи»", "Смесители для кухни", 27500, IMG_MIX).style("loft"));

        // ---- Смесители для ванны ----
        l.add(p("Ванный смеситель «Термо»", "Смесители для ванны", 35000, IMG_MIX).style("modern"));
        l.add(p("Ванный смеситель «На колонну»", "Смесители для ванны", 22000, IMG_MIX).style("classic"));
        l.add(p("Ванный смеситель «На бортик»", "Смесители для ванны", 16500, IMG_MIX).style("classic"));
        l.add(p("Ванный смеситель «С душем»", "Смесители для ванны", 19000, IMG_MIX).style("modern"));
        l.add(p("Ванный смеситель «Дизайнерский»", "Смесители для ванны", 42000, IMG_MIX).style("minimal").badge("new"));

        // ---- Душевые стойки ----
        l.add(p("Душевая стойка «Базовая»", "Душевые стойки", 12000, IMG_SHOWER).style("minimal"));
        l.add(p("Душевая стойка «Тропический душ»", "Душевые стойки", 28000, IMG_SHOWER).style("modern"));
        l.add(p("Душевая стойка «Термо»", "Душевые стойки", 38000, IMG_SHOWER).style("modern"));
        l.add(p("Душевая стойка «С полкой»", "Душевые стойки", 15500, IMG_SHOWER).style("modern").old(18000).badge("sale"));
        l.add(p("Душевая стойка «Премиум»", "Душевые стойки", 45000, IMG_SHOWER).style("minimal"));

        // ---- Душевые системы ----
        l.add(p("Душевая система Rain", "Душевые системы", 34000, IMG_SHOWER).showroom().style("modern").badge("new"));
        l.add(p("Душевая панель «Премиум»", "Душевые системы", 52000, IMG_SHOWER).style("minimal"));
        l.add(p("Душевая система «С полкой»", "Душевые системы", 28000, IMG_SHOWER).style("modern"));
        l.add(p("Душевая система «Хром»", "Душевые системы", 35000, IMG_SHOWER).style("classic"));
        l.add(p("Душевая система «Черная»", "Душевые системы", 38000, IMG_SHOWER).style("loft"));

        // ---- Унитазы ----
        l.add(p("Унитаз-компакт Nord", "Унитазы", 26500, IMG_WC).style("modern"));
        l.add(p("Унитаз «Моноблок»", "Унитазы", 42000, IMG_WC).style("classic"));
        l.add(p("Унитаз «Подвесной»", "Унитазы", 35000, IMG_WC).style("minimal"));
        l.add(p("Биде «Классик»", "Унитазы", 18000, IMG_WC).style("classic"));
        l.add(p("Биде-сиденье «Электронное»", "Унитазы", 28000, IMG_WC).style("modern"));

        // ---- Мебель ----
        l.add(p("Тумба подвесная Wood", "Мебель", 54000, IMG_SINK).w(80).kit("tumba").style("loft"));
        l.add(p("Тумба «Двухстворчатая»", "Мебель", 48000, IMG_SINK).w(100).kit("tumba").style("modern"));
        l.add(p("Тумба «С раковиной»", "Мебель", 62000, IMG_SINK).w(120).kit("tumba").style("minimal"));
        l.add(p("Пенал «Высокий»", "Мебель", 55000, IMG_SINK).w(60).kit("penal").style("modern"));
        l.add(p("Пенал «С зеркалом»", "Мебель", 47000, IMG_SINK).w(60).kit("mirror").style("classic"));
        l.add(p("Тумба «На ножках»", "Мебель", 38000, IMG_SINK).w(80).kit("tumba").style("classic").old(42000).badge("sale"));
        l.add(p("Пенал «Угловой»", "Мебель", 41000, IMG_SINK).w(70).kit("penal").style("minimal"));
        l.add(p("Комод «Ванный»", "Мебель", 33000, IMG_SINK).w(90).kit("full").style("loft"));

        // ---- Зеркала ----
        l.add(p("Зеркало «Контур»", "Зеркала", 18900, IMG_WC).showroom().w(60).style("modern").badge("new"));
        l.add(p("Зеркало «Круглое»", "Зеркала", 15000, IMG_WC).w(70).style("minimal"));
        l.add(p("Зеркало «С подсветкой»", "Зеркала", 22000, IMG_WC).w(80).style("modern"));
        l.add(p("Зеркало «Антизапотевание»", "Зеркала", 28000, IMG_WC).w(90).style("modern"));
        l.add(p("Зеркало «В нишу»", "Зеркала", 12000, IMG_WC).w(60).style("minimal"));
        l.add(p("Зеркало «Овальное»", "Зеркала", 18500, IMG_WC).w(70).style("classic"));
        l.add(p("Зеркало «Трехстворчатое»", "Зеркала", 35000, IMG_WC).w(120).style("classic"));

        // ---- Ванны ----
        l.add(p("Ванна акриловая Oval", "Ванны", 89000, IMG_MIX).style("modern"));
        l.add(p("Ванна «Прямоугольная»", "Ванны", 75000, IMG_MIX).style("minimal"));
        l.add(p("Ванна «Чаша»", "Ванны", 120000, IMG_MIX).style("modern").badge("new"));
        l.add(p("Ванна «Угловая»", "Ванны", 65000, IMG_MIX).style("classic").old(72000).badge("sale"));
        l.add(p("Ванна «С чашей»", "Ванны", 95000, IMG_MIX).style("modern"));

        // ---- Водонагреватели ----
        l.add(p("Водонагреватель Slim", "Водонагреватели", 21500, IMG_SHOWER));
        l.add(p("Водонагреватель «50 литров»", "Водонагреватели", 18000, IMG_SHOWER));
        l.add(p("Водонагреватель «80 литров»", "Водонагреватели", 25000, IMG_SHOWER));
        l.add(p("Водонагреватель «Проточный»", "Водонагреватели", 14000, IMG_SHOWER).old(16000).badge("sale"));

        // ---- Аксессуары ----
        l.add(p("Набор аксессуаров Black", "Аксессуары", 9800, IMG_WC).style("loft"));
        l.add(p("Держатель для полотенец", "Аксессуары", 4500, IMG_WC).style("modern"));
        l.add(p("Мыльница «Настенная»", "Аксессуары", 2800, IMG_WC).style("minimal"));
        l.add(p("Дозатор «Для геля»", "Аксессуары", 3200, IMG_WC).style("minimal"));
        l.add(p("Полка «Для шампуней»", "Аксессуары", 5500, IMG_WC).style("modern"));
        l.add(p("Ерш «Настенный»", "Аксессуары", 2200, IMG_WC).style("loft"));
        l.add(p("Вешалка «Для халатов»", "Аксессуары", 3800, IMG_WC).style("classic"));

        List<Product> result = new ArrayList<>();

        for (P x : l) {
            result.add(x.product);
        }

        return result;
    }

    /**
     * Создаёт вспомогательный объект для последовательного заполнения товара.
     *
     * @param title название товара
     * @param category категория товара
     * @param price цена товара
     * @param imageUrl адрес изображения товара
     * @return объект-построитель товара
     */
    private static P p(String title, String category, double price, String imageUrl) {
        return new P(title, category, price, imageUrl);
    }

    /**
     * Вспомогательный построитель, позволяющий задавать свойства товара
     * цепочкой вызовов методов.
     */
    private static final class P {

        /** Создаваемый товар. */
        final Product product = new Product();

        /**
         * Создаёт товар и задаёт его основные свойства.
         *
         * @param title название товара
         * @param category категория товара
         * @param price цена товара
         * @param imageUrl адрес изображения товара
         */
        P(String title, String category, double price, String imageUrl) {
            product.setTitle(title);
            product.setCategory(category);
            product.setPrice(price);
            product.setImageUrl(imageUrl);
            product.setIsShowroom(false);
            product.setStock(10);
        }

        /** Отмечает товар как представленный в шоуруме. */
        P showroom() {
            product.setIsShowroom(true);
            return this;
        }

        /**
         * Задаёт ширину товара.
         *
         * @param cm ширина в сантиметрах
         * @return текущий построитель
         */
        P w(int cm) {
            product.setWidth(cm);
            return this;
        }

        /**
         * Задаёт комплектацию товара.
         *
         * @param v код комплектации
         * @return текущий построитель
         */
        P kit(String v) {
            product.setKit(v);
            return this;
        }

        /**
         * Задаёт стиль товара.
         *
         * @param v название стиля
         * @return текущий построитель
         */
        P style(String v) {
            product.setStyle(v);
            return this;
        }

        /**
         * Задаёт прежнюю цену товара.
         *
         * @param v прежняя цена
         * @return текущий построитель
         */
        P old(double v) {
            product.setOldPrice(v);
            return this;
        }

        /**
         * Задаёт метку товара.
         *
         * @param v код метки
         * @return текущий построитель
         */
        P badge(String v) {
            product.setBadge(v);
            return this;
        }

        P article(String v) {
            product.setArticle(v);
            return this;
        }

        P description(String v) {
            product.setDescription(v);
            return this;
        }
    }
}
