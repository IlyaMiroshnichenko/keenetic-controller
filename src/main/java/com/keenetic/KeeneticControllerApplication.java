package com.keenetic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.keenetic")
public class KeeneticControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeeneticControllerApplication.class, args);
    }

}
