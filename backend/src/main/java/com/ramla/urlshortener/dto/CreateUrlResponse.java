package com.ramla.urlshortener.dto;

public record CreateUrlResponse(String shortCode, String shortUrl, String originalUrl) {
}
