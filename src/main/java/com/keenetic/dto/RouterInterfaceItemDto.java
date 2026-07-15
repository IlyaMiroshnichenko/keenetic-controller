package com.keenetic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouterInterfaceItemDto(
        @JsonProperty("UsbLte0") UsbLteInterfaceDto usbLteInterface
) {
}
