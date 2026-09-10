package utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Allows exactly one automatic re-run of a failed test, as a safety net against transient/
 * environment issues (e.g. a momentary network blip). This is not a substitute for proper
 * explicit waits - it only masks true one-off flakiness, not systemic timing bugs.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY_COUNT = 1;

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            return true;
        }
        return false;
    }
}
