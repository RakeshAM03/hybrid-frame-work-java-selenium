package pages.branding;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

/**
 * Page object for Step 4 of 4 (Style) of the Brand creation wizard: the "Edges" control - its
 * label, current numeric value, decrement/increment icons, and direct numeric entry.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11, with one major structural correction: the +/- controls
 * are NOT &lt;button&gt; elements at all (the original guess assumed
 * button[aria-label='Increase/Decrease edges'], which does not exist) - they are bare
 * &lt;svg&gt; icons using MUI's own stock "Remove"/"Add" icon paths directly, with no wrapping
 * button and no aria-label. They are located here by their exact, library-stable SVG path data.
 *
 * LIVE FINDING (unresolved): the default value is confirmed to be 4, matching the original guess.
 * But clicking the increment icon was observed to increase the value inconsistently - one click
 * went 4 -&gt; 6 (+2), the very next click went 6 -&gt; 7 (+1) - rather than a clean, fixed step
 * size. This was not resolved further (would need dedicated investigation, e.g. checking for a
 * press-and-hold acceleration feature or a debounce/state-sync race) - TC_38's assumption of a
 * fixed +1/-1 step size does not match what was observed and should be re-verified before relying
 * on this test's exact increment/decrement assertions. Real min/max boundaries were also not
 * reached/confirmed live - branding.style.edgeMin/edgeMax in testdata.properties remain guesses.
 */
public class StyleStepPage extends BasePage {

    private static final By EDGES_LABEL = By.xpath("//*[normalize-space(text())='Edges']");

    // Verified live: the numeric TextField immediately following the "Edges" label - confirmed
    // value="4" by default.
    private static final By EDGES_VALUE_INPUT = By.xpath("//*[normalize-space(text())='Edges']/following::input[1]");

    // Verified live: MUI's stock "Remove" icon path (@mui/icons-material RemoveIcon), rendered as
    // a bare, unwrapped <svg> - not inside any <button>.
    private static final By EDGES_DECREMENT_ICON =
            By.xpath("//*[local-name()='path' and @d='M19 13H5v-2h14z']/parent::*[local-name()='svg']");

    // Verified live: MUI's stock "Add" icon path (@mui/icons-material AddIcon), same caveat as above.
    private static final By EDGES_INCREMENT_ICON =
            By.xpath("//*[local-name()='path' and @d='M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z']/parent::*[local-name()='svg']");

    public StyleStepPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isEdgesLabelVisible() {
        return isDisplayed(EDGES_LABEL);
    }

    public boolean isEdgesControlVisible() {
        return isDisplayed(EDGES_LABEL) && isDisplayed(EDGES_DECREMENT_ICON) && isDisplayed(EDGES_INCREMENT_ICON);
    }

    /** Reads the field's current DOM value attribute, mirroring BasicDetailsStepPage.getBrandNameValue(). */
    public int getEdgeValue() {
        String value = wait.until(ExpectedConditions.visibilityOfElementLocated(EDGES_VALUE_INPUT)).getAttribute("value");
        return Integer.parseInt(value.trim());
    }

    public void clickIncrement() {
        click(EDGES_INCREMENT_ICON);
    }

    public void clickDecrement() {
        click(EDGES_DECREMENT_ICON);
    }

    public void setEdgeValueDirectly(int value) {
        type(EDGES_VALUE_INPUT, String.valueOf(value));
    }

    /**
     * UNVERIFIED at the boundary: since the +/- controls are bare SVG icons rather than real
     * form-control buttons, WebElement.isEnabled() may not reflect a meaningful disabled state
     * for them the way it would for a &lt;button disabled&gt; - this was not tested against an
     * actual min/max boundary live. Treat these two methods as best-effort until re-verified.
     */
    public boolean isIncrementEnabled() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(EDGES_INCREMENT_ICON)).isEnabled();
    }

    public boolean isDecrementEnabled() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(EDGES_DECREMENT_ICON)).isEnabled();
    }
}
