package com.keenetic.service;

import com.keenetic.telegram.KeeneticTelegramBot;
import com.keenetic.dto.UsbLteInterfaceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class SignalMonitoringService {

    private final KeeneticClientService keeneticClientService;
    private final KeeneticTelegramBot telegramBot;

    @Value("${keenetic.signal-thresholds.min-rsrp}")
    private int minRsrp;

    @Value("${keenetic.signal-thresholds.min-sinr}")
    private int minSinr;

    @Value("${telegram.admin-chat-id}")
    private long adminChatId;

    public SignalMonitoringService(KeeneticClientService keeneticClientService, KeeneticTelegramBot telegramBot) {
        this.keeneticClientService = keeneticClientService;
        this.telegramBot = telegramBot;
    }

    // Запуск каждые 5 минут (300 000 миллисекунд).
    // initialDelay дает Spring 10 секунд на старт, прежде чем дергать роутер в первый раз
    @Scheduled(fixedRate = 10000, initialDelay = 5000)
    public void monitorSignalTask() {
        log.info("Запуск планового мониторинга 4G-сигнала...");
        try {
            // Вызываем ваш рабочий метод
            UsbLteInterfaceDto mobileInfo = keeneticClientService.getMobileSignalInfo();

            if (mobileInfo == null) {
                log.warn("Метрики LTE не получены от роутера (интерфейс пуст или lte=null)");
                return;
            }

            if (!StringUtils.hasLength(mobileInfo.rsrp()) || !StringUtils.hasLength(mobileInfo.sinr())) {
                log.warn("В JSON роутера отсутствуют ключи rsrp или sinr");
                return;
            }

            int currentRsrp = Integer.parseInt(mobileInfo.rsrp());
            int currentSinr = Integer.parseInt(mobileInfo.sinr());

            log.debug("Плановый замер: RSRP = {} dBm, SINR = {} dB", currentRsrp, currentSinr);

            // Проверяем, пробиты ли пороги качества
            boolean isBadRsrp = currentRsrp < minRsrp;
            boolean isBadSinr = currentSinr < minSinr;

            if (isBadRsrp || isBadSinr) {
                log.warn("⚠️ ВНИМАНИЕ: Зафиксирован плохой уровень сигнала! RSRP: {}, SINR: {}", currentRsrp, currentSinr);

                // Формируем красивый алерт с эмодзи
                String alertMessage = String.format("""
                        🚨 *ВНИМАНИЕ! Ухудшение сигнала 4G модема!*
                        ------------------------------------------
                        • Текущий RSRP: *%d dBm* %s (Порог: %d)
                        • Текущий SINR: *%d dB* %s (Порог: %d)
                        
                        _Проверьте антенну или положение роутера Skipper 4G._
                        """,
                        currentRsrp, (isBadRsrp ? "🔴" : "🟢"), minRsrp,
                        currentSinr, (isBadSinr ? "🔴" : "🟢"), minSinr
                );

                // Отправляем напрямую в чат админа через метод вашего бота
                telegramBot.sendRawMessage(adminChatId, alertMessage);
            } else {
                log.info("Мониторинг завершен: показатели сигнала в пределах нормы.");
            }

        } catch (Exception e) {
            log.error("Ошибка при плановом мониторинге сигнала Keenetic: {}", e.getMessage(), e);
        }
    }
}
