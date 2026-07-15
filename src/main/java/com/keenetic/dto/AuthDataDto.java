package com.keenetic.dto;

public record AuthDataDto(
        String challengeHeader,
        String realmHeader,
        String cookie
) {
}
