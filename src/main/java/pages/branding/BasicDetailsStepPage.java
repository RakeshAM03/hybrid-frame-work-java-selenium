package pages.branding;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

/**
 * Page object for Step 1 of 4 (Basic Details) of the Brand creation wizard: the brand name field
 * and its validation error, and the Primary Logo upload control.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11. Two real findings that differ from the original guess:
 *  - The brand name input's id is a React-generated, unstable value (e.g. "_r_mu_", changes across
 *    reloads) - unlike LoginPage's email field, this app does NOT give this input a stable id, so
 *    the locator anchors on its placeholder text instead.
 *  - There are 4 file inputs on this step (Primary Logo, Secondary Logo, Website Favicon, Submark
 *    Logo), not 1 - a bare "input[type='file']" locator would silently grab whichever the browser
 *    returns first, which happened to be correct by coincidence during manual testing but is not
 *    a reliable guarantee. Locators below anchor on the "Primary Logo" heading text specifically.
 *  - LIVE FINDING (unresolved): entering an already-existing brand name (TC_03) did NOT produce
 *    any inline validation error - no aria-invalid change, no helper text, and the Next button
 *    still became enabled and successfully advanced to Step 2. The duplicate-name check may only
 *    happen server-side at Publish time; that was NOT tested live to avoid actually creating a
 *    duplicate brand in the real account. BRAND_NAME_ERROR below is kept as a best-guess fallback
 *    locator, but TC_03 as currently written (expecting an error at this step) does not match
 *    observed behavior - see BrandingHubTest's TC_03 for the caveat.
 */
public class BasicDetailsStepPage extends BasePage {

    // Verified live: real placeholder is exactly "Enter brand name".
    private static final By BRAND_NAME_INPUT = By.cssSelector("input[placeholder='Enter brand name']");

    // Unresolved/best-guess only - see class-level "LIVE FINDING" comment above. No error element
    // of any kind was observed for a duplicate name during live testing.
    private static final By BRAND_NAME_ERROR = By.id("brandName-helper-text");

    // Verified live: anchoring on the "Primary Logo" section heading and taking the first
    // following file input correctly targets that specific upload control, distinguishing it from
    // the other 3 file inputs (Secondary Logo, Favicon, Submark) also present on this step.
    private static final By PRIMARY_LOGO_FILE_INPUT =
            By.xpath("//*[normalize-space(text())='Primary Logo']/following::input[@type='file'][1]");

    // Verified live: after a successful upload, the app renders an <img> (pointing at the
    // uploaded file's CDN URL) in place of the empty upload placeholder - no data-testid exists,
    // so presence of that <img> right after the "Primary Logo" heading is the real accepted-signal.
    private static final By PRIMARY_LOGO_ACCEPTED_INDICATOR =
            By.xpath("//*[normalize-space(text())='Primary Logo']/following::img[1]");

    public BasicDetailsStepPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public void enterBrandName(String brandName) {
        type(BRAND_NAME_INPUT, brandName);
    }

    /**
     * Reads the field's current DOM value attribute - not visible text, since
     * WebElement.getText() on an &lt;input&gt; reads rendered text nodes, not its value. Kept
     * local to this class (rather than added to BasePage, which must not be modified) since no
     * other page currently needs to read back a raw input value, mirroring how LoginPage adds
     * its own small, page-specific helpers (e.g. readNativeValidationMessage()) for needs
     * BasePage doesn't cover.
     */
    public String getBrandNameValue() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(BRAND_NAME_INPUT)).getAttribute("value");
    }

    public boolean isBrandNameValidationErrorDisplayed() {
        return isDisplayed(BRAND_NAME_ERROR);
    }

    public String getBrandNameValidationError() {
        return getText(BRAND_NAME_ERROR);
    }

    /**
     * Uploads a file directly via sendKeys(absolutePath) on the native file input - no OS file
     * dialog automation involved. Uses presenceOfElementLocated rather than BasePage.type()
     * (which would wait for visibility and call clear() first) because file inputs are commonly
     * hidden behind a styled trigger button and clear() is not a meaningful/safe operation on a
     * file input in every browser.
     */
    public void uploadPrimaryLogo(String absoluteFilePath) {
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(PRIMARY_LOGO_FILE_INPUT));
        fileInput.sendKeys(absoluteFilePath);
    }

    public boolean isPrimaryLogoAccepted() {
        return isDisplayed(PRIMARY_LOGO_ACCEPTED_INDICATOR);
    }

    public boolean isBrandNameFieldPresent() {
        return isElementPresent(BRAND_NAME_INPUT);
    }

    public boolean isPrimaryLogoUploadPresent() {
        return isElementPresent(PRIMARY_LOGO_FILE_INPUT);
    }
}
