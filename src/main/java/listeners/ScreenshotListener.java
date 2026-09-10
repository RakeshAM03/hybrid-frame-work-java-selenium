package listeners;

import core.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TestNG listener that automatically captures a screenshot whenever a test fails, saving it
 * under test-output/screenshots for later inspection.
 */
public class ScreenshotListener implements ITestListener {

    private static final Logger LOGGER = Logger.getLogger(ScreenshotListener.class.getName());
    private static final String SCREENSHOT_DIRECTORY = "test-output/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            LOGGER.warning("No active WebDriver session; skipping screenshot for failed test '" + result.getName() + "'");
            return;
        }

        try {
            Path directory = Paths.get(SCREENSHOT_DIRECTORY);
            Files.createDirectories(directory);

            String fileName = result.getMethod().getMethodName() + "_"
                    + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".png";
            Path destination = directory.resolve(fileName);

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), destination);

            LOGGER.info("Screenshot captured for failed test '" + result.getName() + "' at " + destination.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to capture screenshot for test '" + result.getName() + "'", e);
        }
    }
}
