package de.twonirwana.infinity;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import io.javalin.Javalin;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Testcontainers
@Disabled("only for manual testing")
public class ManualPlaywrightScreenshotTest {

    static final String RESULT_FOLDER = "playwright/result/";
    static final long TEST_ID = System.currentTimeMillis();
    static final int PLAYWRIGHT_PORT = 3000;
    @Container
    static GenericContainer<?> playwrightContainer = new GenericContainer<>("mcr.microsoft.com/playwright:v1.61.0-noble")
            .withExposedPorts(PLAYWRIGHT_PORT)
            .withAccessToHost(true)
            .withCommand("/bin/bash", "-c", "npx -y playwright@1.61.0 run-server --port 3000 --host 0.0.0.0")
            .waitingFor(Wait.forLogMessage(".*Listening on.*", 1));
    static Playwright playwright;
    static Browser chromium;
    static String baseUrl;
    BrowserContext context;
    Page page;

    @BeforeAll
    public static void setupGlobal() {
        Database database = DatabaseImp.createTimedUpdate("out/html/card/image/");
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "infinity-cards-generator");
        Metrics.addRegistry(registry);
        Javalin app = WebApp.createWebApp(database,
                () -> LocalDate.of(2025, 12, 23).atStartOfDay(),
                registry);
        app.start(0);
        org.testcontainers.Testcontainers.exposeHostPorts(app.port());
        baseUrl = "http://host.testcontainers.internal:" + app.port() + "/";

        String wsEndpoint = "ws://" + playwrightContainer.getHost() + ":" + playwrightContainer.getMappedPort(PLAYWRIGHT_PORT) + "/";

        playwright = Playwright.create();
        chromium = playwright.chromium().connect(wsEndpoint);

        Path RESULT_PATH = Path.of(RESULT_FOLDER);
        try {
            if (Files.notExists(RESULT_PATH)) {
                Files.createDirectories(RESULT_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    private static Stream<Arguments> generateTestData() {
        return Stream.of(
                List.of("gS0HYXJpYWRuYQEggSwBAQEAAgCF8gABAACF8gACAA%3D%3D", "Polaris_1522"),
                List.of("gr4Nc3RlZWwtcGhhbGFueAEggSwBAQEAAwCC0QEDAACC0QEBAACC0QEHAA%3D%3D", "Scylla_721"),
                List.of("gfUGbm9tYWRzASCBLAEBAQACAIYPAAEAAIYPAAIA", "JazzBill_1551"),
                List.of("gfgIdHVuZ3Vza2EBIIEsAQEBAAMAhEMAAQAAhEMAAgAAhEMAAwA%3D", "Zondnautica_1091"),
                List.of("gZEJaGFxcWlzbGFtASCBLAEBAQADAIF8AAEAAIF8AAIAAIF8AAMA", "Sacreface_380"),
                List.of("gr8Kb3BlcmF0aW9ucwpKQSBCZXN0IG9mgSwBAQEAAgCCVQEBAACCVQIBAA%3D%3D", "Posthumans_703"),
                List.of("gS0HYXJpYWRuYQEggSwBAQEAAgCA6QEHAACEZwGQLAA%3D", "manyWeapons"),
                List.of("glsKc2hhc3Zhc3RpaQEggSwBAQEABACFEwEBAACC5QEBAACB9gEIAACB9gEIAA%3D%3D", "allProgamms"),
                List.of("gloFbW9yYXQBIIEsAQEBAAIAh1IBAQAAgvQBAgA%3D", "bootyAndMetaChemistry"),
                List.of("glkNY29tYmluZWQtYXJteQEggSwCAQEACgCDxQECAACGJAECAACF6QEDAACFEwEBAACDDwEDAACB8QEHAACB7wEGAACG5AEEAACCCwEEAACB8gEJAAIBAAEAguUBAQA%3D", "allCAHacker"),
                List.of("ZQpwYW5vY2VhbmlhASCBLAIBAQABAIcMAQEAAgEAAQCHDAEBAA%3D%3D", "panO"),
                List.of("gMkHeXUtamluZwEggSwCAQEAAQCHMQEBAAIBAAEAhy4BAgA%3D", "yuJing"),
                List.of("gS0HYXJpYWRuYQEggSwCAQEAAQCA%2FwEBAAIBAAEAgQYBAwA%3D", "ariadne"),
                List.of("gZEJaGFxcWlzbGFtASCBLAIBAQABAIFDAQYAAgEAAQCHZgECAA%3D%3D", "haq"),
                List.of("gfUGbm9tYWRzASCBLAIBAQABAIGWAQEAAgEAAQCG4gEBAA%3D%3D", "normads"),
                List.of("glkNY29tYmluZWQtYXJteQEggSwCAQEAAQCCFQEBAAIBAAEAgewBBAA%3D", "CA"),
                List.of("gr0FYWxlcGgBIIEsAgEBAAEAhJYBAQACAQABAIbrAQQA", "aleph"),
                List.of("gyEFdG9oYWEBIIEsAgEBAAEAgo8BAgACAQABAILPAQEA", "tohaa"),
                List.of("g4YFZHJ1emUBIIEsAgEBAAEAhh0BAQACAQABAIYCAQIA", "na2"),
                List.of("g%2BkEby0xMgEggSwCAQEAAQCFqQEBAAIBAAEAhe0BAQA%3D", "o12"),
                List.of("hE0DanNhASCBLAIBAQABAICVAYIRAAIBAAEAgJMBAQA%3D", "jsa")
        ).flatMap(a -> Stream.of(
                Arguments.from(addToList(HtmlPrinter.Template.a4_image.name(), a)),
                Arguments.from(addToList(HtmlPrinter.Template.a4_overview.name(), a))
        ));
    }

    private static List<String> addToList(String value, List<String> list) {
        return Stream.concat(Stream.of(value), list.stream())
                .toList();
    }

    @ParameterizedTest
    @MethodSource("generateTestData")
    void testBrowserAndTemplate(HtmlPrinter.Template template, String armyCode, String name) throws IOException {
        context = chromium.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));
        page = context.newPage();
        page.navigate(baseUrl);
        page.waitForLoadState();
        assertThat(page.locator("body")).isVisible();
        page.getByLabel("Select Card Style:").selectOption(template.name());
        page.getByLabel("Army Code or Option IDs:").fill(armyCode);


        Page newPage = page.waitForPopup(() -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Generate and View Cards")).click());

        newPage.waitForLoadState();
        byte[] actualImageBytes = newPage.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));

        String fileName = chromium.browserType().name() + "_" + template.name() + "_" + name;
        File expectedFile = new File("playwright/expected/" + fileName + "_expected.png");
        BufferedImage actual = ImageIO.read(new ByteArrayInputStream(actualImageBytes));
        if (!expectedFile.exists()) {
            ImageIO.write(actual, "png", new File(RESULT_FOLDER + fileName + "_expected.png"));
            Assertions.fail();
        }

        BufferedImage expected = ImageIO.read(expectedFile);
        ImageComparisonResult result = new ImageComparison(expected, actual)
                .setPixelToleranceLevel(0.1)
                .setDifferenceRectangleColor(Color.BLUE)
                .compareImages();


        if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
            ImageIO.write(result.getResult(), "png", new File(RESULT_FOLDER + fileName + "_diff_" + TEST_ID + ".png"));
            ImageIO.write(actual, "png", new File(RESULT_FOLDER + fileName + "_expected" + ".png"));
        }

        Assertions.assertThat(result.getImageComparisonState()).isEqualTo(ImageComparisonState.MATCH);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

}