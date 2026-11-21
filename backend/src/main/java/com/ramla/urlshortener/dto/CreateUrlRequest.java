package com.ramla.urlshortener.dto;

import com.ramla.urlshortener.validation.SafeHttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        @SafeHttpUrl
        String url) {
}
