package com.wh.controller;

import com.wh.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        return "cart";
    }

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable("id") Long id) {
        cartService.addProduct(id);
        return "redirect:/cart";
    }

    @PostMapping("/cart/increase/{id}")
    public String increase(@PathVariable("id") Long id) {
        cartService.increaseQuantity(id);
        return "redirect:/cart";
    }

    @PostMapping("/cart/decrease/{id}")
    public String decrease(@PathVariable("id") Long id) {
        cartService.decreaseQuantity(id);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        cartService.removeItem(id);
        return "redirect:/cart";
    }
}