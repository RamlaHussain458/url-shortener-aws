package com.ramla.urlshortener.controller;

import com.ramla.urlshortener.service.UrlShortenerService;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    /**
     * The regex means only well-formed codes reach the service, so junk paths are rejected early
     * and user input can never contain odd characters. 302 (not 301) is used so browsers do not
     * cache the redirect forever, which keeps later features (click counting, expiry) possible.
     */
    @GetMapping("/{code:[0-9A-Za-z]{4,16}}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        String target = service.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
