package com.goit;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final String apiUrl; //  фактический URL API
    private final int requestLimit;
    private final Semaphore semaphore;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CrptApi(String apiUrl, Duration timeUnit, int requestLimit) {
        this.apiUrl = apiUrl;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        scheduleSemaphoreReset(timeUnit);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        try {
            String documentJson = objectMapper.writeValueAsString(document);
            String requestBody = "{ \"document\": " + documentJson + ", \"signature\": \"" + signature + "\" }";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to create document. HTTP Status Code: " + response.statusCode());
            }
        } finally {
            semaphore.release();
        }
    }

    private void scheduleSemaphoreReset(Duration timeUnit) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(semaphore::release, 0, timeUnit.toSeconds(), TimeUnit.SECONDS);
    }


    // Внутренний класс для представления документа (замените на фактический формат)
    private static class Document {
        // Поля вашего документа
    }
}

