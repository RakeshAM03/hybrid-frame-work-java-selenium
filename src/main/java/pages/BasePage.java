package pages;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Base class for all page objects. Centralizes explicit-wait handling so that no test or page
 * class ever calls WebDriverWait / ExpectedConditions directly, and retries once on
 * StaleElementReferenceException (PageFactory elements may be re-rendered, e.g. after a
 * form re-render following a failed submit).
 */
public abstract class BasePage {

    private static final int STALE_ELEMENT_RETRY_LIMIT = 2;

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Logger logger;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getExplicitWaitSeconds()));
        this.logger = Logger.getLogger(getClass().getName());
    }

    private <T> T withStaleElementRetry(Supplier<T> action) {
        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= STALE_ELEMENT_RETRY_LIMIT; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                lastException = e;
                logger.warning("Stale element reference on attempt " + attempt + ", re-locating and retrying.");
            }
        }
        throw lastException;
    }

    protected void click(WebElement element) {
        withStaleElementRetry(() -> {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
            return null;
        });
    }

    protected void click(By locator) {
        withStaleElementRetry(() -> {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
            return null;
        });
    }

    protected void type(WebElement element, String text) {
        withStaleElementRetry(() -> {
            WebElement visible = wait.until(ExpectedConditions.visibilityOf(element));
            visible.clear();
            if (text != null && !text.isEmpty()) {
                visible.sendKeys(text);
            }
            return null;
        });
    }

    protected void type(By locator, String text) {
        withStaleElementRetry(() -> {
            WebElement visible = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            visible.clear();
            if (text != null && !text.isEmpty()) {
                visible.sendKeys(text);
            }
            return null;
        });
    }

    protected String getText(WebElement element) {
        return withStaleElementRetry(() -> wait.until(ExpectedConditions.visibilityOf(element)).getText());
    }

    protected String getText(By locator) {
        return withStaleElementRetry(() -> wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText());
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return withStaleElementRetry(() -> wait.until(ExpectedConditions.visibilityOf(element)).isDisplayed());
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isDisplayed(By locator) {
        try {
            return withStaleElementRetry(() -> wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed());
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected boolean waitForUrlContains(String fragment) {
        try {
            return wait.until(ExpectedConditions.urlContains(fragment));
        } catch (Exception e) {
            return false;
        }
    }
}
