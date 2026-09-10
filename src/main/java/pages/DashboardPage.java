package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * Page object for the authenticated app shell that appears after a successful login.
 *
 * Note: this application does not deep-link back to the originally requested protected page -
 * a successful login always redirects to /jobs/jobs-overview regardless of which protected URL
 * (e.g. baseUrl) triggered the login redirect (verified live). So rather than asserting on a
 * page-specific element or on baseUrl, success is confirmed via the "Help & Support" header
 * button, which is a real, verified element present on every authenticated platform.joveo.ai
 * page - making the check correct no matter which protected page baseUrl points at.
 */
public class DashboardPage extends BasePage {

    private static final By AUTHENTICATED_SHELL_MARKER =
            By.xpath("//button[@aria-label='Help & Support — opens Joveo Help Center in a new tab']");

    public DashboardPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isDashboardLoaded() {
        return isDisplayed(AUTHENTICATED_SHELL_MARKER);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
