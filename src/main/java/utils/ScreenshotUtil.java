package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * On-demand screenshot capture for specific test steps (e.g. "capture the Typography page after
 * making a selection"). Distinct from {@link listeners.ScreenshotListener}, which only fires
 * automatically when a test fails - this one is called explicitly from test code at a chosen
 * moment. Screenshots land in the same test-output/screenshots directory so every capture, of
 * either kind, is easy to find in one place.
 */
public final class ScreenshotUtil {

    private static final Logger LOGGER = Logger.getLogger(ScreenshotUtil.class.getName());
    private static final String SCREENSHOT_DIRECTORY = "test-output/screenshots";

    private ScreenshotUtil() {
    }

    /**
     * Saves a screenshot named exactly "{fileNameWithoutExtension}.png" - no timestamp - so a
     * caller asking for a specific named file (e.g. "typography-page") gets that exact file every
     * run, overwriting any previous capture of the same name.
     */
    public static void capture(WebDriver driver, String fileNameWithoutExtension) {
        try {
            Path directory = Paths.get(SCREENSHOT_DIRECTORY);
            Files.createDirectories(directory);

            Path destination = directory.resolve(fileNameWithoutExtension + ".png");
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Screenshot saved: " + destination.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to capture screenshot '" + fileNameWithoutExtension + "'", e);
        }
    }
}
