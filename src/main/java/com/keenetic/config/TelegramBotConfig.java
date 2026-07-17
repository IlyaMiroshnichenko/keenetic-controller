package com.keenetic.config;

import com.keenetic.telegram.KeeneticTelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    private final KeeneticTelegramBot keeneticTelegramBot;

    @Value("${telegram.proxy.host:}")
    private String proxyHost;

    @Value("${telegram.proxy.port:0}")
    private int proxyPort;

    // @Lazy — это магия Spring. Она говорит: "Создай этот конфиг сразу,
    // а самого бота подтяни чуть позже, когда он полностью соберется с прокси-опциями".
    // Это на 100% предотвращает ошибку циклической зависимости (Circular Dependency).
    public TelegramBotConfig(@Lazy KeeneticTelegramBot keeneticTelegramBot) {
        this.keeneticTelegramBot = keeneticTelegramBot;
    }

    @Bean
    public DefaultBotOptions botOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        // Если в настройках передан прокси — включаем его для обхода блокировки RUVDS
        if (!proxyHost.isEmpty() && proxyPort > 0) {
            options.setProxyHost(proxyHost);
            options.setProxyPort(proxyPort);
            options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        }
        return options;
    }

    // Аргумент убираем, теперь метод слушает событие, а бота берет из поля класса
    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            // Создаем API-клиент Telegram и регистрируем сессию нашего настроенного бота
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(keeneticTelegramBot);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Не удалось запустить Long Polling сессию для Telegram бота", e);
        }
    }
}
