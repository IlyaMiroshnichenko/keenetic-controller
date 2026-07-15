package com.keenetic.controller;

import com.keenetic.dto.UsbLteInterfaceDto;
import com.keenetic.service.KeeneticClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class KeeneticRestController {

    private final KeeneticClientService keeneticClientService;

    public KeeneticRestController(KeeneticClientService keeneticClientService) {
        this.keeneticClientService = keeneticClientService;
    }

    @GetMapping(value = "/keenetic/api/v1/getMobileSignalInfo", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Object> getMobileSignalInfo() {
        try {
            log.info("Request mobile signal information.");
            UsbLteInterfaceDto result = keeneticClientService.getMobileSignalInfo();

            if (result == null) {
                log.warn("Router returns OK, but body is empty.");
                return new ResponseEntity<>(HttpStatus.OK);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during request", e);
            return new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
