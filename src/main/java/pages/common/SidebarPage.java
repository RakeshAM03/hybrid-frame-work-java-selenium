package pages.common;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import pages.BasePage;

/**
 * Page object for the app's persistent left sidebar/nav chrome - not specific to Branding Hub,
 * which is why it lives outside the {@code pages.branding} package. Covers the top-bar Agency
 * Name display and the Settings -> Preferences -> Branding Hub click path.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-12 against the real app (same authorized test account used
 * elsewhere in this suite):
 *  - Clicking "Settings" does NOT navigate anywhere - it expands an inline sidebar sub-panel
 *    (no URL change) that reveals "Preferences" and, under it, "Branding Hub". Only clicking
 *    "Branding Hub" itself actually navigates (to config's baseUrl).
 *  - Neither "Preferences" nor "Branding Hub" carry any id/data-testid/aria-label - both are
 *    plain, unattributed &lt;p&gt; elements, matched by their exact visible text (the same
 *    convention already used throughout pages.branding).
 *  - The Agency Name button ("Dev QA Sandbox") also has no aria-label, but its icon carries the
 *    stable, semantic lucide class "lucide-panels-top-left" - used to locate the button itself
 *    without baking the expected text into the locator (the text is read and compared separately
 *    against testdata.properties, so this page object works regardless of which agency the
 *    account under test belongs to).
 */
public class SidebarPage extends BasePage {

    private static final By AGENCY_NAME_BUTTON =
            By.xpath("//button[.//*[contains(@class,'lucide-panels-top-left')]]");

    private static final By SETTINGS_NAV_ITEM = By.cssSelector("h6.settings-text");

    private static final By PREFERENCES_LABEL = By.xpath("//p[normalize-space(text())='Preferences']");

    private static final By BRANDING_HUB_LINK = By.xpath("//p[normalize-space(text())='Branding Hub']");

    public SidebarPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public String getAgencyName() {
        return getText(AGENCY_NAME_BUTTON);
    }

    /** Expands the sidebar's Settings sub-panel in place - does not navigate anywhere. */
    public void clickSettings() {
        click(SETTINGS_NAV_ITEM);
    }

    public boolean isPreferencesVisible() {
        return isDisplayed(PREFERENCES_LABEL);
    }

    public boolean isBrandingHubLinkVisible() {
        return isDisplayed(BRANDING_HUB_LINK);
    }

    /** Navigates to the Branding Hub list (config's baseUrl) via the sidebar link, not a direct URL. */
    public void clickBrandingHub() {
        click(BRANDING_HUB_LINK);
    }
}
