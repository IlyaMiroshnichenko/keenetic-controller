package com.keenetic.service;

import com.keenetic.dto.AuthDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class KeeneticAuthorizationService {

    private static final String FIXED_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KeeneticClient/1.0";

    @Value("${keenetic.ip}")
    private String routerIp;

    @Value("${keenetic.login}")
    private String login;

    @Value("${keenetic.password}")
    private String password;

    public AuthDataDto getAuthData() throws Exception {
        String urlAuth = "http://" + routerIp + "/auth";

        // Создаем чистый клиент БЕЗ CookieManager, чтобы избежать магии потоков Spring Boot
        HttpClient client = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // --- ШАГ 1: Challenge ---
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(urlAuth))
                .header("User-Agent", FIXED_USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());

        // Извлекаем сырую куку сессии, которую выдал роутер
        List<String> setCookies = response1.headers().allValues("Set-Cookie");
        String keeneticCookie = setCookies.isEmpty() ? "" : setCookies.getFirst().split(";")[0];

        Optional<String> challengeOpt = response1.headers().firstValue("X-NDM-Challenge")
                .or(() -> response1.headers().firstValue("x-ndm-challenge"));
        Optional<String> realmOpt = response1.headers().firstValue("X-NDM-Realm")
                .or(() -> response1.headers().firstValue("x-ndm-realm"));

        if (challengeOpt.isEmpty()) {
            throw new RuntimeException("Роутер не вернул заголовок Challenge.");
        }

        String challenge = challengeOpt.get();
        String realm = realmOpt.orElse("Keenetic");

        // --- ШАГ 2: Хэш ---
        String step1String = login + ":" + realm + ":" + password;
        String md5Hash = getHash(step1String, "MD5");
        String step2String = challenge + md5Hash;
        String sha256Hash = getHash(step2String, "SHA-256");

        // --- ШАГ 3: Авторизация (Передаем куку строкой) ---
        String authJson = String.format("{\"login\":\"%s\",\"password\":\"%s\"}", login, sha256Hash);
        HttpRequest.Builder req2Builder = HttpRequest.newBuilder()
                .uri(URI.create(urlAuth))
                .header("User-Agent", FIXED_USER_AGENT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authJson));

        if (!keeneticCookie.isEmpty()) {
            req2Builder.header("Cookie", keeneticCookie);
        }

        HttpResponse<String> response2 = client.send(req2Builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response2.statusCode() != 200) {
            throw new RuntimeException("Ошибка авторизации Keenetic: " + response2.statusCode());
        }

        // Если после логина Keenetic обновил сессию — берем свежую куку
        List<String> authCookies = response2.headers().allValues("Set-Cookie");
        if (!authCookies.isEmpty()) {
            keeneticCookie = authCookies.getFirst().split(";")[0];
        }

        return new AuthDataDto(challenge, realm, keeneticCookie);

    }

    private static String getHash(String input, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
