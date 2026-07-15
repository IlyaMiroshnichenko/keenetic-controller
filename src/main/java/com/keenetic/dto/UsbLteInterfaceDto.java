package com.keenetic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsbLteInterfaceDto(
        String rssi,
        String rsrp,
        @JsonProperty("cinr") String sinr,
        String rsrq
) {
}
