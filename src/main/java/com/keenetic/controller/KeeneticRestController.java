package com.keenetic.controller;

import com.keenetic.service.KeeneticClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class KeeneticRestController {

    private final KeeneticClient keeneticClient;

    public KeeneticRestController(KeeneticClient keeneticClient) {
        this.keeneticClient = keeneticClient;
    }

    // Явно указываем производимый контент JSON в UTF-8
    @GetMapping(value = "/router-api/show-hosts", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getHosts() {
        try {
            String result = keeneticClient.getHostsCommand();

            if (result == null || result.isEmpty()) {
                System.out.println("[ВНИМАНИЕ] Роутер вернул статус 200, но тело по-прежнему пустое.");
                return ResponseEntity.ok("{}");
            }

            System.out.println("[УСПЕХ] Данные hotspot получены! Длина: " + result.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
