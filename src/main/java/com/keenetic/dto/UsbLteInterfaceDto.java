package com.keenetic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsbLteInterfaceDto(
        String rssi,
        String rsrp,
        String cinr,
        String rsrq
) {
}
