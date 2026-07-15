package com.keenetic.service;

import com.keenetic.dto.AuthDataDto;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("Request auth data...");
        // Создаем чистый клиент БЕЗ CookieManager, чтобы избежать магии потоков Spring Boot
        HttpClient client = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // --- ШАГ 1: Challenge ---
        HttpRequest requestChallenge = HttpRequest.newBuilder()
                .uri(URI.create(urlAuth))
                .header("User-Agent", FIXED_USER_AGENT)
                .GET()
                .build();
        log.info("Request auth challenge.");
        HttpResponse<String> responseChallenge = client.send(requestChallenge, HttpResponse.BodyHandlers.ofString());
        log.info("Received auth challenge.");
        // Извлекаем сырую куку сессии, которую выдал роутер
        List<String> setCookies = responseChallenge.headers().allValues("Set-Cookie");
        String keeneticCookie = setCookies.isEmpty() ? "" : setCookies.getFirst().split(";")[0];

        Optional<String> challengeOpt = responseChallenge.headers().firstValue("X-NDM-Challenge")
                .or(() -> responseChallenge.headers().firstValue("x-ndm-challenge"));
        Optional<String> realmOpt = responseChallenge.headers().firstValue("X-NDM-Realm")
                .or(() -> responseChallenge.headers().firstValue("x-ndm-realm"));

        if (challengeOpt.isEmpty()) {
            log.error("Router did not return challenge header.");
            throw new RuntimeException("Роутер не вернул заголовок Challenge.");
        }

        String challenge = challengeOpt.get();
        String realm = realmOpt.orElse("Keenetic");

        // --- ШАГ 2: Хэш ---
        log.info("Calculating hash...");
        String step1String = login + ":" + realm + ":" + password;
        String md5Hash = getHash(step1String, "MD5");
        String step2String = challenge + md5Hash;
        String sha256Hash = getHash(step2String, "SHA-256");
        log.info("Successful hash calculation.");

        // --- ШАГ 3: Авторизация (Передаем куку строкой) ---
        String authJson = String.format("{\"login\":\"%s\",\"password\":\"%s\"}", login, sha256Hash);
        HttpRequest.Builder requestAuth = HttpRequest.newBuilder()
                .uri(URI.create(urlAuth))
                .header("User-Agent", FIXED_USER_AGENT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authJson));

        if (!keeneticCookie.isEmpty()) {
            requestAuth.header("Cookie", keeneticCookie);
        }

        log.info("Send auth cookie...");
        HttpResponse<String> responseAuth = client.send(requestAuth.build(), HttpResponse.BodyHandlers.ofString());

        if (responseAuth.statusCode() != 200) {
            log.error("Authorization failed: {}, body: {}", responseAuth.statusCode(), responseAuth.body());
            throw new RuntimeException("Authorization failed: " + responseAuth.statusCode());
        }

        log.info("Auth cookie received.");
        // Если после логина Keenetic обновил сессию — берем свежую куку
        List<String> authCookies = responseAuth.headers().allValues("Set-Cookie");
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
