package com.keenetic.telegram;

import com.keenetic.dto.UsbLteInterfaceDto;
import com.keenetic.service.KeeneticClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class KeeneticTelegramBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botName;
    private final KeeneticClientService keeneticClientService;

    public KeeneticTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.name}") String botName,
            @Value("${telegram.base.url:https://telegram.org}") String baseUrl,
            KeeneticClientService keeneticClientService) {

        // Передаем в super объект опций с нашим незашифрованным HTTP URL
        super(createBotOptions(baseUrl));

        this.botToken = botToken;
        this.botName = botName;
        this.keeneticClientService = keeneticClientService;

        log.info("Telegram бот успешно переключен на HTTP-туннель: {}", baseUrl);
    }

    private static DefaultBotOptions createBotOptions(String baseUrl) {
        DefaultBotOptions options = new DefaultBotOptions();
        if (baseUrl != null && !baseUrl.contains("api.telegram.org")) {
            options.setBaseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        }
        return options;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getFrom().getUserName();

            if (username == null || !username.equals("IlyaMiroshnichenko")) {
                sendMessage(chatId, "Недостаточно прав для выполнения операции.", createMainMenuKeyboard());
                return;
            }

            log.info("Получено сообщение от @{}: '{}'", username, messageText);

            switch (messageText) {
                case "/start":
                    sendWelcomeMessage(chatId);
                    break;
                case "📊 Статус сигнала":
                    try {
                        sendSignalInfo(chatId);
                    } catch (Exception e) {
                        log.error("Ошибка при получении данных с Keenetic: {}", e.getMessage());
                        sendMessage(chatId, "⚠️ Не удалось получить данные с роутера. Проверьте сеть.", createMainMenuKeyboard());
                    }
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда. Используйте кнопку ниже.", createMainMenuKeyboard());
                    break;
            }
        }
    }

    // --- ЛОГИКА ОТВЕТОВ ---
    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Привет! Я бот для мониторинга роутера Keenetic.\nНажмите на кнопку ниже, чтобы проверить сигнал.";
        sendMessage(chatId, welcomeText, createMainMenuKeyboard());
    }

    private void sendSignalInfo(long chatId) throws Exception {
        log.info("Запрошена информация о сигнале для chatId: {}", chatId);
        UsbLteInterfaceDto mobileSignalInfo = keeneticClientService.getMobileSignalInfo();

        String response = """
                📶 *Параметры LTE сигнала:*
                -------------------------------------
                • Статус: *Connected*
                • RSSI:\s""" + mobileSignalInfo.rssi() + """
                \n• RSRP:\s""" + mobileSignalInfo.rsrp() + """
                \n• RSRQ:\s""" + mobileSignalInfo.rsrq() + """
                \n• SINR:\s""" + mobileSignalInfo.sinr() + """
                """;
        sendMessage(chatId, response, createMainMenuKeyboard());
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ОТПРАВКИ И КЛАВИАТУРЫ ---
    private void sendMessage(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboardMarkup != null) {
            message.setReplyMarkup(keyboardMarkup);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в Telegram для chatId {}: {}", chatId, e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("📊 Статус сигнала"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public void sendRawMessage(long chatId, String text) {
        sendMessage(chatId, text, createMainMenuKeyboard());
    }
}
