package de.twonirwana.infinity;

import de.twonirwana.infinity.fireteam.FireteamChart;
import de.twonirwana.infinity.unit.api.UnitOption;
import io.javalin.Javalin;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebAppSecurityTest {

    private static Javalin app;
    private static String baseUrl;
    private static HttpClient httpClient;

    @BeforeAll
    static void startServer() {
        Database stubDatabase = new Database() {
            @Override
            public List<UnitOption> getAllUnitOptions(Language language) {
                return List.of();
            }

            @Override
            public ArmyList getArmyListForArmyCode(String armyCode, Language language) {
                throw new IllegalArgumentException("Simulated decode failure for " + armyCode);
            }

            @Override
            public List<Sectorial> getAllSectorials(Language language) {
                return List.of();
            }

            @Override
            public void updateData(String imageOutputFolder) {
            }

            @Override
            public boolean canDecodeArmyCode(String armyCode) {
                return true;
            }

            @Override
            public List<ValidationError> validateArmyCodeUnits(String armyCode, Language language) {
                return List.of();
            }

            @Override
            public List<HackingProgram> getAllHackingPrograms(Language language) {
                return List.of();
            }

            @Override
            public List<MartialArtLevel> getAllMartialArtLevels(Language language) {
                return List.of();
            }

            @Override
            public List<BootyRoll> getAllBootyRolls(Language language) {
                return List.of();
            }

            @Override
            public List<MetaChemistryRoll> getAllMetaChemistryRolls(Language language) {
                return List.of();
            }

            @Override
            public FireteamChart getFireteamChart(Sectorial sectorial, Language language) {
                return null;
            }
        };

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        app = WebApp.createWebApp(stubDatabase, () -> LocalDate.of(2025, 12, 23).atStartOfDay(), registry);
        app.start("127.0.0.1", 0);
        baseUrl = "http://127.0.0.1:" + app.port();
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @AfterAll
    static void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void invalidStyleDoesNotReflectHtml() throws Exception {
        String payload = "<script>alert(1)</script>";
        String encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8);
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/generate?armyData=test&style=" + encoded)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/plain");
        assertThat(response.body()).doesNotContain("<script>");
    }

    @Test
    void armyCodeErrorDoesNotReflectHtml() throws Exception {
        String payload = "<img src=x onerror=alert(1)>";
        String encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8);
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/generate?armyData=" + encoded + "&style=a4_image")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/plain");
        assertThat(response.body()).doesNotContain("<img");
    }

    @Test
    void missingArmyCodeReturnsPlainText() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/generate?style=a4_image")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/plain");
    }

    @Test
    void viewCardRejectsPathTraversalAndServesValidCard() throws Exception {
        Path secretOutside = Path.of("out/html/secret_outside_card.html");
        Path validCard = Path.of("out/html/card/valid_test_card-123.html");
        Files.createDirectories(validCard.getParent());
        Files.writeString(secretOutside, "<html><body>SECRET</body></html>", StandardCharsets.UTF_8);
        Files.writeString(validCard, "<html><body>VALID_CARD</body></html>", StandardCharsets.UTF_8);

        try {
            // Attempt traversal with encoded backslash and dot-dot sequences
            for (String maliciousKey : List.of("..%5Csecret_outside_card", "foo.bar", "..")) {
                HttpResponse<String> response = httpClient.send(
                        HttpRequest.newBuilder(URI.create(baseUrl + "/view/" + maliciousKey)).GET().build(),
                        HttpResponse.BodyHandlers.ofString()
                );
                assertThat(response.statusCode()).isEqualTo(400);
                assertThat(response.body()).doesNotContain("SECRET");
            }

            HttpResponse<String> missingResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/view/does_not_exist_999")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(missingResponse.statusCode()).isEqualTo(404);

            HttpResponse<String> validResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/view/valid_test_card-123")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(validResponse.statusCode()).isEqualTo(200);
            assertThat(validResponse.body()).contains("VALID_CARD");
        } finally {
            Files.deleteIfExists(secretOutside);
            Files.deleteIfExists(validCard);
        }
    }

    @Test
    void securityHeadersArePresentOnPagesAndSandboxedOnStaticImages() throws Exception {
        HttpResponse<String> indexResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(indexResponse.statusCode()).isEqualTo(200);
        assertThat(indexResponse.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(indexResponse.headers().firstValue("X-Frame-Options")).contains("DENY");
        assertThat(indexResponse.headers().firstValue("Referrer-Policy")).contains("no-referrer");
        assertThat(indexResponse.headers().firstValue("Content-Security-Policy").orElse(""))
                .contains("script-src 'none'")
                .contains("frame-ancestors 'none'");

        Path testSvg = Path.of("out/html/card/image/security-test-logo.svg");
        Files.createDirectories(testSvg.getParent());
        Files.writeString(testSvg, "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>", StandardCharsets.UTF_8);
        try {
            HttpResponse<String> svgResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/view/image/security-test-logo.svg")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(svgResponse.statusCode()).isEqualTo(200);
            assertThat(svgResponse.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
            assertThat(svgResponse.headers().firstValue("Content-Security-Policy").orElse(""))
                    .contains("default-src 'none'")
                    .contains("sandbox");
        } finally {
            Files.deleteIfExists(testSvg);
        }
    }

    @Test
    void csvDownloadSetsCsvContentType() throws Exception {
        Path csvDir = Path.of(Database.CSV_LIST_PATH.formatted("en"));
        Path diffDir = Path.of(Database.CSV_DIFF_LIST_PATH.formatted("en"));
        Files.createDirectories(csvDir);
        Files.createDirectories(diffDir);
        Path testCsv = csvDir.resolve("security-test.csv");
        Files.writeString(testCsv, "col1;col2\nval1;val2\n", StandardCharsets.UTF_8);

        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/csv/security-test.csv?lang=en")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/csv");
            assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        } finally {
            Files.deleteIfExists(testCsv);
        }
    }
}
