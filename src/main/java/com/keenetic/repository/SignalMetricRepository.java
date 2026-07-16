package com.keenetic.repository;

import com.keenetic.entity.SignalMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalMetricRepository extends JpaRepository<SignalMetric, Long> {
}
