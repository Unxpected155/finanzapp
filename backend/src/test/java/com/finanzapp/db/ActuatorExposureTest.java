package com.finanzapp.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;

/** Actuator expone solo health, sin detalles; todo lo demás está cerrado. */
class ActuatorExposureTest extends PostgresTestBase {

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void healthRespondeUpSinDetalles() throws Exception {
        HttpResponse<String> res = get("/actuator/health");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("\"status\":\"UP\"").doesNotContain("components", "db");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/beans", "/actuator/heapdump", "/api/cualquier-cosa"})
    void loDemasNoResponde200(String path) throws Exception {
        assertThat(get(path).statusCode()).isIn(401, 403, 404);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
            HttpResponse.BodyHandlers.ofString());
    }
}
