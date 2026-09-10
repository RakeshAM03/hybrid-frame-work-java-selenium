package pages.branding;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

/**
 * Page object for the wizard "chrome" shared by all 4 Brand creation steps (Basic Details,
 * Typography, Colors, Style): the "Step X of 4" indicator, the Back/Next/Publish/Exit controls
 * and their enabled state, the Exit confirmation dialog, and the right-side live brand preview
 * panel. Step-specific fields live in their own page classes
 * ({@link BasicDetailsStepPage}, {@link TypographyStepPage}, {@link ColorsStepPage},
 * {@link StyleStepPage}); BrandingHubTest composes this class together with whichever step page
 * is relevant for a given test case, since both wrap the same underlying wizard page.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11. The button-text xpaths below match text as EITHER a
 * direct text child of the button OR nested inside a descendant, since some of this app's
 * buttons (e.g. "+ Brand" on the list page) render their label as a direct text node after an
 * icon span while others may wrap it - matching both shapes makes these locators resilient to
 * that inconsistency instead of guessing one and breaking on the other.
 */
public class BrandWizardPage extends BasePage {

    // Verified live: real text is exactly "Step X of 4".
    private static final By STEP_INDICATOR =
            By.xpath("//*[contains(normalize-space(text()),'Step') and contains(normalize-space(text()),'of 4')]");

    private static final By BACK_BUTTON = buttonByText("Back");

    private static final By NEXT_BUTTON = buttonByText("Next");

    private static final By PUBLISH_BUTTON = buttonByText("Publish");

    // Verified live: this is a normal text button reading "Exit", NOT an icon-only button as
    // originally guessed - there is no aria-label at all.
    private static final By EXIT_BUTTON = buttonByText("Exit");

    // Verified live: standard MUI dialog role - shared by both the Exit and Delete confirmation
    // dialogs, which both use the same "Cancel" / "Confirm" button pair (see BrandingHubPage).
    private static final By EXIT_DIALOG = By.xpath("//div[@role='dialog']");

    // Verified live: the confirm button's real text is "Confirm", not "Exit" as originally guessed.
    private static final By EXIT_DIALOG_CONFIRM_BUTTON =
            By.xpath("//div[@role='dialog']//button[normalize-space(text())='Confirm']");

    private static final By EXIT_DIALOG_CANCEL_BUTTON =
            By.xpath("//div[@role='dialog']//button[normalize-space(text())='Cancel']");

    // Verified live: no data-testid or other attribute exists on the preview column at all. The
    // one stable, real text found inside it is this exact sentence, present on every step -
    // anchoring on it is more reliable than guessing an ancestor's exact nesting depth.
    private static final By PREVIEW_PANEL_ANCHOR = By.xpath("//*[contains(normalize-space(text()),'This is a brand preview')]");

    public BrandWizardPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public String getCurrentStepLabel() {
        return getText(STEP_INDICATOR);
    }

    public boolean isOnStep(String expectedStepLabelFragment) {
        try {
            return getCurrentStepLabel().contains(expectedStepLabelFragment);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isBackEnabled() {
        return isEnabled(BACK_BUTTON);
    }

    public boolean isNextEnabled() {
        return isEnabled(NEXT_BUTTON);
    }

    public boolean isNextButtonPresent() {
        return isElementPresent(NEXT_BUTTON);
    }

    public boolean isPublishEnabled() {
        return isEnabled(PUBLISH_BUTTON);
    }

    public boolean isPublishButtonPresent() {
        return isElementPresent(PUBLISH_BUTTON);
    }

    public void clickBack() {
        click(BACK_BUTTON);
    }

    public void clickNext() {
        click(NEXT_BUTTON);
    }

    public void clickPublish() {
        click(PUBLISH_BUTTON);
    }

    public void clickExit() {
        click(EXIT_BUTTON);
    }

    public boolean isExitDialogVisible() {
        return isDisplayed(EXIT_DIALOG);
    }

    public void confirmExit() {
        click(EXIT_DIALOG_CONFIRM_BUTTON);
    }

    public void cancelExit() {
        click(EXIT_DIALOG_CANCEL_BUTTON);
    }

    public boolean isPreviewPanelVisible() {
        return isDisplayed(PREVIEW_PANEL_ANCHOR);
    }

    /**
     * Returns the preview panel's anchor sentence itself. NOTE: unlike the anchor's mere
     * visibility (a solid, verified signal), the panel's exact outer boundary/ancestor was not
     * pinned down live, so this deliberately does NOT try to capture the panel's full text -
     * doing so accurately requires knowing that boundary. Callers doing cross-content checks
     * (e.g. TC_31, TC_41) should treat this as a presence signal, not a content diff.
     */
    public String getPreviewPanelText() {
        return getText(PREVIEW_PANEL_ANCHOR);
    }

    /**
     * Local enabled-state check, mirroring LoginPage.isLoginButtonEnabled(): uses
     * presenceOfElementLocated (not elementToBeClickable), because elementToBeClickable also
     * requires the element to already be enabled and would time out forever on a button we
     * expect to be disabled - we want to observe the state, not wait for it to become clickable.
     */
    private boolean isEnabled(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator)).isEnabled();
    }

    private static By buttonByText(String text) {
        return By.xpath("//button[normalize-space(text())='" + text + "' or .//*[normalize-space(text())='" + text + "']]");
    }
}
