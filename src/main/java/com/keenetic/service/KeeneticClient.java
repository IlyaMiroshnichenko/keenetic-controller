package com.keenetic.service;

import com.keenetic.dto.AuthDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class KeeneticClient {

    private static final String FIXED_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KeeneticClient/1.0";

    private final KeeneticAuthorizationService authorizationService;

    public KeeneticClient(KeeneticAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Value("${keenetic.ip}")
    private String routerIp;

    public String getHostsCommand() throws Exception {
        String urlRci = "http://" + routerIp + "/rci/show/interface";

        HttpClient client = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        AuthDataDto authData = authorizationService.getAuthData();
        String paddedJson = "{\n  \n}";
        HttpRequest.Builder req3Builder = HttpRequest.newBuilder()
                .uri(URI.create(urlRci))
                .header("User-Agent", FIXED_USER_AGENT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-NDM-Challenge", authData.challengeHeader())
                .header("X-NDM-Realm", authData.realmHeader());

        if (!authData.cookie().isEmpty()) {
            req3Builder.header("Cookie", authData.cookie());
        }

        req3Builder.POST(HttpRequest.BodyPublishers.ofString(paddedJson, java.nio.charset.StandardCharsets.UTF_8));
        HttpResponse<String> response3 = client.send(req3Builder.build(), HttpResponse.BodyHandlers.ofString());

        return response3.body();
    }
}
