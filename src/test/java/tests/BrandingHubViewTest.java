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
import pages.branding.BrandingHubPage;
import pages.common.SidebarPage;
import utils.TestLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Ordered, single-browser-session coverage of the Branding Hub list/landing page itself: sidebar
 * navigation into it, its page-level UI (pagination, items-per-page, first-item interactions),
 * and its Grid view / List view toggle - implementing TC_01-TC_16. Does NOT create, edit, or
 * delete a brand (that's BrandingHubTest's job); this suite assumes at least one brand already
 * exists, which is true for the live account used here (179 brands at last count).
 *
 * SESSION MODEL: like BrandingHubTest, shares one WebDriver for the whole run
 * (BaseTest's @BeforeClass/@AfterClass) and uses @Test(priority = n) rather than
 * dependsOnMethods, so TestNG always attempts every method in order even if an earlier one fails.
 *
 * REAL BEHAVIOR FINDINGS, verified live on 2026-08-12 against the real app - see
 * SidebarPage's and BrandingHubPage's own class comments for the full detail on each:
 *  - Clicking "Settings" in the sidebar does not navigate; it expands an inline sub-panel
 *    revealing "Preferences" and, under it, "Branding Hub" - only that link actually navigates.
 *  - Pagination and "Items per page" exist ONLY in List view - Grid view renders every brand at
 *    once with no pagination at all. TC_05/TC_06 therefore check via a temporary switch to List
 *    view, restoring Grid afterward so TC_09/TC_10 still find their expected default.
 *  - Clicking a brand (TC_07) reopens the creation wizard in edit mode, not a distinct detail
 *    page or generic modal - confirmed by the wizard's "Step 1 of 4" indicator appearing with no
 *    URL change. hasOpenedDetailOrModal() still checks both possibilities generically per the
 *    original requirement, but this is the confirmed real outcome.
 *  - TC_13's cross-view data check: since Grid has no pagination while List paginates at 10/page,
 *    the two views never show the same-sized subset by default - see TC_13 for the matching-size
 *    subset workaround, and BrandingHubPage's class comment for the underlying finding.
 *  - TC_14: no card/row exposes an actual logo/avatar image/placeholder at all (most of this
 *    account's test brands never had one uploaded) - adapted to check for non-empty, non-broken
 *    text content instead of a visual placeholder that doesn't exist.
 */
public class BrandingHubViewTest extends BaseTest {

    private static final Logger LOGGER = TestLogger.getLogger(BrandingHubViewTest.class);

    private SidebarPage sidebarPage;
    private BrandingHubPage brandingHubPage;

    // Baseline captured in TC_10 (first confirmed default-Grid observation) and compared against
    // in TC_16, to verify Grid view is genuinely restored - not just "some" data - after the
    // Grid -> List -> Grid round trip exercised by TC_11-TC_15.
    private Set<String> originalGridBrandNames;

    @BeforeClass
    public void login() {
        LOGGER.info("Logging in via the existing LoginPage flow before running Branding Hub view scenarios");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.getValidEmail(), ConfigReader.getValidPassword());

        DashboardPage dashboardPage = new DashboardPage(driver);
        if (!dashboardPage.isDashboardLoaded()) {
            throw new IllegalStateException(
                    "Login did not succeed before the Branding Hub view suite could start; see LoginTest for login diagnostics.");
        }

        sidebarPage = new SidebarPage(driver);
        brandingHubPage = new BrandingHubPage(driver);
    }

    @Test(priority = 1)
    public void tc01_verifyAgencyName() {
        LOGGER.info("TC_01: Verifying the Agency Name matches configured test data");
        Assert.assertEquals(sidebarPage.getAgencyName(), TestDataReader.get("branding.agencyName"),
                "Unexpected Agency Name displayed in the top bar");
    }

    @Test(priority = 2)
    public void tc02_clickSettingsAndWaitForPanel() {
        LOGGER.info("TC_02: Clicking Settings in the sidebar and waiting for the settings panel");
        sidebarPage.clickSettings();
        Assert.assertTrue(sidebarPage.isPreferencesVisible(),
                "Expected the '" + TestDataReader.get("branding.sidebar.preferencesLabel") + "' section to become visible after clicking Settings");
    }

    @Test(priority = 3)
    public void tc03_verifyAndClickBrandingHub() {
        LOGGER.info("TC_03: Verifying '" + TestDataReader.get("branding.sidebar.brandingHubLabel") + "' is visible under Preferences and clicking it");
        Assert.assertTrue(sidebarPage.isBrandingHubLinkVisible(),
                "Expected '" + TestDataReader.get("branding.sidebar.brandingHubLabel") + "' to be visible under Preferences");
        sidebarPage.clickBrandingHub();
        Assert.assertTrue(brandingHubPage.isLoaded(), "Expected the Branding Hub list page to load after clicking the sidebar link");
    }

    @Test(priority = 4)
    public void tc04_verifyAllRequiredPageElements() {
        LOGGER.info("TC_04: Verifying all required page elements are visible on the Branding Hub page (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(brandingHubPage.isLoaded(), "Expected the '+ Brand' button to be visible");
        softAssert.assertTrue(brandingHubPage.isGridViewToggleVisible(), "Expected the Grid view toggle icon to be visible");
        softAssert.assertTrue(brandingHubPage.isListViewToggleVisible(), "Expected the List view toggle icon to be visible");
        softAssert.assertFalse(brandingHubPage.getFirstItemName().trim().isEmpty(), "Expected at least one brand to be visible");
        softAssert.assertAll();
    }

    @Test(priority = 5)
    public void tc05_verifyPaginationControlsPresent() {
        // LIVE FINDING: pagination exists ONLY in List view - Grid (the default) renders every
        // brand at once with no pagination at all. Checking via a temporary List view visit,
        // then restoring Grid so TC_09/TC_10 still find their expected default afterward.
        LOGGER.info("TC_05: Verifying pagination controls are present (checked via List view - see class comment)");
        brandingHubPage.switchToListView();
        Assert.assertTrue(brandingHubPage.isPaginationVisible(), "Expected pagination controls (previous/next) to be visible in List view");
        brandingHubPage.switchToGridView();
    }

    @Test(priority = 6)
    public void tc06_verifyItemsPerPageSelectorVisible() {
        LOGGER.info("TC_06: Verifying the 'Items per page' selector is visible (checked via List view - see TC_05)");
        brandingHubPage.switchToListView();
        Assert.assertTrue(brandingHubPage.isItemsPerPageSelectorVisible(), "Expected the 'Items per page' selector to be visible in List view");
        brandingHubPage.switchToGridView();
    }

    @Test(priority = 7)
    public void tc07_firstItemClickableOpensDetailOrModal() {
        LOGGER.info("TC_07: Verifying the first Branding Hub item is clickable and opens a detail view or modal");
        String urlBeforeClick = driver.getCurrentUrl();
        brandingHubPage.clickFirstItem();
        Assert.assertTrue(brandingHubPage.hasOpenedDetailOrModal(urlBeforeClick),
                "Expected clicking the first item to either change the URL or open a modal/wizard overlay");
        // Confirmed live this reopens the creation wizard in edit mode - exit cleanly (discarding
        // any state, since we made no edits) so later tests operate on the plain list page again.
        brandingHubPage.returnToListIfWizardOpened();
    }

    @Test(priority = 8)
    public void tc08_verifyThreeDotMenuShowsEditAndDelete() {
        LOGGER.info("TC_08: Verifying the 3-dot context menu shows Edit and Delete (structural check only)");
        brandingHubPage.openFirstItemMenu();
        Assert.assertTrue(brandingHubPage.isEditMenuItemVisible(), "Expected the 3-dot menu to show an 'Edit' option");
        Assert.assertTrue(brandingHubPage.isDeleteMenuItemVisible(), "Expected the 3-dot menu to show a 'Delete' option");
        brandingHubPage.closeOpenMenu();
    }

    @Test(priority = 9)
    public void tc09_verifyGridAndListToggleIconsPresent() {
        LOGGER.info("TC_09: Verifying Grid and List view toggle icons are present");
        Assert.assertTrue(brandingHubPage.isGridViewToggleVisible(), "Expected the Grid view toggle icon to be visible");
        Assert.assertTrue(brandingHubPage.isListViewToggleVisible(), "Expected the List view toggle icon to be visible");
    }

    @Test(priority = 10)
    public void tc10_verifyDefaultViewIsGrid() {
        LOGGER.info("TC_10: Verifying the default view is Grid (cards visible, no table)");
        Assert.assertTrue(brandingHubPage.isGridViewActive(), "Expected Grid to be the default view");
        Assert.assertFalse(brandingHubPage.isListTableVisible(), "Expected no table to be present in the default Grid view");

        originalGridBrandNames = new HashSet<>(brandingHubPage.getVisibleBrandNames());
        Assert.assertFalse(originalGridBrandNames.isEmpty(), "Expected at least one brand name to be visible in the default Grid view");
    }

    @Test(priority = 11)
    public void tc11_clickListToggleAndVerifySwitch() {
        LOGGER.info("TC_11: Clicking the List view toggle and verifying the view switches");
        brandingHubPage.switchToListView();
        Assert.assertTrue(brandingHubPage.isListViewActive(), "Expected the view to switch to List after clicking its toggle");
    }

    @Test(priority = 12)
    public void tc12_verifyListViewTableHeadersAndPagination() {
        LOGGER.info("TC_12: Verifying List view shows a table, column headers, and pagination");
        Assert.assertTrue(brandingHubPage.isListTableVisible(), "Expected a table to be visible in List view");

        List<String> expectedHeaders = Arrays.asList(TestDataReader.get("branding.listView.columnHeaders").split(","));
        List<String> actualHeaders = brandingHubPage.getTableColumnHeaders();
        Assert.assertEquals(actualHeaders.subList(0, Math.min(expectedHeaders.size(), actualHeaders.size())), expectedHeaders,
                "Unexpected table column headers in List view");

        Assert.assertTrue(brandingHubPage.isPaginationVisible(), "Expected pagination controls to be visible in List view");
    }

    @Test(priority = 13)
    public void tc13_verifyBrandNamesConsistentBetweenGridAndList() {
        // TODO: confirm pagination behaves identically across views - verified live that Grid
        // renders all 179 brands unpaginated while List paginates at 10/page, so a straight
        // full-list comparison would never match regardless of correctness. Comparing only over
        // a matching-size subset (List's current page vs. the same number of Grid entries) as a
        // pragmatic workaround - see BrandingHubPage's class comment for the underlying finding.
        LOGGER.info("TC_13: Verifying brand names are consistent between Grid and List view (matching-size subset, order-independent)");
        List<String> listNames = brandingHubPage.getVisibleBrandNames();

        brandingHubPage.switchToGridView();
        List<String> gridNames = brandingHubPage.getVisibleBrandNames();
        Set<String> gridSubset = new HashSet<>(gridNames.subList(0, Math.min(listNames.size(), gridNames.size())));
        brandingHubPage.switchToListView();

        Assert.assertEquals(gridSubset, new HashSet<>(listNames),
                "Expected the same brands (by name) to appear in Grid and List view over a matching-size subset");
    }

    @Test(priority = 14)
    public void tc14_verifyNoBrokenDataInListView() {
        // LIVE FINDING: no card/row exposes an actual logo/avatar image or placeholder at all
        // (see BrandingHubPage's class comment) - most test brands in this account never had one
        // uploaded, and the app renders nothing in its place rather than a placeholder graphic.
        // Checking for non-empty, non-broken text content instead of a visual slot that doesn't exist.
        LOGGER.info("TC_14: Verifying no broken data or UI elements in List view (soft assertions)");
        SoftAssert softAssert = new SoftAssert();
        List<String> names = brandingHubPage.getVisibleBrandNames();
        softAssert.assertFalse(names.isEmpty(), "Expected at least one row to be visible in List view");
        for (String name : names) {
            softAssert.assertTrue(name != null && !name.trim().isEmpty(), "Expected every row's brand name to be non-empty");
            softAssert.assertFalse(containsBrokenDataMarker(name), "Found a broken-data marker in a brand name: '" + name + "'");
        }
        softAssert.assertAll();
    }

    @Test(priority = 15)
    public void tc15_clickGridToggleAndVerifySwitchesBack() {
        LOGGER.info("TC_15: Clicking the Grid view toggle and verifying the view switches back");
        brandingHubPage.switchToGridView();
        Assert.assertTrue(brandingHubPage.isGridViewActive(), "Expected the view to switch back to Grid after clicking its toggle");
    }

    @Test(priority = 16)
    public void tc16_verifyGridViewFullyRestoredWithCorrectData() {
        LOGGER.info("TC_16: Verifying Grid view is fully restored with the same data it had before toggling to List");
        Set<String> currentGridBrandNames = new HashSet<>(brandingHubPage.getVisibleBrandNames());
        Assert.assertEquals(currentGridBrandNames, originalGridBrandNames,
                "Expected Grid view to show exactly the same brands after round-tripping through List view");
    }

    /** A common real-world sign of a broken data binding: a literal placeholder string instead of real data. */
    private boolean containsBrokenDataMarker(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase();
        return normalized.equals("undefined") || normalized.equals("null") || normalized.equals("nan");
    }
}
