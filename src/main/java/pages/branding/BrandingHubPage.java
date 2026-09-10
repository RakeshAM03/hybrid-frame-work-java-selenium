package pages.branding;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.BasePage;

import java.util.ArrayList;
import java.util.List;

/**
 * Page object for the Branding Hub list page (config's {@code baseUrl}) - the screen showing all
 * existing brands, with search, a "+ Brand" button to launch the creation wizard, a per-brand
 * 3-dot menu (Edit / Delete) with confirmation dialogs, and a Grid view / List view toggle.
 *
 * LOCATORS VERIFIED LIVE on 2026-08-11/12 against the real Branding Hub (accessible via the same
 * authorized test account used by LoginTest). None of the brand cards, search box, "+ Brand"
 * button, or view-toggle icons expose a data-testid/id/aria-label - the app relies entirely on
 * plain MUI components with visible text or icon class as the only identifying signal, so
 * locators here are text/icon-class-anchored rather than attribute-anchored.
 *
 * REAL BEHAVIOR FINDINGS relevant to the Grid/List view suite (BrandingHubViewTest):
 *  - Grid view renders EVERY brand at once (179 in this account) with NO pagination and NO
 *    "Items per page" control at all - both of those exist ONLY in List view, which paginates at
 *    10 rows/page. Any "pagination is present" check must be done in List view.
 *  - List view renders a real HTML &lt;table&gt; with columns Name / Info / Last Activity /
 *    Created At (plus an unlabeled actions column for the 3-dot menu).
 *  - Clicking a brand's name (in either view) reopens the SAME 4-step creation wizard, now
 *    pre-filled for editing (confirmed live: the wizard's "Step 1 of 4" indicator appears, with
 *    no URL change) - it is neither a distinct "detail" page nor a generic modal.
 *  - No card/row exposes an actual logo/avatar image or placeholder &lt;img&gt; - most test
 *    brands never had a logo uploaded, and the app renders nothing in its place rather than a
 *    placeholder graphic.
 */
public class BrandingHubPage extends BasePage {

    // Verified live: real placeholder is exactly "Search Brand".
    private static final By SEARCH_INPUT = By.cssSelector("input[placeholder='Search Brand']");

    // Verified live: the button's own visible text is exactly "Brand" - the "+" is a decorative
    // lucide "plus" SVG icon, not part of the text at all. The original guess required BOTH a "+"
    // AND "Brand" text node, which never matched - this was the exact locator that caused the
    // first live run's @BeforeClass failure (see BrandingHubTest history).
    private static final By ADD_BRAND_BUTTON = By.xpath("//button[normalize-space(text())='Brand']");

    // Verified live: MUI Menu items render as <li role="menuitem"> with exactly this text - the
    // original guess was already correct.
    private static final By EDIT_MENU_ITEM = By.xpath("//li[@role='menuitem'][normalize-space(text())='Edit']");

    private static final By DELETE_MENU_ITEM = By.xpath("//li[@role='menuitem'][normalize-space(text())='Delete']");

    // Verified live: standard MUI dialog role - the original guess was already correct.
    private static final By DELETE_CONFIRM_DIALOG = By.xpath("//div[@role='dialog']");

    // Verified live: the confirm button's real text is "Confirm", not "Delete" as originally guessed.
    private static final By DELETE_CONFIRM_BUTTON =
            By.xpath("//div[@role='dialog']//button[normalize-space(text())='Confirm']");

    private static final By DELETE_CANCEL_BUTTON =
            By.xpath("//div[@role='dialog']//button[normalize-space(text())='Cancel']");

    // Verified live: the page's own content heading, a plain <h6> - distinct from the sidebar's
    // "Branding Hub" <p> link (see SidebarPage). Used as a neutral click-away target to close an
    // open 3-dot menu without triggering Edit or Delete.
    private static final By PAGE_HEADING = By.xpath("//h6[normalize-space(text())='Branding Hub']");

    // Verified live: bare IconButtons with no aria-label - identified by their lucide icon class.
    private static final By GRID_VIEW_TOGGLE = By.xpath("//button[.//*[contains(@class,'lucide-layout-grid')]]");

    private static final By LIST_VIEW_TOGGLE = By.xpath("//button[.//*[contains(@class,'lucide-list')]]");

    // Verified live: List view renders a real <table>; Grid view has no table at all - this
    // doubles as the view-state discriminator used by isListViewActive()/getVisibleBrandNames().
    private static final By LIST_TABLE = By.tagName("table");

    private static final By TABLE_COLUMN_HEADERS = By.cssSelector("table thead th");

    private static final By TABLE_ROW_NAME_CELLS = By.cssSelector("table tbody tr td:first-child");

    private static final By FIRST_TABLE_ROW_NAME_CELL = By.cssSelector("table tbody tr:first-child td:first-child");

    // Verified live: pagination/"Items per page" exist ONLY in List view (see class-level
    // comment) - anchored on the label text since the MUI Select trigger itself has no id.
    private static final By ITEMS_PER_PAGE_LABEL = By.xpath("//*[normalize-space(text())='Items per page:']");

    private static final By ITEMS_PER_PAGE_SELECTOR =
            By.xpath("//*[normalize-space(text())='Items per page:']/following::*[@role='combobox'][1]");

    private static final By PAGINATION_PREVIOUS_BUTTON = By.xpath("//button[@aria-label='previous page']");

    private static final By PAGINATION_NEXT_BUTTON = By.xpath("//button[@aria-label='next page']");

    // Verified live: renders e.g. "1-10 of 179", immediately after the items-per-page control.
    private static final By PAGINATION_RANGE_TEXT =
            By.xpath("//*[normalize-space(text())='Items per page:']/following::*[contains(text(),' of ')][1]");

    // Verified live: each Grid card's kebab icon lives 3 ancestors above the "more options"
    // button, in a container whose FIRST descendant <p> (in document order) is that card's own
    // name - confirmed by manual DOM walk (see BrandingHubViewTest's investigation notes). Using
    // ancestor::*[3] rather than a direct-parent relationship because the name and the kebab
    // button are NOT siblings here (unlike the single-brand-lookup locators above, which target a
    // brand by name and don't need this positional relationship).
    private static final By GRID_CARD_KEBAB_BUTTONS =
            By.xpath("//button[.//*[contains(@class,'lucide-ellipsis-vertical')]]");

    private static final By GRID_CARD_NAMES =
            By.xpath("//button[.//*[contains(@class,'lucide-ellipsis-vertical')]]/ancestor::*[3]//p[1]");

    private static final By FIRST_GRID_CARD_NAME =
            By.xpath("(//button[.//*[contains(@class,'lucide-ellipsis-vertical')]]/ancestor::*[3]//p[1])[1]");

    private static final By FIRST_GRID_CARD_KEBAB =
            By.xpath("(//button[.//*[contains(@class,'lucide-ellipsis-vertical')]])[1]");

    public BrandingHubPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    /** Navigates directly to the Branding Hub list. Only valid once already authenticated - see BrandingHubTest. */
    public BrandingHubPage open() {
        driver.get(ConfigReader.getBaseUrl());
        wait.until(ExpectedConditions.visibilityOfElementLocated(ADD_BRAND_BUTTON));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(ADD_BRAND_BUTTON);
    }

    /**
     * Best-effort, structure-agnostic check that clicking a brand navigated away from the plain
     * list view into some other view (detail page, edit wizard, etc.) - deliberately doesn't
     * assert what that other view specifically looks like, since that's unconfirmed.
     */
    public boolean hasLeftListView() {
        return !isDisplayed(ADD_BRAND_BUTTON);
    }

    public void clickAddBrand() {
        click(ADD_BRAND_BUTTON);
    }

    public void searchBrand(String brandName) {
        type(SEARCH_INPUT, brandName);
    }

    public void clearSearch() {
        type(SEARCH_INPUT, "");
    }

    public boolean isBrandVisible(String brandName) {
        return isDisplayed(brandCardLocator(brandName));
    }

    public void openBrand(String brandName) {
        click(brandCardLinkLocator(brandName));
    }

    public void openBrandMenu(String brandName) {
        click(brandCardMenuButtonLocator(brandName));
    }

    public void clickEditFromMenu() {
        click(EDIT_MENU_ITEM);
    }

    public void clickDeleteFromMenu() {
        click(DELETE_MENU_ITEM);
    }

    public boolean isDeleteConfirmDialogVisible() {
        return isDisplayed(DELETE_CONFIRM_DIALOG);
    }

    public void confirmDelete() {
        click(DELETE_CONFIRM_BUTTON);
    }

    public void cancelDelete() {
        click(DELETE_CANCEL_BUTTON);
    }

    /** Closes an open 3-dot menu via a neutral click-away, without triggering Edit or Delete. */
    public void closeOpenMenu() {
        click(PAGE_HEADING);
    }

    public boolean isGridViewToggleVisible() {
        return isDisplayed(GRID_VIEW_TOGGLE);
    }

    public boolean isListViewToggleVisible() {
        return isDisplayed(LIST_VIEW_TOGGLE);
    }

    public void switchToGridView() {
        click(GRID_VIEW_TOGGLE);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LIST_TABLE));
    }

    public void switchToListView() {
        click(LIST_VIEW_TOGGLE);
        wait.until(ExpectedConditions.visibilityOfElementLocated(LIST_TABLE));
    }

    /** Verified live: List view renders a real &lt;table&gt;; Grid view never does. */
    public boolean isListViewActive() {
        return isElementPresent(LIST_TABLE);
    }

    public boolean isGridViewActive() {
        return !isListViewActive();
    }

    /**
     * Pagination and the "Items per page" selector exist ONLY in List view (verified live - see
     * class-level comment) - both checks below will correctly report false while in Grid view.
     */
    public boolean isPaginationVisible() {
        return isDisplayed(PAGINATION_PREVIOUS_BUTTON) && isDisplayed(PAGINATION_NEXT_BUTTON);
    }

    public boolean isItemsPerPageSelectorVisible() {
        return isDisplayed(ITEMS_PER_PAGE_LABEL) && isDisplayed(ITEMS_PER_PAGE_SELECTOR);
    }

    public String getPaginationRangeText() {
        return getText(PAGINATION_RANGE_TEXT);
    }

    public boolean isListTableVisible() {
        return isDisplayed(LIST_TABLE);
    }

    public List<String> getTableColumnHeaders() {
        return elementTexts(TABLE_COLUMN_HEADERS);
    }

    /**
     * Returns the brand names currently visible, reading from whichever view is active - Grid
     * (all cards, unpaginated) or List (only the current page's rows). Callers comparing across
     * views (e.g. TC_13) must account for Grid having no pagination while List does - see
     * BrandingHubViewTest's class-level comment.
     */
    public List<String> getVisibleBrandNames() {
        return isListViewActive() ? elementTexts(TABLE_ROW_NAME_CELLS) : elementTexts(GRID_CARD_NAMES);
    }

    public String getFirstItemName() {
        return isListViewActive() ? getText(FIRST_TABLE_ROW_NAME_CELL) : getText(FIRST_GRID_CARD_NAME);
    }

    public void clickFirstItem() {
        click(isListViewActive() ? FIRST_TABLE_ROW_NAME_CELL : FIRST_GRID_CARD_NAME);
    }

    public void openFirstItemMenu() {
        click(isListViewActive() ? By.cssSelector("table tbody tr:first-child button") : FIRST_GRID_CARD_KEBAB);
    }

    public boolean isEditMenuItemVisible() {
        return isDisplayed(EDIT_MENU_ITEM);
    }

    public boolean isDeleteMenuItemVisible() {
        return isDisplayed(DELETE_MENU_ITEM);
    }

    /**
     * TODO: confirm expected behavior - live testing found clicking a brand opens the same
     * creation wizard in edit mode (Step 1 of 4 appears, no URL change), but this checks BOTH a
     * URL change and a modal/wizard indicator so the assertion holds regardless of which occurs,
     * per the original requirement not to hardcode one assumption. Uses normalize-space(.)
     * (whole subtree string-value), not normalize-space(text()) - the latter only examines an
     * element's FIRST direct text node when a step number is interpolated among several text
     * nodes, which silently breaks a "Step X of 4" contains-check exactly like this one.
     */
    public boolean hasOpenedDetailOrModal(String urlBeforeClick) {
        boolean urlChanged = !driver.getCurrentUrl().equals(urlBeforeClick);
        boolean modalOrWizardVisible = isDisplayed(By.xpath("//div[@role='dialog']"))
                || isDisplayed(By.xpath("//*[contains(normalize-space(.),'Step') and contains(normalize-space(.),'of 4')]"));
        return urlChanged || modalOrWizardVisible;
    }

    /**
     * If clicking an item opened the creation wizard (confirmed live behavior - see class
     * comment), exits it via Exit + Confirm so later tests operate on the plain list page again.
     * No-op if neither the wizard nor a dialog is currently open. Uses normalize-space(.) rather
     * than normalize-space(text()) for the step-indicator check - see hasOpenedDetailOrModal()'s
     * comment for why that distinction matters here.
     */
    public void returnToListIfWizardOpened() {
        By stepIndicator = By.xpath("//*[contains(normalize-space(.),'Step') and contains(normalize-space(.),'of 4')]");
        if (!isDisplayed(stepIndicator)) {
            return;
        }

        click(By.xpath("//button[normalize-space(text())='Exit' or .//*[normalize-space(text())='Exit']]"));

        By exitDialog = By.xpath("//div[@role='dialog']");
        if (isDisplayed(exitDialog)) {
            click(By.xpath("//div[@role='dialog']//button[normalize-space(text())='Confirm']"));
        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(stepIndicator));
    }

    private List<String> elementTexts(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        List<String> texts = new ArrayList<>();
        for (WebElement element : elements) {
            texts.add(element.getText());
        }
        return texts;
    }

    /**
     * Verified live: each brand's name renders as a plain, unattributed &lt;p&gt; (no
     * data-testid, no wrapping card element with any identifying attribute at all) - matching on
     * its exact text is the only practical anchor. This same locator doubles as both the
     * "is it visible" check and the click-to-open target (see brandCardLinkLocator()) since
     * they're literally the same element.
     */
    private static By brandCardLocator(String brandName) {
        return By.xpath("//p[normalize-space(text())=\"" + brandName + "\"]");
    }

    /**
     * Verified live: clicking the name &lt;p&gt; itself works to open the brand - the click event
     * bubbles up to whichever ancestor actually holds the "open brand" handler, so targeting the
     * text node directly is reliable and doesn't depend on knowing that ancestor's structure.
     */
    private static By brandCardLinkLocator(String brandName) {
        return brandCardLocator(brandName);
    }

    /**
     * Verified live: the 3-dot menu button is a sibling of the name &lt;p&gt; within its
     * immediate parent, and its icon carries the stable, semantic lucide class
     * "lucide-ellipsis-vertical" (part of the icon library's naming, not a build-hashed class) -
     * used here instead of a bare "//button" match to avoid ever matching some other future
     * button added to the same row.
     */
    private static By brandCardMenuButtonLocator(String brandName) {
        return By.xpath("//p[normalize-space(text())=\"" + brandName
                + "\"]/parent::*//button[.//*[contains(@class,'lucide-ellipsis-vertical')]]");
    }
}
