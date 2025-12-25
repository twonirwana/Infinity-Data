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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PlaywrightScreenshotTest {

    static Playwright playwright;
    static Browser browser;
    static String baseUrl;
    BrowserContext context;
    Page page;
//hE4Mc2hpbmRlbmJ1dGFpASCBLAIBAQAKAISIAQEAAIcaAQIAAIU1AQQAAIdSAQEAAIcZAQQAAICeAQEAAIcdAQUAAICnAQMAAIcbAQMAAIccAQMAAgEABQCHHwECAACG%2FAEBAACGIQEFAACAoQEBAACA4AGC5QA%3D

    @BeforeAll
    public static void setupGlobal() {
        Database database = new DatabaseImp("playwright/resources");
        Javalin app = WebApp.createWebApp(database, () -> LocalDate.of(2025, 12, 23).atStartOfDay());
        app.start(0);

        int port = app.port();
        baseUrl = "http://localhost:" + port;
        System.out.println("Test server running at: " + baseUrl);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));
        page = context.newPage();
    }

    @ParameterizedTest
    @CsvSource({"a4_image", "c6onA4_image", "a4_overview", "a7_image", "letter_image", "card_bw"})
    void testCssRefactoringMatchesBaseline(String style) throws IOException {
        page.navigate(baseUrl + "/");
        page.waitForLoadState();
        assertThat(page.locator("body")).isVisible();
        page.getByLabel("Select Card Style:").selectOption(style);
        page.getByLabel("Your army code:").fill("hE4Mc2hpbmRlbmJ1dGFpASCBkAIBAQAKAISIAQEAAIcaAQIAAIU1AQQAAIdSAQEAAIcZAQQAAICeAQEAAIcdAQUAAICnAQMAAIcbAQMAAIccAQMAAgEABwCHHwECAACG%2FAEBAACGIQEFAACAoQEBAACA4AGC5QAAgKIBAgAAhyMBAgA%3D");


        Page newPage = page.waitForPopup(() -> {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Generate and View Cards")).click();
        });

        newPage.waitForLoadState();
        byte[] actualImageBytes = newPage.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));


        BufferedImage actual = ImageIO.read(new ByteArrayInputStream(actualImageBytes));
        BufferedImage expected = ImageIO.read(new File("playwright/expected/" + style + "_expected.png"));

        ImageComparisonResult result = new ImageComparison(expected, actual)
                .setPixelToleranceLevel(0.1)
                .compareImages();


        if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
            ImageIO.write(result.getResult(), "png", new File("playwright/" + style + "_diff_" + System.currentTimeMillis() + ".png"));
            ImageIO.write(actual, "png", new File("playwright/" + style + "_actual_" + System.currentTimeMillis() + ".png"));
        }

        Assertions.assertThat(result.getImageComparisonState()).isEqualTo(ImageComparisonState.MATCH);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }
}