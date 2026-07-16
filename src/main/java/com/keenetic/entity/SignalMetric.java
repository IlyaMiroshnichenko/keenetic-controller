package com.keenetic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "signal_metrics") // Так будет называться таблица в H2 и в Postgres
public class SignalMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкрементный ID
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private int rsrp;
    private int sinr;
    private int rsrq;
    private int rssi;

    // Конструктор по умолчанию (обязателен для спецификации JPA/Hibernate)
    public SignalMetric() {}

    // Удобный конструктор для быстрого создания записи в коде
    public SignalMetric(int rsrp, int sinr, int rsrq, int rssi) {
        this.createdAt = LocalDateTime.now(); // Время замера проставится автоматически
        this.rsrp = rsrp;
        this.sinr = sinr;
        this.rsrq = rsrq;
        this.rssi = rssi;
    }
}
