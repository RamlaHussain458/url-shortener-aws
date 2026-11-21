package com.ramla.urlshortener.controller;

import com.ramla.urlshortener.config.AppProperties;
import com.ramla.urlshortener.dto.CreateUrlRequest;
import com.ramla.urlshortener.dto.CreateUrlResponse;
import com.ramla.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlShortenerService service;
    private final String baseUrl;

    public UrlController(UrlShortenerService service, AppProperties properties) {
        this.service = service;
        this.baseUrl = stripTrailingSlashes(properties.baseUrl());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        String originalUrl = request.url().trim();
        String code = service.create(originalUrl);
        String shortUrl = baseUrl + "/" + code;
        return ResponseEntity.created(URI.create(shortUrl))
                .body(new CreateUrlResponse(code, shortUrl, originalUrl));
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
