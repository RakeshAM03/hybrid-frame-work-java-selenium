package tests;

import base.BaseTest;
import config.ConfigReader;
import config.TestDataReader;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.LoginPage;
import utils.RetryAnalyzer;
import utils.TestLogger;

import java.util.logging.Logger;

/**
 * All 7 login scenarios run in ONE browser session against ONE page load. BaseTest's
 * @BeforeClass/@AfterClass create and quit the single shared WebDriver for this whole class;
 * openLoginPageOnce() below (also @BeforeClass, guaranteed by TestNG to run after BaseTest's)
 * performs the single navigation. Between scenarios, testLogin() calls
 * LoginPage.resetLoginForm() instead of reloading the page.
 *
 * TestNG invokes @DataProvider rows sequentially in the order returned (no parallelism is
 * configured), and reuses a single instance of this class across all 7 invocations by default -
 * both are required for the scenario ordering and the shared attemptNumber/loginPage state below
 * to be correct. Scenario 7 (valid + valid) is deliberately last: it navigates away from the
 * login page on success, so nothing may run after it in this session.
 */
public class LoginTest extends BaseTest {

    private static final Logger LOGGER = TestLogger.getLogger(LoginTest.class);

    private LoginPage loginPage;

    // Tracks how many scenarios have run so far in this shared session, so every attempt except
    // the first can reset the form in place first. A mid-suite retry (RetryAnalyzer) also
    // increments this and re-triggers a reset before the retry - harmless, since resetLoginForm()
    // is idempotent and a clean slate before a retry is correct too.
    private int attemptNumber = 0;

    @BeforeClass
    public void openLoginPageOnce() {
        loginPage = new LoginPage(driver);
        LOGGER.info("Navigating to the login page ONCE for the entire scenario suite");
        loginPage.open();
    }

    @DataProvider(name = "loginScenarios")
    public Object[][] loginScenarios() {
        String validEmail = ConfigReader.getValidEmail();
        String validPassword = ConfigReader.getValidPassword();

        String invalidPassword = TestDataReader.get("invalid.password");
        String unregisteredEmail = TestDataReader.get("unregistered.email");
        String malformedEmail = TestDataReader.get("malformed.email");

        String genericLoginError = TestDataReader.get("error.genericLogin");
        String emailFormatError = TestDataReader.get("error.emailFormat");
        String emailRequiredError = TestDataReader.get("error.emailRequired");
        String passwordRequiredError = TestDataReader.get("error.passwordRequired");

        // Verified live against the real app (see LoginPage's class-level comment): once both
        // fields have already been touched earlier in this SAME session - which is now always
        // true for scenario 6, since scenarios 1-5 ran first against the one shared page load -
        // clearing them back to empty and submitting shows BOTH field-required messages
        // together, not the generic banner used in scenarios 1/2. Built from the same two
        // properties already used by scenarios 4 and 5, joined with LoginPage's own separator,
        // so this is not a new hardcoded literal.
        String bothFieldsRequiredError = emailRequiredError + LoginPage.ERROR_MESSAGE_SEPARATOR + passwordRequiredError;

        return new Object[][]{
                // scenario, email, password, expectSuccess, expectedErrorMessage
                {"1. Valid email + invalid password", validEmail, invalidPassword, false, genericLoginError},
                {"2. Invalid/unregistered email + valid password", unregisteredEmail, validPassword, false, genericLoginError},
                {"3. Invalid email format + any password", malformedEmail, validPassword, false, emailFormatError},
                {"4. Empty email + valid password", "", validPassword, false, emailRequiredError},
                {"5. Valid email + empty password", validEmail, "", false, passwordRequiredError},
                {"6. Empty email + empty password", "", "", false, bothFieldsRequiredError},
                {"7. Valid email + valid password", validEmail, validPassword, true, null}
        };
    }

    @Test(dataProvider = "loginScenarios", retryAnalyzer = RetryAnalyzer.class)
    public void testLogin(String scenario, String email, String password, boolean expectSuccess, String expectedErrorMessage) {
        attemptNumber++;
        LOGGER.info("Running scenario " + attemptNumber + ": " + scenario);

        if (attemptNumber > 1) {
            LOGGER.info("Step: resetting the login form in place (no reload) before this attempt");
            loginPage.resetLoginForm();
        }

        LOGGER.info("Step: submitting credentials");
        loginPage.login(email, password);

        if (expectSuccess) {
            LOGGER.info("Step: verifying successful login lands on the authenticated dashboard");
            DashboardPage dashboardPage = new DashboardPage(driver);
            Assert.assertTrue(dashboardPage.isDashboardLoaded(),
                    "Expected user to land on the authenticated dashboard after a valid login. Current URL: "
                            + dashboardPage.getCurrentUrl());
        } else {
            LOGGER.info("Step: verifying a login/validation error is displayed");
            Assert.assertTrue(loginPage.isLoginErrorDisplayed(),
                    "Expected a login/validation error to be displayed for scenario: " + scenario);

            LOGGER.info("Step: verifying the error message content");
            String actualErrorMessage = loginPage.getErrorMessage();
            Assert.assertEquals(actualErrorMessage, expectedErrorMessage,
                    "Unexpected error message for scenario: " + scenario);
        }

        LOGGER.info("Scenario completed: " + scenario);
    }
}
