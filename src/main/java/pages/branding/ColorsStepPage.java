package pages.branding;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Page object for Step 3 of 4 (Colors) of the Brand creation wizard.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11, and the real structure differs from the original design
 * in several important ways:
 *  - The 7 real section names are Primary, Secondary, Gray, Success, Error, Info, Warning (NOT
 *    the guessed background/surface/text) with real default HEX values #3A86FF, #FFB33A,
 *    #9FA6B2, #388e3c, #E35141, #2196F3, #f57c00 respectively (see testdata.properties).
 *  - Each section renders as: an {@code <h6>} label, then an {@code <h6>} showing the HEX value
 *    as plain READ-ONLY TEXT (not an input!), then 4 plain, unattributed {@code <div>} color
 *    swatches with no children: one 68px "main" swatch followed by three 30px shade swatches
 *    (a lighter tint, the same base color again, and a darker shade).
 *  - Clicking the MAIN (68px) swatch opens a real color picker - it is the well-known open-source
 *    "react-color" library's ChromePicker component (recognizable by its stable, public
 *    "chrome-picker"/"flexbox-fix" class names), which has NO Cancel/Apply/X button at all -
 *    every edit inside it applies live, and it closes only via a standard MUI click-away (a real
 *    click anywhere outside it, which a synthetic JS .click() does not trigger).
 *  - LIVE FINDING (changes TC_27/TC_29's premise): the 3 small "shade" swatches are static,
 *    non-interactive color chips - clicking one does NOT open any popover or picker. Only the
 *    main swatch is interactive. "4 hex values to cross-validate" (TC_29) is therefore
 *    reinterpreted here as the computed colors of all 4 swatches in a section (1 main + 3 shades),
 *    read via WebElement.getCssValue("background-color") - not 4 options inside a shade picker,
 *    which does not exist.
 */
public class ColorsStepPage extends BasePage {

    // Verified live real section display names/order.
    public static final List<String> DEFAULT_SECTION_KEYS =
            Arrays.asList("Primary", "Secondary", "Gray", "Success", "Error", "Info", "Warning");

    // Verified live: a stable heading unique to this step, used as a neutral click-away target to
    // close the color picker popover (see closePickerByClickingAway()) - clicking any element
    // outside the picker closes it, and this heading is never inside the picker itself.
    private static final By CLICK_AWAY_TARGET = By.xpath("//*[normalize-space(text())='Select Your Colors']");

    // Verified live: this is react-color's ChromePicker component - "chrome-picker" is that
    // library's own stable, public CSS class, not an app-specific guess.
    private static final By COLOR_PICKER_POPOVER = By.cssSelector(".chrome-picker");

    // Verified live: react-color's Chrome picker layout places the hex field first among its
    // inputs (hex, then r/g/b/a numeric fields after) - the first input inside .chrome-picker is
    // confirmed (by its live value matching the section's current hex) to be the hex field.
    private static final By COLOR_PICKER_HEX_INPUT = By.cssSelector(".chrome-picker input");

    public ColorsStepPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isColorSectionVisible(String sectionName) {
        return isDisplayed(sectionLabelLocator(sectionName));
    }

    /** Reads the section's HEX value as plain text (there is no input to read a "value" attribute from). */
    public String getSectionHex(String sectionName) {
        return getText(hexTextLocator(sectionName));
    }

    public void openMainSwatchPicker(String sectionName) {
        click(mainSwatchLocator(sectionName));
    }

    /**
     * Kept for API completeness, but see the class-level "LIVE FINDING" comment: clicking a
     * shade swatch does NOT open a picker in the real app - calling this will click the element,
     * and isPickerPopoverOpen() is expected to correctly report false afterward.
     */
    public void openShadeSwatchPicker(String sectionName) {
        click(shadeSwatchLocator(sectionName, 1));
    }

    public boolean isPickerPopoverOpen() {
        return isDisplayed(COLOR_PICKER_POPOVER);
    }

    public void typeHexInPicker(String hexValue) {
        type(COLOR_PICKER_HEX_INPUT, hexValue);
    }

    /**
     * Closes the open color picker via a real click on a neutral element outside it (MUI's
     * click-away dismissal - confirmed live that a synthetic click does not trigger this, only a
     * genuine pointer click does). There is no Cancel/X/Apply button to click instead - every
     * edit already applied live the moment it was typed, so "closing without applying" (TC_27)
     * only holds true if nothing was typed before calling this.
     */
    public void closePickerByClickingAway() {
        click(CLICK_AWAY_TARGET);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(COLOR_PICKER_POPOVER));
    }

    /**
     * Returns the computed background-color of all 4 swatches in a section (1 main + 3 shades),
     * for TC_29's cross-validation. Uses getCssValue rather than a hex attribute since these are
     * plain, styled &lt;div&gt;s with no color-related attribute at all.
     */
    public List<String> getAllSwatchColors(String sectionName) {
        List<WebElement> swatches = driver.findElements(allSwatchesLocator(sectionName));
        List<String> colors = new ArrayList<>();
        for (WebElement swatch : swatches) {
            colors.add(swatch.getCssValue("background-color"));
        }
        return colors;
    }

    /** Verified live: the section's own &lt;h6&gt; label. */
    private static By sectionLabelLocator(String sectionName) {
        return By.xpath("//h6[normalize-space(text())='" + sectionName + "']");
    }

    /** Verified live: the very next &lt;h6&gt; after the label is the read-only HEX text. */
    private static By hexTextLocator(String sectionName) {
        return By.xpath("//h6[normalize-space(text())='" + sectionName + "']/following::h6[1]");
    }

    /** Verified live: the first of the section's 4 childless swatch &lt;div&gt;s is the larger "main" swatch. */
    private static By mainSwatchLocator(String sectionName) {
        return By.xpath("//h6[normalize-space(text())='" + sectionName + "']/parent::div//div[not(*)][1]");
    }

    /** Verified live: shade swatches are the 2nd-4th childless divs; shadeIndex is 1-3. */
    private static By shadeSwatchLocator(String sectionName, int shadeIndex) {
        return By.xpath("//h6[normalize-space(text())='" + sectionName + "']/parent::div//div[not(*)][" + (shadeIndex + 1) + "]");
    }

    private static By allSwatchesLocator(String sectionName) {
        return By.xpath("//h6[normalize-space(text())='" + sectionName + "']/parent::div//div[not(*)]");
    }
}
