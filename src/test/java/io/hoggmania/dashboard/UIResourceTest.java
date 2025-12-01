package io.hoggmania.dashboard;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class UIResourceTest {

    @Test
    void uiFormLoads() {
        given()
          .when().get("/ui")
          .then()
          .statusCode(200)
          .body(containsString("ESA Dashboard"))
          .body(containsString("Paste Sample"));
    }

    @Test
    void sampleEndpointReturnsJson() {
        given()
          .when().get("/ui/sample")
          .then()
          .statusCode(200)
          .header("Content-Type", containsString("application/json"))
          .body(containsString("Application Security Governance"));
    }
}
