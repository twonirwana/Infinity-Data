package de.twonirwana.infinity;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import io.javalin.Javalin;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Testcontainers
public class PlaywrightScreenshotTest {

    @Container
    static GenericContainer<?> playwrightContainer = new GenericContainer<>("mcr.microsoft.com/playwright:v1.57.0-noble")
            .withExposedPorts(3000)
            .withCommand("/bin/bash", "-c", "npx -y playwright run-server --port 3000 --host 0.0.0.0")
            .waitingFor(Wait.forLogMessage(".*Listening on.*", 1));

    static final String RESULT_FOLDER = "playwright/result/";
    static final long TEST_ID = System.currentTimeMillis();
    static Playwright playwright;
    static Browser chromium;
    static Browser firefox;
    static Browser webkit;
    static String baseUrl;
    BrowserContext context;
    Page page;
//hE4Mc2hpbmRlbmJ1dGFpASCBLAIBAQAKAISIAQEAAIcaAQIAAIU1AQQAAIdSAQEAAIcZAQQAAICeAQEAAIcdAQUAAICnAQMAAIcbAQMAAIccAQMAAgEABQCHHwECAACG%2FAEBAACGIQEFAACAoQEBAACA4AGC5QA%3D

    @BeforeAll
    public static void setupGlobal() {
        Database database = DatabaseImp.createWithoutUpdate("playwright/resources");
        Javalin app = WebApp.createWebApp(database, () -> LocalDate.of(2025, 12, 23).atStartOfDay());
        app.start(0);

        String wsEndpoint = "ws://" + playwrightContainer.getHost() + ":" + playwrightContainer.getMappedPort(3000) + "/";

        playwright = Playwright.create();
        chromium = playwright.chromium().connect(wsEndpoint);
        firefox = playwright.firefox().connect(wsEndpoint);
        webkit = playwright.webkit().connect(wsEndpoint);

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
        return Arrays.stream(BrowserTyp.values())
                .flatMap(b -> Arrays.stream(HtmlPrinter.Template.values())
                        .map(t -> Arguments.of(b, t)));
    }

    private Browser fromType(BrowserTyp browserTyp) {
        if (browserTyp == BrowserTyp.FIREFOX) {
            return firefox;
        } else if (browserTyp == BrowserTyp.CHROMIUM) {
            return chromium;
        } else if (browserTyp == BrowserTyp.WEBKIT) {
            return webkit;
        }
        throw new IllegalArgumentException("Unknown browser type: " + browserTyp);
    }

    @ParameterizedTest
    @MethodSource("generateTestData")
    void testCssRefactoringMatchesBaseline(BrowserTyp browserType, HtmlPrinter.Template template) throws IOException {
        Browser browser = fromType(browserType);
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));
        page = context.newPage();
        page.navigate(baseUrl + "/");
        page.waitForLoadState();
        assertThat(page.locator("body")).isVisible();
        page.getByLabel("Select Card Style:").selectOption(template.name());
        page.getByLabel("Your army code:").fill("hE4Mc2hpbmRlbmJ1dGFpASCBkAIBAQAKAISIAQEAAIcaAQIAAIU1AQQAAIdSAQEAAIcZAQQAAICeAQEAAIcdAQUAAICnAQMAAIcbAQMAAIccAQMAAgEABwCHHwECAACG%2FAEBAACGIQEFAACAoQEBAACA4AGC5QAAgKIBAgAAhyMBAgA%3D");


        Page newPage = page.waitForPopup(() -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Generate and View Cards")).click());

        newPage.waitForLoadState();
        byte[] actualImageBytes = newPage.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));

        String fileName = browser.browserType().name() + "_" + template.name();
        File expectedFile = new File("playwright/expected/" + fileName + "_expected.png");
        BufferedImage actual = ImageIO.read(new ByteArrayInputStream(actualImageBytes));
        if (!expectedFile.exists()) {
            ImageIO.write(actual, "png", new File(RESULT_FOLDER + fileName + "_expected.png"));
            Assertions.fail();
        }

        BufferedImage expected = ImageIO.read(expectedFile);
        ImageComparisonResult result = new ImageComparison(expected, actual)
                .setPixelToleranceLevel(0.1)
                .compareImages();


        if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
            ImageIO.write(result.getResult(), "png", new File(RESULT_FOLDER + fileName + "_diff_" + TEST_ID + ".png"));
            ImageIO.write(actual, "png", new File(RESULT_FOLDER + fileName + "_actual_" + TEST_ID + ".png"));
        }

        Assertions.assertThat(result.getImageComparisonState()).isEqualTo(ImageComparisonState.MATCH);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    private enum BrowserTyp {
        CHROMIUM,
        FIREFOX,
        WEBKIT
    }
}