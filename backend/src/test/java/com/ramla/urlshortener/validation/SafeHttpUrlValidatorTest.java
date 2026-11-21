package com.ramla.urlshortener.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SafeHttpUrlValidatorTest {

    private final SafeHttpUrlValidator validator = new SafeHttpUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com/path?x=1#section",
            "https://sub.example.co.uk:8443/a/b",
            "HTTPS://EXAMPLE.COM",
            "https://8.8.8.8/dns"
    })
    void acceptsPublicHttpUrls(String url) {
        assertThat(validator.isValid(url, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html,hello",
            "file:///etc/passwd",
            "ftp://example.com/file",
            "//example.com",
            "example.com",
            "http://localhost",
            "http://localhost.",
            "http://intranet/admin",
            "http://127.0.0.1",
            "http://127.1",
            "http://2130706433",
            "http://010.0.0.1",
            "http://10.0.0.5",
            "http://172.16.0.1",
            "http://172.31.255.1",
            "http://192.168.1.1",
            "http://169.254.169.254/latest/meta-data",
            "http://100.64.0.1",
            "http://[::1]/",
            "https://user:pass@example.com",
            "http://exa mple.com",
            "http://printer.local"
    })
    void rejectsUnsafeUrls(String url) {
        assertThat(validator.isValid(url, null)).isFalse();
    }

    @Test
    void rejectsBlankAndOverlongUrls() {
        assertThat(validator.isValid("   ", null)).isFalse();
        assertThat(validator.isValid("https://example.com/" + "a".repeat(2100), null)).isFalse();
    }

    @Test
    void nullIsLeftToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
