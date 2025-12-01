package com.example.dashboard;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

@QuarkusTest
public class DashboardResourceTest {

    private String readPayload() throws IOException {
        // Read payload from classpath to avoid path issues in test profile
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-payload.json");
        if (is == null) throw new IOException("test-payload.json not found on classpath");
        return new String(is.readAllBytes());
    }

    @Test
    public void testSvgEndpoint() throws IOException {
        String payload = readPayload();
        String svg =
        given()
            .contentType("application/json")
            .body(payload)
        .when()
            .post("/api/v1/dashboard/svg")
        .then()
            .statusCode(200)
            .contentType(containsString("image/svg+xml"))
            // Basic checks: has SVG root and our titles
            .body(containsString("<svg"))
            .body(containsString("Application Security"))
            .body(containsString("Application Security Governance"))
            .body(containsString("Application Security Capabilities"))
            // Blue title bars (full-width rectangles for title + sections)
            .body(containsString("<rect x=\"10\" y=\"10\" width=\"1380\" height=\"35\" rx=\"4\" fill=\"#1E3A8A\""))
            .body(containsString("<rect x=\"10\" y=\"50\" width=\"1380\" height=\"25\" rx=\"3\" fill=\"#1E3A8A\""))
            // At least one domain header and a component box
            .body(containsString("Application Security Testing"))
            .body(containsString("<rect x=\"0\" y=\"0\" width=\""))
            .extract()
            .asString();

        org.junit.jupiter.api.Assertions.assertTrue(
            countOccurrences(svg, "width=\"1380\" height=\"25\" rx=\"3\" fill=\"#1E3A8A\"") >= 2,
            "Should render both governance and capabilities title bars");
    }

    @Test
    public void testPngEndpoint() throws IOException {
        String payload = readPayload();
        byte[] bytes =
            given()
                .contentType("application/json")
                .body(payload)
            .when()
                .post("/api/v1/dashboard/png")
            .then()
                .statusCode(200)
                .contentType(containsString("image/png"))
                .extract()
                .asByteArray();
        // Ensure non-empty PNG
        org.junit.jupiter.api.Assertions.assertTrue(bytes.length > 1000, "PNG output should be non-empty");
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
