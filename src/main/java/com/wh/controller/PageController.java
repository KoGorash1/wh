package com.wh.controller;

import com.wh.model.Product;
import com.wh.repository.ProductRepository;
import com.wh.model.ShopSettings;
import com.wh.repository.ShopSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ShopSettingsRepository settingsRepository;

    // Главная страница
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Страница каталога
    @GetMapping("/catalog")
    public String catalog(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "catalog";
    }

    // Страница детального просмотра товара
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/catalog";
        }
        model.addAttribute("product", product);
        return "product-detail";
    }

    // Оформление заказа (корзина лежит в браузере, форма отправляется в /api/orders)
    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("shopSettings", settingsRepository.findById(1L).orElseGet(ShopSettings::new));
        return "checkout";
    }

    // Страница успешного оформления заказа
    @GetMapping("/order-success")
    public String orderSuccess() {
        return "order-success";
    }

    // Старая ссылка из прежней заглушки корзины
    @GetMapping("/order/checkout")
    public String legacyCheckout() {
        return "redirect:/checkout";
    }
}
