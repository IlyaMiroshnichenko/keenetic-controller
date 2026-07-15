package com.keenetic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.keenetic")
@EnableScheduling
public class KeeneticControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeeneticControllerApplication.class, args);
    }

}
