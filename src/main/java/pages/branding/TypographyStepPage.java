package pages.branding;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

/**
 * Page object for Step 2 of 4 (Typography) of the Brand creation wizard: the Font and Heading
 * dropdowns, the pencil/edit icon and its editor modal (X / Cancel / Update), and the
 * Desktop/Tablet/Mobile preview cards showing "Aa" sample text.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11. The dropdowns are confirmed real MUI Selects (a
 * {@code role="combobox"} trigger div opening a floating {@code role="listbox"} of
 * {@code <li role="option">} items) - the original MUI-style-select assumption was correct.
 * Confirmed real defaults: Font = "Roboto", Heading = "Heading H3" (matching the original guess
 * exactly); confirmed real alternate options exist for "Open Sans" and "Heading H1" (also
 * matching the original testdata guesses exactly).
 *
 * LIVE FINDING (unresolved - TC_12/TC_17 currently have nothing real to target): an exhaustive
 * inventory of every rendered icon (by lucide CSS class) and every element whose text matches
 * /edit/i anywhere on this step found NO pencil/edit icon and NO associated editor modal at all.
 * The one "file-pen-line" icon present on the page belongs to an unrelated background data-grid
 * component (from the underlying list page, which stays mounted behind the wizard overlay), not
 * to anything on this step. Either this feature does not exist in the current app build, or it
 * is conditionally rendered by a trigger not discovered during this investigation (deliberately
 * did not guess further to avoid fabricating locators for a feature that may not exist). The
 * PENCIL_EDIT_ICON/EDITOR_* locators below are left as best-guess placeholders for now.
 */
public class TypographyStepPage extends BasePage {

    // Verified live: no id exists on this element; its own visible text ("Font"'s adjacent label)
    // is the only anchor - see FONT_SELECT_TRIGGER's construction below.
    private static final By FONT_SELECT_TRIGGER =
            By.xpath("//p[normalize-space(text())='Font']/following::*[@role='combobox'][1]");

    // Verified live: this control has no distinct field label at all (its own current value,
    // e.g. "Heading H3", is the only visible text) and no id - it is confirmed to be exactly the
    // second role="combobox" element on this step, immediately after the Font trigger.
    private static final By HEADING_SELECT_TRIGGER = By.xpath("(//*[@role='combobox'])[2]");

    // UNRESOLVED - see class-level "LIVE FINDING" comment. No pencil/edit icon was found live.
    private static final By PENCIL_EDIT_ICON = By.xpath("//button[@aria-label='Edit typography']");

    private static final By EDITOR_DIALOG = By.xpath("//div[@role='dialog']");

    private static final By EDITOR_CLOSE_X_BUTTON = By.xpath("//div[@role='dialog']//button[@aria-label='close']");

    private static final By EDITOR_CANCEL_BUTTON =
            By.xpath("//div[@role='dialog']//button[.//*[normalize-space(text())='Cancel']]");

    private static final By EDITOR_UPDATE_BUTTON =
            By.xpath("//div[@role='dialog']//button[.//*[normalize-space(text())='Update']]");

    // Verified live: "Desktop"/"Tablet"/"Mobile" labels and exactly 3 "Aa" sample-text occurrences
    // both confirmed present on this step. The exact ancestor linking each label to its own "Aa"
    // instance was NOT pinned down live (time-boxed), so these deliberately check only the
    // device label's own visibility - a real, confirmed signal - rather than a compound
    // label+"Aa" xpath built on an unconfirmed nesting depth that could silently never match.
    private static final By DESKTOP_LABEL = By.xpath("//*[normalize-space(text())='Desktop']");

    private static final By TABLET_LABEL = By.xpath("//*[normalize-space(text())='Tablet']");

    private static final By MOBILE_LABEL = By.xpath("//*[normalize-space(text())='Mobile']");

    public TypographyStepPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public String getSelectedFont() {
        return getText(FONT_SELECT_TRIGGER);
    }

    public String getSelectedHeading() {
        return getText(HEADING_SELECT_TRIGGER);
    }

    public void selectFont(String optionText) {
        click(FONT_SELECT_TRIGGER);
        click(optionLocator(optionText));
    }

    public void selectHeading(String optionText) {
        click(HEADING_SELECT_TRIGGER);
        click(optionLocator(optionText));
    }

    public boolean isPencilIconVisible() {
        return isDisplayed(PENCIL_EDIT_ICON);
    }

    public boolean isPencilIconEnabled() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(PENCIL_EDIT_ICON)).isEnabled();
    }

    public void openPencilEditor() {
        click(PENCIL_EDIT_ICON);
    }

    public boolean isEditorOpen() {
        return isDisplayed(EDITOR_DIALOG);
    }

    public boolean isEditorCloseXVisible() {
        return isDisplayed(EDITOR_CLOSE_X_BUTTON);
    }

    public boolean isEditorCancelButtonVisible() {
        return isDisplayed(EDITOR_CANCEL_BUTTON);
    }

    public boolean isEditorUpdateButtonVisible() {
        return isDisplayed(EDITOR_UPDATE_BUTTON);
    }

    public void closeEditorWithCancel() {
        click(EDITOR_CANCEL_BUTTON);
    }

    public void closeEditorWithX() {
        click(EDITOR_CLOSE_X_BUTTON);
    }

    public void clickEditorUpdate() {
        click(EDITOR_UPDATE_BUTTON);
    }

    public boolean isDesktopPreviewVisible() {
        return isDisplayed(DESKTOP_LABEL);
    }

    public boolean isTabletPreviewVisible() {
        return isDisplayed(TABLET_LABEL);
    }

    public boolean isMobilePreviewVisible() {
        return isDisplayed(MOBILE_LABEL);
    }

    /** Verified live: options render as real &lt;li role="option"&gt; elements - the original guess was correct. */
    private static By optionLocator(String optionText) {
        return By.xpath("//li[@role='option'][normalize-space(text())=\"" + optionText + "\"]");
    }
}
