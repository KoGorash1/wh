
package com.wh.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Выводит в консоль ссылки на главную страницу
 * и JSON товаров при запуске приложения.
 */
@Slf4j
@Component
public class StartupLinks {

    private final Environment env;

    public StartupLinks(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printLinks() {
        String port = env.getProperty(
                "local.server.port",
                env.getProperty("server.port", "8080")
        );

        String ctx = env.getProperty(
                "server.servlet.context-path", ""
        );

        if (ctx.equals("/")) {
            ctx = "";
        }

        String base = "http://localhost:" + port + ctx;

        log.info("");
        log.info("========================================================");
        log.info("Приложение «Белый Дом» запущено");
        log.info("  Главная:      {}/", base);
        log.info("  JSON товаров: {}/api/products", base);
        log.info("========================================================");
    }
}
