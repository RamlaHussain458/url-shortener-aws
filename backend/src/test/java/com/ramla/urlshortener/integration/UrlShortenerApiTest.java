package com.ramla.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class UrlShortenerApiTest extends AbstractMySqlIntegrationTest {

    private static final String TARGET = "https://example.com/some/long/path?q=1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsShortUrlAndRedirectsToOriginal() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + TARGET + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value(TARGET))
                .andExpect(jsonPath("$.shortCode").isString())
                .andReturn();

        String code = JsonPath.read(created.getResponse().getContentAsString(), "$.shortCode");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", TARGET));

        // Second call is served through the cache path and must behave identically.
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", TARGET));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "ftp://example.com/file",
            "http://localhost:8080/admin",
            "http://192.168.1.10/router",
            "https://user:pass@example.com",
            "not a url"
    })
    void rejectsUnsafeOrInvalidUrls(String url) throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }

    @Test
    void rejectsMissingUrl() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownCode() throws Exception {
        mockMvc.perform(get("/Zz9Zz9Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Short link not found"));
    }

    @Test
    void sendsSecurityHeaders() throws Exception {
        mockMvc.perform(get("/Zz9Zz9Z"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void healthEndpointIsPublicButOtherActuatorEndpointsAreNot() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
    }

    @Test
    void unknownApiPathsAreDenied() throws Exception {
        mockMvc.perform(get("/api/v1/urls")).andExpect(status().isForbidden());
    }
}
