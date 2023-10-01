package com.itm.space.backendresources;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import net.minidev.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BackendResourcesTest {

    @Container
    static private KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("keycloak/export.json");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @DynamicPropertySource
    static void registerResourceServerIssuerProperty(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloak.getAuthServerUrl() + "realms/ITM");
//        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> keycloak.getAuthServerUrl() + "/realms/ITM/protocol/openid-connect/certs");
    }

    private String getToken() {
        try (Keycloak keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(keycloak.getAuthServerUrl())
                .grantType("password")
                .realm("ITM")
                .clientId("backend-gateway-client")
                .username("user")
                .password("user")
                .clientSecret("boVu3GXpc6nFfLHb3aQTOnTMV21h8rai")
                .build()) {
            String access_token = keycloakAdminClient.tokenManager().getAccessToken().getToken();
            return "Bearer " + access_token;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    @DisplayName("авторизованный пользователь")
    public void test1(){
        String token = getToken();
        RestAssured.registerParser("text/plain", Parser.JSON);
        given()
                .header("Authorization",token)
                .when()
                .get("api/users/hello")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(equalTo("0d19c49b-0e56-424a-80fa-aa5e968d7e78"));
        }

    @Test
    @DisplayName("получение пользователя")
    public void test2(){
        String userid="0d19c49b-0e56-424a-80fa-aa5e968d7e78";
        String token = getToken();
        given()
                .header("Authorization",token)
                .when()
                .get("/api/users/"+userid)
                .prettyPeek()
                .then()
                .statusCode(200)
                .body("firstName",equalTo("user"))
                .body("lastName",equalTo("user"))
                .body("email",equalTo("user@user.ru"));
        }


    @Test
    @DisplayName("регистрация пользователя")
    public void test3(){
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("username", "user2");
        jsonObj.put("password", "user2");
        jsonObj.put("email","user2@user.ru");
        jsonObj.put("firstName", "user2");
        jsonObj.put("lastName", "user2");

        String token = getToken();
        Response response = RestAssured.given()
                .header("Authorization",token)
                .contentType("application/json")
                .body(jsonObj.toString())
                .when()
                .post("/api/users")
                .prettyPeek();
        Assertions.assertEquals(response.statusCode(), 200);

    }
}