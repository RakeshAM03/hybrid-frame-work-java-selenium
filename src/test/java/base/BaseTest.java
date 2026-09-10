package base;

import core.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import utils.TestLogger;

import java.util.logging.Logger;

/**
 * Common TestNG lifecycle for all test classes. The WebDriver session is created ONCE per test
 * class (@BeforeClass) and quit ONCE after every test method in that class has finished
 * (@AfterClass) - deliberately NOT per test method - so a whole scenario suite (e.g. all 7 login
 * scenarios in LoginTest) shares a single browser session and a single page load instead of
 * reloading the URL for every scenario.
 *
 * TestNG guarantees superclass @BeforeClass methods run before subclass @BeforeClass methods
 * (and the reverse for @AfterClass), so a subclass such as LoginTest can safely add its own
 * @BeforeClass method that navigates once, using the `driver` field set up here.
 */
public abstract class BaseTest {

    protected static final Logger LOGGER = TestLogger.getLogger(BaseTest.class);

    protected WebDriver driver;

    @BeforeClass
    public void setUpDriver() {
        DriverManager.initDriver();
        driver = DriverManager.getDriver();
        LOGGER.info("WebDriver session started ONCE for this test class (thread " + Thread.currentThread().getId() + ")");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownDriver() {
        LOGGER.info("All scenarios in this class are complete - quitting the single shared WebDriver session");
        DriverManager.quitDriver();
    }
}
