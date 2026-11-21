package com.wh.controller;

import com.wh.model.ShopSettings;
import com.wh.repository.ShopSettingsRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ShopSettingsAdvice {
    private final ShopSettingsRepository settingsRepository;

    public ShopSettingsAdvice(ShopSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @ModelAttribute("shopSettings")
    public ShopSettings shopSettings() {
        return settingsRepository.findById(1L).orElseGet(ShopSettings::new);
    }
}
