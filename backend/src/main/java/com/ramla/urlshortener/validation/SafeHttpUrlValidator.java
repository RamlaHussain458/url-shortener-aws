package com.ramla.urlshortener.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Rejects URLs that make a shortener dangerous or abusable:
 * <ul>
 *   <li>non http/https schemes (javascript:, data:, file:, ftp: ...), which enable XSS and local file tricks</li>
 *   <li>embedded credentials (http://trusted.com@evil.com), a classic phishing trick</li>
 *   <li>localhost, single-label hosts, private/reserved IPv4 ranges and IPv6 literals, so the service
 *       is not used to point people at internal addresses</li>
 *   <li>very long URLs (database column limit)</li>
 * </ul>
 * The server never fetches the URL, so this is abuse prevention, not SSRF protection.
 * A null value is considered valid; use @NotBlank to require a value.
 */
public class SafeHttpUrlValidator implements ConstraintValidator<SafeHttpUrl, String> {

    static final int MAX_LENGTH = 2048;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > MAX_LENGTH) {
            return false;
        }

        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        if (uri.getRawUserInfo() != null) {
            return false;
        }
        String host = uri.getHost();
        return host != null && isPublicHost(host.toLowerCase(Locale.ROOT));
    }

    private static boolean isPublicHost(String rawHost) {
        String host = rawHost.endsWith(".") ? rawHost.substring(0, rawHost.length() - 1) : rawHost;

        if (host.isEmpty() || host.startsWith("[")) {
            return false; // empty host or IPv6 literal
        }
        if (!host.contains(".")) {
            return false; // localhost, intranet names, decimal IPs such as 2130706433
        }
        if (host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")
                || host.endsWith(".lan") || host.endsWith(".home.arpa")) {
            return false;
        }
        if (host.matches("[0-9.]+")) {
            return isPublicIpv4(host);
        }
        return true;
    }

    private static boolean isPublicIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false; // short forms like 127.1 are read as IPs by browsers
        }
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')) {
                return false; // empty, too long, or leading zero (octal ambiguity)
            }
            octets[i] = Integer.parseInt(part);
            if (octets[i] > 255) {
                return false;
            }
        }
        int a = octets[0];
        int b = octets[1];
        if (a == 0 || a == 10 || a == 127 || a >= 224) {
            return false;
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return false; // carrier-grade NAT
        }
        if (a == 169 && b == 254) {
            return false; // link-local, includes cloud metadata address
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return false;
        }
        return !(a == 192 && b == 168);
    }
}
