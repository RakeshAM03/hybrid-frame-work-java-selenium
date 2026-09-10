package pages;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Page object for the Joveo Accounts login page (accounts.joveo.com/login), which the
 * application redirects to whenever an unauthenticated session hits the platform URL.
 *
 * Locators below were captured directly from the live DOM via DevTools/inspection, not guessed:
 *  - email / password inputs expose real id="email" / id="password" attributes.
 *  - the "Log In" button is a MUI <button> with no id/name/data-testid, so it is targeted by
 *    its visible text (only reasonably stable option available on this control).
 *  - the generic server-side error banner is a <p> that always renders as a sibling of an
 *    <svg data-testid="ErrorIcon">; its dismiss ("x") icon is the next sibling after that <p>
 *    (no stable id/testid exists on the icon itself, only its structural position is stable).
 *  - field-level client validation messages render with real, stable ids:
 *    "email-helper-text" and "password-helper-text".
 *
 * SINGLE-SESSION USAGE: LoginTest now navigates to this page ONCE for the whole scenario suite
 * (see BaseTest/LoginTest @BeforeClass). Never call open() again mid-suite - call
 * resetLoginForm() between attempts instead, which clears the fields and dismisses any visible
 * error without a page reload.
 *
 * ROOT-CAUSE INVESTIGATION for the "empty email + empty password" scenario (done live against
 * the real app via DevTools + network trace, under both navigation models):
 *  - Neither a native HTML5 "required" attribute nor a disabled Log In button is involved at
 *    any point - getEmailNativeValidationMessage()/getPasswordNativeValidationMessage() are
 *    empty and isLoginButtonEnabled() is always true, confirmed live. The two defensive checks
 *    below exist only in case that ever changes; they are not what currently drives the
 *    behavior.
 *  - On a freshly loaded, never-touched form, clicking "Log In" with both fields empty is NOT
 *    blocked client-side: it fires a real POST to /user/v1/login, which returns 400, and the
 *    app renders the same generic credentials-incorrect banner used for wrong-password/
 *    unregistered-email.
 *  - BUT once the email/password fields have already been interacted with earlier in the SAME
 *    browser session - exactly what happens now that scenarios 1-5 run first against one shared
 *    page load - the form already considers both fields "touched". Clearing them back to empty
 *    and submitting now returns 422 from the same endpoint, and the app instead renders BOTH
 *    field-level required messages ("Email is required" and "Password is required") together.
 *    That is why LoginTest's expected message for scenario 6 combines both field-required
 *    strings rather than reusing the generic banner text from scenarios 1/2.
 *
 * ROOT-CAUSE INVESTIGATION for leftover text carrying across scenarios (found by actually
 * running the single-session suite: from scenario 2 onward every scenario kept reporting
 * scenario 3's "Please enter a valid email address" error, regardless of what was typed
 * afterwards): plain WebElement.clear() was not enough. This email field is a React/MUI
 * controlled input - its on-screen value is driven by component state, not just the raw DOM
 * value. clear() empties the DOM node directly without going through a real keyboard event, so
 * on some renders the framework's own state (and therefore its validation, which reads that
 * state, not the DOM) never learns the field became empty and keeps validating the *previous*
 * value. Because clear() also does not throw when this happens, the mismatch was silent. A
 * later sendKeys(newValue) still appends onto whatever the framework's stale internal value
 * was, not onto a truly empty field, so the "invalid format" state can persist indefinitely.
 * clearFieldRobustly() below fixes this with three layered steps (see its comments) and then
 * verifies the DOM actually reports value="" before proceeding, instead of assuming clear()
 * worked.
 */
public class LoginPage extends BasePage {

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    // Used only by clearFieldRobustly() to re-locate each field fresh from the driver right
    // before clearing it, rather than reusing the @FindBy WebElement references above - a prior
    // validation error can re-render this input's DOM node, and we want a guaranteed-current
    // handle for the verify-after-clear step below, not one obtained earlier in the scenario.
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");

    private static final By LOGIN_BUTTON =
            By.xpath("//button[.//*[normalize-space(text())='Log In']]");

    private static final By GENERIC_ERROR_BANNER =
            By.xpath("//*[@data-testid='ErrorIcon']/following-sibling::p");

    private static final By GENERIC_ERROR_DISMISS_ICON =
            By.xpath("//*[@data-testid='ErrorIcon']/following-sibling::p/following-sibling::*[1]");

    private static final By EMAIL_FIELD_ERROR = By.id("email-helper-text");

    private static final By PASSWORD_FIELD_ERROR = By.id("password-helper-text");

    /** Separator used whenever more than one error is visible at once; reused by LoginTest so the combined
     *  scenario-6 expectation is built from real property values instead of a new hardcoded literal. */
    public static final String ERROR_MESSAGE_SEPARATOR = "; ";

    public LoginPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public LoginPage open() {
        driver.get(ConfigReader.getBaseUrl());
        wait.until(ExpectedConditions.visibilityOf(emailInput));
        return this;
    }

    public void enterEmail(String email) {
        type(emailInput, email);
    }

    public void enterPassword(String password) {
        type(passwordInput, password);
    }

    public void clickLogin() {
        click(LOGIN_BUTTON);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }

    /**
     * Clears both fields (robustly - see clearFieldRobustly()) and dismisses any visible generic
     * error banner, WITHOUT reloading the page, so the next scenario starts from a genuinely
     * clean form state - not just a clean-*looking* one - within the same browser session and
     * driver instance. Never uses driver.navigate().refresh() or driver.get(...).
     *
     * Field-level required/format messages (email/password helper text) are intentionally NOT
     * force-dismissed here: they are recomputed live from the field's current value, so the next
     * scenario's enterEmail()/enterPassword() call naturally updates or clears them.
     */
    public void resetLoginForm() {
        logger.info("Resetting login form in place before next scenario (no page reload)");
        clearFieldRobustly(EMAIL_INPUT, "email");
        clearFieldRobustly(PASSWORD_INPUT, "password");
        dismissGenericErrorBannerIfPresent();
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
    }

    /**
     * Empties a field and VERIFIES it actually ended up empty, retrying the whole sequence once
     * before failing loudly - never silently proceeding with a field that still holds leftover
     * text from a previous scenario (see class-level "leftover text" comment for why plain
     * clear() alone was not reliable here).
     */
    private void clearFieldRobustly(By locator, String fieldName) {
        runClearSequence(locator);
        if (isFieldValueEmpty(locator)) {
            return;
        }

        logger.warning("Field '" + fieldName + "' still not empty after the first clear sequence; retrying once.");
        runClearSequence(locator);
        if (isFieldValueEmpty(locator)) {
            return;
        }

        throw new IllegalStateException("Failed to clear the '" + fieldName
                + "' field after two full clear attempts (clear() + select-all/delete + JS force-clear). "
                + "Refusing to continue with a scenario while this field may still hold leftover text "
                + "from a previous attempt.");
    }

    /**
     * Runs all three clearing steps unconditionally, from weakest to strongest, against a freshly
     * re-located element each time (never a WebElement handle obtained earlier in the scenario):
     *   1. element.clear() - the standard WebDriver call. Often sufficient, but on this
     *      React/MUI-controlled input it can empty the visible DOM value without the framework's
     *      own component state (which drives validation) ever being told the field changed.
     *   2. Select-all + Delete via real key events (Keys.chord). This exercises the same
     *      keydown/input event path a real user pressing Ctrl/Cmd+A then Delete would produce,
     *      which a controlled input's onChange/onInput handler is far more likely to observe
     *      than a bare clear(). Cmd is used on Mac (Ctrl+A does not select-all in a Mac text
     *      field - it's the Emacs-style "move to line start" binding there), Ctrl everywhere else.
     *   3. JavascriptExecutor force-clear as a last resort: directly sets the DOM value to "" and
     *      manually fires 'input' and 'change' events, so a framework whose handlers are
     *      listening for those specific events (rather than raw key events) still gets notified,
     *      even if step 2 alone did not do it.
     * clearFieldRobustly() then verifies the outcome once all three steps have run - not after
     * each individual step - and only retries/fails based on that final check.
     */
    private void runClearSequence(By locator) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();

        element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        Keys selectAllModifier = Platform.getCurrent().is(Platform.MAC) ? Keys.COMMAND : Keys.CONTROL;
        element.sendKeys(Keys.chord(selectAllModifier, "a"), Keys.DELETE);

        element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = ''; "
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                element);
    }

    private boolean isFieldValueEmpty(By locator) {
        try {
            return wait.until(ExpectedConditions.attributeToBe(locator, "value", ""));
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void dismissGenericErrorBannerIfPresent() {
        if (isElementPresent(GENERIC_ERROR_BANNER)) {
            click(GENERIC_ERROR_DISMISS_ICON);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(GENERIC_ERROR_BANNER));
        }
    }

    public boolean isLoginErrorDisplayed() {
        return isDisplayed(GENERIC_ERROR_BANNER)
                || isDisplayed(EMAIL_FIELD_ERROR)
                || isDisplayed(PASSWORD_FIELD_ERROR);
    }

    /**
     * Returns whichever error text is currently visible: the generic server-side banner, the
     * field-level client validation helper text(s), or - only as a fallback, see class-level
     * comment - native HTML5 validation messages. Combines multiple if more than one happens to
     * be visible at once, joined by ERROR_MESSAGE_SEPARATOR.
     */
    public String getErrorMessage() {
        StringBuilder message = new StringBuilder();
        if (isElementPresent(GENERIC_ERROR_BANNER)) {
            appendWithSeparator(message, getText(GENERIC_ERROR_BANNER));
        }
        if (isElementPresent(EMAIL_FIELD_ERROR)) {
            appendWithSeparator(message, getText(EMAIL_FIELD_ERROR));
        }
        if (isElementPresent(PASSWORD_FIELD_ERROR)) {
            appendWithSeparator(message, getText(PASSWORD_FIELD_ERROR));
        }

        // Defensive fallback only (not currently exercised): if this app ever starts using real
        // native "required" attributes instead of its own JS-driven validation, surface that
        // message here instead of asserting an empty string and failing with no explanation.
        if (message.length() == 0) {
            appendIfPresent(message, getEmailNativeValidationMessage());
            appendIfPresent(message, getPasswordNativeValidationMessage());
        }

        return message.toString();
    }

    /**
     * Defensive check requested for scenario 6: confirms whether the Log In button is disabled
     * rather than assuming a click-then-error flow always applies. Verified live that this
     * button stays enabled even with both fields empty - it is never disabled by this app - but
     * the check is kept so a future change would be caught explicitly.
     */
    public boolean isLoginButtonEnabled() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(LOGIN_BUTTON)).isEnabled();
    }

    /**
     * Defensive check requested for scenario 6: reads the browser's native HTML5
     * validationMessage instead of assuming a custom on-page element always renders. Verified
     * live that this field carries no native "required" constraint - validationMessage is always
     * empty - so this is not the real mechanism at play, but is kept as a safe fallback.
     */
    public String getEmailNativeValidationMessage() {
        return readNativeValidationMessage(emailInput);
    }

    public String getPasswordNativeValidationMessage() {
        return readNativeValidationMessage(passwordInput);
    }

    private String readNativeValidationMessage(WebElement element) {
        Object result = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].validationMessage;", element);
        return result == null ? "" : result.toString();
    }

    private void appendWithSeparator(StringBuilder builder, String text) {
        if (builder.length() > 0) {
            builder.append(ERROR_MESSAGE_SEPARATOR);
        }
        builder.append(text);
    }

    private void appendIfPresent(StringBuilder builder, String text) {
        if (text != null && !text.isEmpty()) {
            appendWithSeparator(builder, text);
        }
    }
}
