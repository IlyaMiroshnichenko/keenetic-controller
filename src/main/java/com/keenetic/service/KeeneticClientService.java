package com.keenetic.service;

import com.keenetic.dto.AuthDataDto;
import com.keenetic.dto.RouterInterfaceDto;
import com.keenetic.dto.UsbLteInterfaceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class KeeneticClientService {

    private static final String FIXED_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KeeneticClient/1.0";

    private final KeeneticAuthorizationService authorizationService;

    public KeeneticClientService(KeeneticAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Value("${keenetic.ip}")
    private String routerIp;

    public UsbLteInterfaceDto getMobileSignalInfo() throws Exception {
        String urlRci = "http://" + routerIp + "/rci/show/interface";

        HttpClient client = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        AuthDataDto authData = authorizationService.getAuthData();
        String paddedJson = "{\n  \n}";
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(urlRci))
                .header("User-Agent", FIXED_USER_AGENT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-NDM-Challenge", authData.challengeHeader())
                .header("X-NDM-Realm", authData.realmHeader());

        if (!authData.cookie().isEmpty()) {
            request.header("Cookie", authData.cookie());
        }

        log.info("Sending request...");
        request.POST(HttpRequest.BodyPublishers.ofString(paddedJson, java.nio.charset.StandardCharsets.UTF_8));
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        log.info("Response received.");
        ObjectMapper mapper = new ObjectMapper();
        RouterInterfaceDto routerInterface = mapper.readValue(response.body(), RouterInterfaceDto.class);

        return routerInterface.interfaceItems().usbLteInterface();
    }
}
