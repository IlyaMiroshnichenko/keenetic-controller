package com.keenetic.config;

import com.keenetic.telegram.KeeneticTelegramBot;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    private final KeeneticTelegramBot keeneticTelegramBot;

    public TelegramBotConfig(KeeneticTelegramBot keeneticTelegramBot) {
        this.keeneticTelegramBot = keeneticTelegramBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(keeneticTelegramBot);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Не удалось запустить Long Polling сессию", e);
        }
    }
}
