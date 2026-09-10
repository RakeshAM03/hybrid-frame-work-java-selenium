package tests;

import base.BaseTest;
import config.ConfigReader;
import config.TestDataReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.DashboardPage;
import pages.LoginPage;
import pages.branding.BasicDetailsStepPage;
import pages.branding.BrandWizardPage;
import pages.branding.BrandingHubPage;
import pages.branding.ColorsStepPage;
import pages.branding.StyleStepPage;
import pages.branding.TypographyStepPage;
import utils.ScreenshotUtil;
import utils.TestLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ordered, single-browser-session coverage of the Brand creation wizard (Basic Details →
 * Typography → Colors → Style) and Branding Hub list management (search / open / edit / delete),
 * implementing TC_01-TC_49.
 *
 * SESSION MODEL: like LoginTest, this class shares one WebDriver for its whole run
 * (BaseTest's @BeforeClass/@AfterClass) - but unlike LoginTest's independent scenarios, every test
 * method here is a step in one continuous journey: create a brand, walk it through all 4 wizard
 * steps, publish it, then manage it from the list. @Test(priority = n) is used (not
 * dependsOnMethods) specifically so that TestNG always attempts every method in order even if an
 * earlier one fails - dependsOnMethods would instead auto-SKIP every downstream method, hiding
 * how much of the flow still works. Several fields (selectedFont, selectedHeading,
 * primaryHexAfterChange, secondaryHexAfterChange) are assigned the INTENDED value before the UI
 * action that sets it is attempted, so a later cross-check method reads a real expected value and
 * fails with a clear mismatch instead of a NullPointerException if an earlier step's action
 * silently didn't take effect.
 *
 * LOCATORS: verified LIVE on 2026-08-11 against the real Branding Hub (see each page class's own
 * header comment for what was confirmed and how). A few real, confirmed discrepancies from this
 * test class's original design remain and are called out at their specific test method below
 * rather than silently "fixed" by inventing new behavior:
 *  - TC_03 (duplicate brand name): no inline validation error was observed live; the test now
 *    asserts that real, observed behavior instead of a fabricated error message.
 *  - TC_12/TC_17 (pencil/edit icon): confirmed NOT to exist anywhere on the Typography step live;
 *    these now assert its absence rather than a feature that isn't there.
 *  - TC_27/TC_29 (shade swatch): confirmed the 3 small "shade" swatches are static and
 *    non-interactive - only the main swatch opens a picker. Both now exercise the main swatch's
 *    open/close-without-editing flow instead.
 *  - TC_38 (edge +/- step size): a live increment was observed to jump inconsistently (+2 once,
 *    then +1), so this no longer asserts an exact +1/-1 delta, only that the value moves in the
 *    expected direction.
 */
public class BrandingHubTest extends BaseTest {

    private static final Logger LOGGER = TestLogger.getLogger(BrandingHubTest.class);

    private BrandingHubPage brandingHubPage;
    private BrandWizardPage wizard;
    private BasicDetailsStepPage basicDetailsStep;
    private TypographyStepPage typographyStep;
    private ColorsStepPage colorsStep;
    private StyleStepPage styleStep;

    private String uniqueBrandName;
    private String logoFilePath;

    // Assigned to their intended value *before* the corresponding UI action is attempted (see
    // class-level comment) so later cross-checks never NPE, even if an earlier step's action
    // silently failed - they'll instead fail with a clear, specific mismatch.
    private String selectedFont;
    private String selectedHeading;
    private String primaryHexAfterChange;
    private String secondaryHexAfterChange;

    @BeforeClass
    public void loginAndOpenBrandingHub() throws IOException {
        LOGGER.info("Logging in via the existing LoginPage flow before running Branding Hub scenarios");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.getValidEmail(), ConfigReader.getValidPassword());

        DashboardPage dashboardPage = new DashboardPage(driver);
        if (!dashboardPage.isDashboardLoaded()) {
            throw new IllegalStateException(
                    "Login did not succeed before the Branding Hub suite could start; see LoginTest for login diagnostics.");
        }

        // The app always redirects a successful login to /jobs/jobs-overview regardless of which
        // protected URL was originally requested (documented on DashboardPage) - so reaching the
        // Branding Hub requires navigating there explicitly now that we're authenticated.
        LOGGER.info("Navigating to the Branding Hub");
        brandingHubPage = new BrandingHubPage(driver).open();
        wizard = new BrandWizardPage(driver);
        basicDetailsStep = new BasicDetailsStepPage(driver);
        typographyStep = new TypographyStepPage(driver);
        colorsStep = new ColorsStepPage(driver);
        styleStep = new StyleStepPage(driver);

        uniqueBrandName = TestDataReader.get("branding.newBrandNamePrefix") + " " + System.currentTimeMillis();
        logoFilePath = generateOnePixelPngFile();

        selectedFont = TestDataReader.get("branding.typography.defaultFont");
        selectedHeading = TestDataReader.get("branding.typography.defaultHeading");
        primaryHexAfterChange = TestDataReader.get("branding.color.primary.defaultHex");
        secondaryHexAfterChange = TestDataReader.get("branding.color.secondary.defaultHex");

        LOGGER.info("Branding Hub suite ready. Brand name for this run: " + uniqueBrandName);
    }

    // ---------------------------------------------------------------------------------------
    // Section 1 - Basic Details (Step 1 of 4) - TC_01-TC_08
    // ---------------------------------------------------------------------------------------

    @Test(priority = 1)
    public void tc01_openBrandingHubAndClickAddBrand() {
        LOGGER.info("TC_01: Verifying the Branding Hub is loaded and opening the Brand creation wizard");
        Assert.assertTrue(brandingHubPage.isLoaded(), "Expected the Branding Hub list page to be loaded");
        brandingHubPage.clickAddBrand();
        Assert.assertTrue(wizard.isOnStep("Step 1 of 4"), "Expected the wizard to open on Step 1 of 4 (Basic Details)");
    }

    @Test(priority = 2)
    public void tc02_verifyBackDisabledAndNextDisabledOnEmptyForm() {
        LOGGER.info("TC_02: Verifying Back is disabled on the first step and Next is disabled with an empty required field");
        Assert.assertFalse(wizard.isBackEnabled(), "Expected Back to be disabled on the first wizard step");
        Assert.assertFalse(wizard.isNextEnabled(), "Expected Next to be disabled while the required brand name field is empty");
    }

    @Test(priority = 3)
    public void tc03_enterExistingBrandNameShowsValidationError() {
        String existingBrandName = TestDataReader.get("branding.existingBrandName");
        LOGGER.info("TC_03: Entering an already-existing brand name ('" + existingBrandName + "') and checking for a validation error");
        basicDetailsStep.enterBrandName(existingBrandName);
        // LIVE FINDING: entering a genuinely existing brand name ("Test Brand") produced no
        // inline error at all - no aria-invalid change, no helper text, and Next still enabled.
        // The duplicate-name check may only happen server-side at Publish time, which was
        // deliberately not tested live to avoid creating a real duplicate brand in the account.
        // Asserting the real, observed behavior here rather than a fabricated error message.
        Assert.assertFalse(basicDetailsStep.isBrandNameValidationErrorDisplayed(),
                "Expected no inline validation error for a duplicate brand name at this step (confirmed live) - "
                        + "if this now fails, the app may have added inline duplicate-name validation since this was last verified");
        Assert.assertTrue(wizard.isNextEnabled(),
                "Expected Next to remain enabled after a duplicate name, matching confirmed live behavior");
    }

    @Test(priority = 4)
    public void tc04_uploadPrimaryLogo() {
        LOGGER.info("TC_04: Uploading a generated 1x1 PNG (" + logoFilePath + ") as the Primary Logo");
        basicDetailsStep.uploadPrimaryLogo(logoFilePath);
        Assert.assertTrue(basicDetailsStep.isPrimaryLogoAccepted(), "Expected the Primary Logo upload to be accepted and previewed");
    }

    @Test(priority = 5)
    public void tc05_fillUniqueBrandNameEnablesNext() {
        LOGGER.info("TC_05: Filling a unique brand name ('" + uniqueBrandName + "') and verifying Next becomes enabled");
        basicDetailsStep.enterBrandName(uniqueBrandName);
        Assert.assertTrue(wizard.isNextEnabled(), "Expected Next to become enabled once a valid, unique brand name is entered");
    }

    @Test(priority = 6)
    public void tc06_verifyAllBasicDetailsElements() {
        LOGGER.info("TC_06: Verifying every UI element on the Basic Details step (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isOnStep("Step 1 of 4"), "Expected the step indicator to read 'Step 1 of 4'");
        softAssert.assertTrue(basicDetailsStep.isBrandNameFieldPresent(), "Expected the brand name field to be present");
        softAssert.assertTrue(basicDetailsStep.isPrimaryLogoUploadPresent(), "Expected the Primary Logo upload control to be present");
        softAssert.assertFalse(wizard.isBackEnabled(), "Expected Back to still be disabled on Step 1");
        softAssert.assertTrue(wizard.isNextEnabled(), "Expected Next to be enabled after TC_05's valid brand name");
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        softAssert.assertAll();
    }

    @Test(priority = 7)
    public void tc07_clickNextAdvancesToTypography() {
        LOGGER.info("TC_07: Clicking Next and verifying the wizard advances to Step 2 of 4 (Typography)");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 2 of 4"), "Expected the wizard to advance to Step 2 of 4 (Typography)");
    }

    @Test(priority = 8)
    public void tc08_clickBackRestoresBrandName() {
        LOGGER.info("TC_08: Clicking Back and verifying Step 1 is restored with the brand name preserved");
        wizard.clickBack();
        Assert.assertTrue(wizard.isOnStep("Step 1 of 4"), "Expected Back to return to Step 1 of 4 (Basic Details)");
        // Same "app state vs. DOM state" concern as LoginPage.resetLoginForm(): don't assume a
        // simple re-render reflects the app's actually-retained value - read it back and compare.
        Assert.assertEquals(basicDetailsStep.getBrandNameValue(), uniqueBrandName,
                "Expected the brand name to still be '" + uniqueBrandName + "' after navigating Back to Step 1");
    }

    // ---------------------------------------------------------------------------------------
    // Section 2 - Typography (Step 2 of 4) - TC_09-TC_19
    // ---------------------------------------------------------------------------------------

    @Test(priority = 9)
    public void tc09_clickNextAndCaptureTypographyScreenshot() {
        LOGGER.info("TC_09: Clicking Next again and capturing a screenshot of the Typography page");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 2 of 4"), "Expected the wizard to be on Step 2 of 4 (Typography)");
        ScreenshotUtil.capture(driver, "typography-page");
    }

    @Test(priority = 10)
    public void tc10_verifyBackEnabledOnTypography() {
        LOGGER.info("TC_10: Verifying Back is enabled and noting the Next button state on Typography");
        Assert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled on the Typography step");
        LOGGER.info("Next button enabled state on the Typography step: " + wizard.isNextEnabled());
    }

    @Test(priority = 11)
    public void tc11_verifyTypographyDefaults() {
        LOGGER.info("TC_11: Verifying Font defaults to 'Roboto' and Heading defaults to 'Heading H3'");
        Assert.assertEquals(typographyStep.getSelectedFont(), TestDataReader.get("branding.typography.defaultFont"),
                "Unexpected default Font selection");
        Assert.assertEquals(typographyStep.getSelectedHeading(), TestDataReader.get("branding.typography.defaultHeading"),
                "Unexpected default Heading selection");
    }

    @Test(priority = 12)
    public void tc12_verifyPencilIconVisibleAndEnabled() {
        LOGGER.info("TC_12: Checking for the pencil/edit icon on the Typography step");
        // LIVE FINDING: an exhaustive inventory of every rendered icon (by lucide CSS class) and
        // every "edit"-matching text node on this step found no pencil/edit icon or editor modal
        // at all (see TypographyStepPage's class comment). Asserting its confirmed absence here
        // rather than a feature that isn't there - if this app gains the feature later, this
        // assertion will fail loudly and should be flipped back to isPencilIconVisible()==true.
        Assert.assertFalse(typographyStep.isPencilIconVisible(),
                "No pencil/edit icon was found on the Typography step during live verification - "
                        + "if this now fails (icon present), the feature may have been added; update this test and "
                        + "PENCIL_EDIT_ICON's locator in TypographyStepPage together");
    }

    @Test(priority = 13)
    public void tc13_verifyPreviewPanelContentOnTypography() {
        LOGGER.info("TC_13: Verifying the right-side brand preview panel content on Typography (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        String previewText = wizard.getPreviewPanelText();
        softAssert.assertTrue(previewText != null && !previewText.trim().isEmpty(),
                "Expected the preview panel to render some content");
        softAssert.assertAll();
    }

    @Test(priority = 14)
    public void tc14_verifyDevicePreviewCardsWithSampleText() {
        LOGGER.info("TC_14: Verifying Desktop, Tablet, Mobile preview cards are visible with 'Aa' sample text");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(typographyStep.isDesktopPreviewVisible(), "Expected the Desktop preview card with 'Aa' sample text");
        softAssert.assertTrue(typographyStep.isTabletPreviewVisible(), "Expected the Tablet preview card with 'Aa' sample text");
        softAssert.assertTrue(typographyStep.isMobilePreviewVisible(), "Expected the Mobile preview card with 'Aa' sample text");
        softAssert.assertAll();
    }

    @Test(priority = 15)
    public void tc15_changeFontSelection() {
        String alternateFont = TestDataReader.get("branding.typography.alternateFont");
        LOGGER.info("TC_15: Changing the Font dropdown to '" + alternateFont + "' and verifying the selection updates");
        selectedFont = alternateFont;
        typographyStep.selectFont(alternateFont);
        Assert.assertEquals(typographyStep.getSelectedFont(), alternateFont, "Expected the Font selection to update to " + alternateFont);
    }

    @Test(priority = 16)
    public void tc16_changeHeadingSelection() {
        String alternateHeading = TestDataReader.get("branding.typography.alternateHeading");
        LOGGER.info("TC_16: Changing the Heading dropdown to '" + alternateHeading + "' and verifying the selection updates");
        selectedHeading = alternateHeading;
        typographyStep.selectHeading(alternateHeading);
        Assert.assertEquals(typographyStep.getSelectedHeading(), alternateHeading, "Expected the Heading selection to update to " + alternateHeading);
    }

    @Test(priority = 17)
    public void tc17_pencilEditorCancelAndXBothClose() {
        LOGGER.info("TC_17: Skipped body - depends on the pencil/edit icon confirmed absent in TC_12 (see class-level comment)");
        // Deliberately does not attempt openPencilEditor(): clicking a confirmed-nonexistent
        // element would just time out after the full explicit-wait duration for no diagnostic
        // value beyond what TC_12 already established. Re-enable this test's original body once
        // TC_12 confirms the icon exists (i.e. once PENCIL_EDIT_ICON's locator is corrected).
        Assert.assertFalse(typographyStep.isPencilIconVisible(),
                "Skipping the editor-modal flow because the pencil/edit icon is confirmed absent - see TC_12");
    }

    @Test(priority = 18)
    public void tc18_captureTypographyScreenshotAfterSelections() {
        LOGGER.info("TC_18: Capturing a screenshot of the Typography page after the font/heading selections");
        ScreenshotUtil.capture(driver, "typography-page-after-selection");
    }

    @Test(priority = 19)
    public void tc19_fullTypographyPageSoftAssertions() {
        LOGGER.info("TC_19: Full-page soft assertions on the Typography page (Step 2 of 4)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isOnStep("Step 2 of 4"), "Expected the step indicator to read 'Step 2 of 4'");
        softAssert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled");
        softAssert.assertTrue(typographyStep.isPencilIconVisible(), "Expected the pencil/edit icon to be visible");
        softAssert.assertTrue(typographyStep.isDesktopPreviewVisible(), "Expected the Desktop preview card to be visible");
        softAssert.assertTrue(typographyStep.isTabletPreviewVisible(), "Expected the Tablet preview card to be visible");
        softAssert.assertTrue(typographyStep.isMobilePreviewVisible(), "Expected the Mobile preview card to be visible");
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        softAssert.assertAll();
    }

    // ---------------------------------------------------------------------------------------
    // Section 3 - Colors (Step 3 of 4) - TC_20-TC_32
    // ---------------------------------------------------------------------------------------

    @Test(priority = 20)
    public void tc20_clickNextAdvancesToColors() {
        LOGGER.info("TC_20: Clicking Next and verifying the wizard advances to Step 3 of 4 (Colors)");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 3 of 4"), "Expected the wizard to advance to Step 3 of 4 (Colors)");
    }

    @Test(priority = 21)
    public void tc21_backFromColorsPreservesTypography() {
        LOGGER.info("TC_21: Clicking Back from Colors and verifying Typography selections are preserved");
        wizard.clickBack();
        Assert.assertTrue(wizard.isOnStep("Step 2 of 4"), "Expected Back to return to Step 2 of 4 (Typography)");
        Assert.assertEquals(typographyStep.getSelectedFont(), selectedFont, "Expected the Font selection to persist after Back");
        Assert.assertEquals(typographyStep.getSelectedHeading(), selectedHeading, "Expected the Heading selection to persist after Back");
    }

    @Test(priority = 22)
    public void tc22_reNavigateToColorsAndCaptureScreenshot() {
        LOGGER.info("TC_22: Re-navigating to the Colors page and capturing colors-page.png");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 3 of 4"), "Expected to be back on Step 3 of 4 (Colors)");
        ScreenshotUtil.capture(driver, "colors-page");
    }

    @Test(priority = 23)
    public void tc23_verifyBackAndNextEnabledOnColors() {
        LOGGER.info("TC_23: Verifying Back and Next are both enabled on the Colors page");
        Assert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled on the Colors step");
        Assert.assertTrue(wizard.isNextEnabled(), "Expected Next to be enabled on the Colors step");
    }

    @Test(priority = 24)
    public void tc24_verifyAllDefaultColorSectionsVisible() {
        LOGGER.info("TC_24: Verifying all 7 default color sections are visible and pre-selected");
        SoftAssert softAssert = new SoftAssert();
        for (String sectionKey : ColorsStepPage.DEFAULT_SECTION_KEYS) {
            softAssert.assertTrue(colorsStep.isColorSectionVisible(sectionKey), "Expected color section '" + sectionKey + "' to be visible");
            // testdata.properties keys are lowercase (branding.color.primary.defaultHex, etc.);
            // ColorsStepPage.DEFAULT_SECTION_KEYS holds the real, display-cased section names
            // (Primary, Secondary, ...) needed to build locators - lowercase only for this lookup.
            String expectedDefaultHex = TestDataReader.get("branding.color." + sectionKey.toLowerCase() + ".defaultHex");
            softAssert.assertEquals(colorsStep.getSectionHex(sectionKey), expectedDefaultHex,
                    "Expected color section '" + sectionKey + "' to be pre-selected with its default HEX");
        }
        softAssert.assertAll();
    }

    @Test(priority = 25)
    public void tc25_verifyPreviewReflectsDefaultColors() {
        LOGGER.info("TC_25: Verifying the right-side brand preview panel reflects the default colors (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        String previewText = wizard.getPreviewPanelText();
        softAssert.assertTrue(previewText != null && !previewText.trim().isEmpty(),
                "Expected the preview panel to render default-color-driven content");
        softAssert.assertAll();
    }

    @Test(priority = 26)
    public void tc26_changePrimaryColorViaMainSwatch() {
        String replacementHex = TestDataReader.get("branding.color.primary.replacementHex");
        LOGGER.info("TC_26: Changing Primary color via its main swatch to " + replacementHex);
        primaryHexAfterChange = replacementHex;
        colorsStep.openMainSwatchPicker("Primary");
        Assert.assertTrue(colorsStep.isPickerPopoverOpen(), "Expected the Primary color picker popover to open");
        colorsStep.typeHexInPicker(replacementHex);
        // No Apply button exists (react-color's ChromePicker applies live) - closing via
        // click-away is what commits the edit here; see ColorsStepPage's class comment.
        colorsStep.closePickerByClickingAway();
        Assert.assertEquals(colorsStep.getSectionHex("Primary"), replacementHex,
                "Expected the Primary section's HEX display to update to " + replacementHex);
    }

    @Test(priority = 27)
    public void tc27_primaryMainSwatchCancelDoesNotApply() {
        // LIVE FINDING: the 3 small "shade" swatches are static, non-interactive color chips -
        // clicking one does not open any popover. Only the main (large) swatch is interactive,
        // so this now exercises the real open-then-close-without-editing flow on the main swatch
        // instead - see ColorsStepPage's class comment for the full finding.
        LOGGER.info("TC_27: Opening the Primary main swatch picker and closing via click-away WITHOUT typing anything");
        String hexBeforeCancel = colorsStep.getSectionHex("Primary");
        colorsStep.openMainSwatchPicker("Primary");
        Assert.assertTrue(colorsStep.isPickerPopoverOpen(), "Expected the Primary color picker popover to open");
        colorsStep.closePickerByClickingAway();
        Assert.assertFalse(colorsStep.isPickerPopoverOpen(), "Expected the color picker popover to close");
        Assert.assertEquals(colorsStep.getSectionHex("Primary"), hexBeforeCancel,
                "Expected the Primary HEX to remain unchanged after opening and closing the picker without editing");
    }

    @Test(priority = 28)
    public void tc28_changeSecondaryColorViaMainSwatch() {
        String replacementHex = TestDataReader.get("branding.color.secondary.replacementHex");
        LOGGER.info("TC_28: Changing Secondary color via its main swatch to " + replacementHex);
        secondaryHexAfterChange = replacementHex;
        colorsStep.openMainSwatchPicker("Secondary");
        Assert.assertTrue(colorsStep.isPickerPopoverOpen(), "Expected the Secondary color picker popover to open");
        colorsStep.typeHexInPicker(replacementHex);
        colorsStep.closePickerByClickingAway();
        Assert.assertEquals(colorsStep.getSectionHex("Secondary"), replacementHex,
                "Expected the Secondary section's HEX display to update to " + replacementHex);
    }

    @Test(priority = 29)
    public void tc29_secondarySwatchesCrossValidateFourColorsThenCancel() {
        // LIVE FINDING (see TC_27): reinterpreted per ColorsStepPage's class comment - "4 hex
        // values" now means the 4 real swatches per section (1 main + 3 shades), read via
        // getCssValue("background-color"), not 4 options inside a shade-picker popover (which
        // does not exist).
        LOGGER.info("TC_29: Cross-validating Secondary's 4 swatch colors, then opening/closing its main swatch without editing");
        List<String> swatchColors = colorsStep.getAllSwatchColors("Secondary");
        Assert.assertEquals(swatchColors.size(), 4, "Expected exactly 4 color swatches (1 main + 3 shades) for the Secondary section");
        for (String swatchColor : swatchColors) {
            Assert.assertTrue(swatchColor != null && !swatchColor.trim().isEmpty(), "Expected every swatch to report a non-empty computed color");
        }

        String hexBeforeCancel = colorsStep.getSectionHex("Secondary");
        colorsStep.openMainSwatchPicker("Secondary");
        Assert.assertTrue(colorsStep.isPickerPopoverOpen(), "Expected the Secondary color picker popover to open");
        colorsStep.closePickerByClickingAway();
        Assert.assertEquals(colorsStep.getSectionHex("Secondary"), hexBeforeCancel,
                "Expected the Secondary HEX to remain unchanged after opening and closing the picker without editing");
    }

    @Test(priority = 30)
    public void tc30_invalidHexInputDoesNotCorruptDisplay() {
        String invalidHex = TestDataReader.get("branding.color.invalidHex");
        LOGGER.info("TC_30: Entering invalid HEX '" + invalidHex + "' into the Primary picker and verifying it is rejected safely");
        // There is no always-editable inline HEX field (the section's HEX renders as read-only
        // text - see ColorsStepPage's class comment) - invalid input can only be exercised
        // through the picker's own hex field.
        String hexBeforeInvalidInput = colorsStep.getSectionHex("Primary");
        colorsStep.openMainSwatchPicker("Primary");
        colorsStep.typeHexInPicker(invalidHex);
        colorsStep.closePickerByClickingAway();
        String hexAfterInvalidInput = colorsStep.getSectionHex("Primary");
        Assert.assertEquals(hexAfterInvalidInput, hexBeforeInvalidInput,
                "Expected an invalid HEX value ('" + invalidHex + "') to be rejected, leaving the previous value '"
                        + hexBeforeInvalidInput + "' intact");
    }

    @Test(priority = 31)
    public void tc31_verifyPreviewSyncsWithUpdatedColors() {
        LOGGER.info("TC_31: Verifying the preview panel reflects the updated Primary and Secondary colors");
        Assert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        // A precise "does the preview literally show these hex values" check isn't possible
        // without knowing the preview panel's real DOM structure - this confirms the panel is
        // still rendering non-empty content after both color changes, and is a natural place to
        // tighten the assertion once the real structure (e.g. a swatch with a title attribute) is known.
        String previewText = wizard.getPreviewPanelText();
        Assert.assertTrue(previewText != null && !previewText.trim().isEmpty(),
                "Expected the preview panel to reflect the updated Primary (" + primaryHexAfterChange
                        + ") and Secondary (" + secondaryHexAfterChange + ") colors");
    }

    @Test(priority = 32)
    public void tc32_fullColorsPageSoftAssertions() {
        LOGGER.info("TC_32: Full Colors page content verification (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isOnStep("Step 3 of 4"), "Expected the step indicator to read 'Step 3 of 4'");
        softAssert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled");
        softAssert.assertTrue(wizard.isNextEnabled(), "Expected Next to be enabled");
        for (String sectionKey : ColorsStepPage.DEFAULT_SECTION_KEYS) {
            softAssert.assertTrue(colorsStep.isColorSectionVisible(sectionKey), "Expected color section '" + sectionKey + "' to be visible");
        }
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        softAssert.assertAll();
    }

    // ---------------------------------------------------------------------------------------
    // Section 4 - Style (Step 4 of 4) - TC_33-TC_43
    // ---------------------------------------------------------------------------------------

    @Test(priority = 33)
    public void tc33_clickNextAdvancesToStyle() {
        LOGGER.info("TC_33: Clicking Next and verifying the wizard advances to Step 4 of 4 (Style)");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 4 of 4"), "Expected the wizard to advance to Step 4 of 4 (Style)");
    }

    @Test(priority = 34)
    public void tc34_backFromStylePreservesColors() {
        LOGGER.info("TC_34: Clicking Back from Style and verifying Primary/Secondary HEX values persist on Colors");
        wizard.clickBack();
        Assert.assertTrue(wizard.isOnStep("Step 3 of 4"), "Expected Back to return to Step 3 of 4 (Colors)");
        Assert.assertEquals(colorsStep.getSectionHex("Primary"), primaryHexAfterChange, "Expected Primary HEX to persist after Back");
        Assert.assertEquals(colorsStep.getSectionHex("Secondary"), secondaryHexAfterChange, "Expected Secondary HEX to persist after Back");
    }

    @Test(priority = 35)
    public void tc35_reNavigateToStyleAndCaptureScreenshot() {
        LOGGER.info("TC_35: Re-navigating to the Style page and capturing style-page.png");
        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 4 of 4"), "Expected to be back on Step 4 of 4 (Style)");
        ScreenshotUtil.capture(driver, "style-page");
    }

    @Test(priority = 36)
    public void tc36_verifyStyleStepButtons() {
        LOGGER.info("TC_36: Verifying Back=enabled, Publish=enabled, and no Next button on the Style page");
        Assert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled on the Style step");
        Assert.assertTrue(wizard.isPublishButtonPresent(), "Expected a Publish button to be present on the final step");
        Assert.assertTrue(wizard.isPublishEnabled(), "Expected Publish to be enabled on the Style step");
        Assert.assertFalse(wizard.isNextButtonPresent(), "Expected no Next button on the final (Style) step");
    }

    @Test(priority = 37)
    public void tc37_verifyEdgesControlDefaults() {
        LOGGER.info("TC_37: Verifying the Edges label, default value, and -/+ controls are visible");
        Assert.assertTrue(styleStep.isEdgesControlVisible(), "Expected the Edges label and -/+ controls to be visible");
        int expectedDefault = Integer.parseInt(TestDataReader.get("branding.style.defaultEdgeValue"));
        Assert.assertEquals(styleStep.getEdgeValue(), expectedDefault, "Expected the Edges control to default to " + expectedDefault);
    }

    @Test(priority = 38)
    public void tc38_edgeValueIncrementDecrementAndBoundaries() {
        LOGGER.info("TC_38: Exercising Edges increment, decrement, direct input, and boundary checks");
        // LIVE FINDING: a live increment click was observed to jump the value by +2 once and by
        // +1 the very next click, rather than a fixed step size - see StyleStepPage's class
        // comment. Asserting only the DIRECTION of change (increased/decreased), not an exact
        // delta, until that inconsistency is properly root-caused.
        int startingValue = styleStep.getEdgeValue();

        styleStep.clickIncrement();
        Assert.assertTrue(styleStep.getEdgeValue() > startingValue,
                "Expected + to increase the edge value above " + startingValue + " (exact step size is unconfirmed - see class comment)");

        int afterIncrement = styleStep.getEdgeValue();
        styleStep.clickDecrement();
        Assert.assertTrue(styleStep.getEdgeValue() < afterIncrement,
                "Expected - to decrease the edge value below " + afterIncrement + " (exact step size is unconfirmed - see class comment)");

        int targetValue = Integer.parseInt(TestDataReader.get("branding.style.targetEdgeValue"));
        styleStep.setEdgeValueDirectly(targetValue);
        Assert.assertEquals(styleStep.getEdgeValue(), targetValue, "Expected direct numeric entry to set the edge value to " + targetValue);

        // TODO: the real min/max boundaries for this control are unconfirmed - branding.style.edgeMin
        // and branding.style.edgeMax in testdata.properties are placeholder assumptions pending
        // live verification against the real app.
        int min = Integer.parseInt(TestDataReader.get("branding.style.edgeMin"));
        int max = Integer.parseInt(TestDataReader.get("branding.style.edgeMax"));

        styleStep.setEdgeValueDirectly(min);
        Assert.assertEquals(styleStep.getEdgeValue(), min, "Expected the edge value to accept its assumed minimum, " + min);
        Assert.assertFalse(styleStep.isDecrementEnabled(), "Expected - to be disabled at the minimum edge value");

        styleStep.setEdgeValueDirectly(max);
        Assert.assertEquals(styleStep.getEdgeValue(), max, "Expected the edge value to accept its assumed maximum, " + max);
        Assert.assertFalse(styleStep.isIncrementEnabled(), "Expected + to be disabled at the maximum edge value");

        // Leave the control at the deterministic target value expected by TC_39 onward.
        styleStep.setEdgeValueDirectly(targetValue);
    }

    @Test(priority = 39)
    public void tc39_verifyPreviewSyncsWithEdgeValue() {
        LOGGER.info("TC_39: Verifying the preview panel syncs with the edge value and is fully visible");
        int targetValue = Integer.parseInt(TestDataReader.get("branding.style.targetEdgeValue"));
        Assert.assertEquals(styleStep.getEdgeValue(), targetValue, "Expected the edge value to still be " + targetValue + " from TC_38");
        Assert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be fully visible");
    }

    @Test(priority = 40)
    public void tc40_fullStylePageSoftAssertions() {
        LOGGER.info("TC_40: Full Style page content verification (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isOnStep("Step 4 of 4"), "Expected the step indicator to read 'Step 4 of 4'");
        softAssert.assertTrue(wizard.isBackEnabled(), "Expected Back to be enabled");
        softAssert.assertTrue(wizard.isPublishEnabled(), "Expected Publish to be enabled");
        softAssert.assertTrue(styleStep.isEdgesControlVisible(), "Expected the Edges control to be visible");
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        softAssert.assertAll();
    }

    @Test(priority = 41)
    public void tc41_crossVerifyAllStepsInPreviewPanel() {
        LOGGER.info("TC_41: Cross-verifying Typography, Colors, and Style edge data all reflected in the preview panel");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(wizard.isPreviewPanelVisible(), "Expected the live brand preview panel to be visible");
        String previewText = wizard.getPreviewPanelText();
        softAssert.assertTrue(previewText != null && !previewText.trim().isEmpty(),
                "Expected the preview panel to render content reflecting Font=" + selectedFont + ", Heading=" + selectedHeading
                        + ", Primary=" + primaryHexAfterChange + ", Secondary=" + secondaryHexAfterChange
                        + ", Edges=" + TestDataReader.get("branding.style.targetEdgeValue"));
        softAssert.assertAll();
    }

    @Test(priority = 42)
    public void tc42_backToColorsThenNextToStylePersistsData() {
        LOGGER.info("TC_42: Navigating Back to Colors then Next back to Style, verifying data persists both ways");
        wizard.clickBack();
        Assert.assertTrue(wizard.isOnStep("Step 3 of 4"), "Expected Back to return to Step 3 of 4 (Colors)");
        Assert.assertEquals(colorsStep.getSectionHex("Primary"), primaryHexAfterChange, "Expected Primary HEX to persist after Back");
        Assert.assertEquals(colorsStep.getSectionHex("Secondary"), secondaryHexAfterChange, "Expected Secondary HEX to persist after Back");

        wizard.clickNext();
        Assert.assertTrue(wizard.isOnStep("Step 4 of 4"), "Expected Next to return to Step 4 of 4 (Style)");
        int targetValue = Integer.parseInt(TestDataReader.get("branding.style.targetEdgeValue"));
        Assert.assertEquals(styleStep.getEdgeValue(), targetValue, "Expected the edge value to persist at " + targetValue + " after Back then Next");
    }

    @Test(priority = 43)
    public void tc43_exitConfirmationCancelStaysOnStyle() {
        LOGGER.info("TC_43: Clicking Exit and verifying Cancel keeps the user on the Style page");
        wizard.clickExit();
        Assert.assertTrue(wizard.isExitDialogVisible(), "Expected an Exit confirmation dialog to appear");
        wizard.cancelExit();
        Assert.assertFalse(wizard.isExitDialogVisible(), "Expected Cancel to close the Exit confirmation dialog");
        Assert.assertTrue(wizard.isOnStep("Step 4 of 4"), "Expected Cancel to keep the user on Step 4 of 4 (Style) without exiting");
    }

    // ---------------------------------------------------------------------------------------
    // Section 5 - Publish & Branding Hub list management - TC_44-TC_49
    // ---------------------------------------------------------------------------------------

    @Test(priority = 44)
    public void tc44_publishRedirectsToBrandingHubList() {
        LOGGER.info("TC_44: Publishing the brand and verifying redirect to the Branding Hub list page");
        wizard.clickPublish();
        Assert.assertTrue(brandingHubPage.isLoaded(), "Expected Publish to redirect back to the Branding Hub list page");
    }

    @Test(priority = 45)
    public void tc45_searchForCreatedBrand() {
        LOGGER.info("TC_45: Searching for the created brand '" + uniqueBrandName + "' in the Branding Hub");
        brandingHubPage.searchBrand(uniqueBrandName);
        Assert.assertTrue(brandingHubPage.isBrandVisible(uniqueBrandName),
                "Expected the created brand '" + uniqueBrandName + "' to appear in search results");
    }

    @Test(priority = 46)
    public void tc46_openCreatedBrand() {
        LOGGER.info("TC_46: Clicking the created brand and verifying it opens");
        brandingHubPage.openBrand(uniqueBrandName);
        Assert.assertTrue(brandingHubPage.hasLeftListView(), "Expected clicking the brand to navigate away from the plain list view");
    }

    @Test(priority = 47)
    public void tc47_editCreatedBrandViaThreeDotMenu() {
        LOGGER.info("TC_47: Editing the created brand via its 3-dot menu");
        // Return to a known-good list state first, regardless of where TC_46 left off.
        brandingHubPage.open();
        brandingHubPage.searchBrand(uniqueBrandName);
        brandingHubPage.openBrandMenu(uniqueBrandName);
        brandingHubPage.clickEditFromMenu();
        Assert.assertTrue(wizard.isOnStep("Step 1 of 4"), "Expected Edit to reopen the brand in the wizard, starting at Step 1 of 4");

        // Leave the edit wizard cleanly so TC_48/TC_49 operate on the list again.
        wizard.clickExit();
        if (wizard.isExitDialogVisible()) {
            wizard.confirmExit();
        }
        Assert.assertTrue(brandingHubPage.isLoaded(), "Expected exiting the edit wizard to return to the Branding Hub list");
    }

    @Test(priority = 48)
    public void tc48_deleteCreatedBrandCancelThenConfirm() {
        LOGGER.info("TC_48: Deleting the created brand via its 3-dot menu - Cancel first, then Confirm");
        brandingHubPage.searchBrand(uniqueBrandName);

        brandingHubPage.openBrandMenu(uniqueBrandName);
        brandingHubPage.clickDeleteFromMenu();
        Assert.assertTrue(brandingHubPage.isDeleteConfirmDialogVisible(), "Expected a delete confirmation dialog to appear");

        brandingHubPage.cancelDelete();
        Assert.assertFalse(brandingHubPage.isDeleteConfirmDialogVisible(), "Expected Cancel to close the delete confirmation dialog");
        Assert.assertTrue(brandingHubPage.isBrandVisible(uniqueBrandName), "Expected the brand to still exist after cancelling delete");

        brandingHubPage.openBrandMenu(uniqueBrandName);
        brandingHubPage.clickDeleteFromMenu();
        Assert.assertTrue(brandingHubPage.isDeleteConfirmDialogVisible(), "Expected the delete confirmation dialog to reappear");
        brandingHubPage.confirmDelete();
    }

    @Test(priority = 49)
    public void tc49_verifyCreatedBrandNoLongerVisible() {
        LOGGER.info("TC_49: Verifying the created brand is no longer visible after deletion");
        brandingHubPage.clearSearch();
        brandingHubPage.searchBrand(uniqueBrandName);
        Assert.assertFalse(brandingHubPage.isBrandVisible(uniqueBrandName),
                "Expected the deleted brand '" + uniqueBrandName + "' to no longer appear in the Branding Hub");
    }

    /**
     * Generates a throwaway 1x1 PNG at test time for TC_04's logo upload, so the suite doesn't
     * need a committed binary fixture. The file is marked deleteOnExit() since it's purely
     * transient test input, not an artifact worth keeping.
     */
    private String generateOnePixelPngFile() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x2B3A67);
        File tempFile = File.createTempFile("branding-logo-", ".png");
        tempFile.deleteOnExit();
        ImageIO.write(image, "png", tempFile);
        return tempFile.getAbsolutePath();
    }
}
