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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PlaywrightScreenshotTest {

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

        int port = app.port();
        baseUrl = "http://localhost:" + port;
        System.out.println("Test server running at: " + baseUrl);

        playwright = Playwright.create();
        chromium = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        firefox = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
        webkit = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(true));
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
            ImageIO.write(actual, "png", new File("playwright/" + fileName + "_expected.png"));
            Assertions.fail();
        }

        BufferedImage expected = ImageIO.read(expectedFile);
        ImageComparisonResult result = new ImageComparison(expected, actual)
                .setPixelToleranceLevel(0.1)
                .compareImages();


        if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
            ImageIO.write(result.getResult(), "png", new File("playwright/" + fileName + "_diff_" + System.currentTimeMillis() + ".png"));
            ImageIO.write(actual, "png", new File("playwright/" + fileName + "_actual_" + System.currentTimeMillis() + ".png"));
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