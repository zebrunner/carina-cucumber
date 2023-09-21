package com.zebrunner.carina.cucumber;

import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.testng.ITestResult;

import com.zebrunner.agent.testng.core.testname.TestNameResolver;

import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;

public class CucumberNameResolver implements TestNameResolver {
    private static final String STR_FORMAT_TEST_NAME = "%s (%s)";
    private static final String TEST_NAME_FORMAT = " S%02dL%04d";
    private static final String FEATURE_NAME_OPTIONAL = "Optional";
    static final String SCENARIO_COUNT_PARAMETER = "scenario_count";

    @Override
    @SuppressWarnings({ "unlikely-arg-type" })
    public String resolve(ITestResult result) {
        if (result.getTestContext() == null) {
            throw new IllegalArgumentException("Unable to set test name without testContext!");
        }

        Optional<PickleWrapper> pickleWrapper = Arrays.stream(result.getParameters())
                .filter(PickleWrapper.class::isInstance)
                .findAny()
                .map(p -> (PickleWrapper) p);

        Optional<FeatureWrapper> featureWrapper = Arrays.stream(result.getParameters())
                .filter(FeatureWrapper.class::isInstance)
                .findAny()
                .map(p -> (FeatureWrapper) p);

        if (pickleWrapper.isEmpty() || featureWrapper.isEmpty()) {
            return result.getTestContext().getCurrentXmlTest().getName();
        }

        return generateTestName(pickleWrapper.get(), featureWrapper.get(),
                (int) result.getTestContext().getAttribute(SCENARIO_COUNT_PARAMETER));
    }

    static String generateTestName(PickleWrapper pickleWrapper, FeatureWrapper featureWrapper, int amountOfTests) {
        String testName = CucumberNameResolver.prepareTestName(STR_FORMAT_TEST_NAME, pickleWrapper, featureWrapper);
        if (amountOfTests > 1) {
            testName = testName.concat(String.format(TEST_NAME_FORMAT,
                    pickleWrapper.getPickle().getScenarioLine(),
                    pickleWrapper.getPickle().getLine()));
        }
        return testName;
    }

    private static String prepareTestName(String strFormat, PickleWrapper pickleWrapper, FeatureWrapper featureWrapper) {
        String featureName = cleanQuotes(featureWrapper.toString());
        if (featureName.startsWith(FEATURE_NAME_OPTIONAL + "[")) {
            featureName = featureName.replace(FEATURE_NAME_OPTIONAL, "");
        }
        return String.format(strFormat, cleanBrackets(featureName), cleanQuotes(pickleWrapper.toString()));
    }

    private static String cleanQuotes(String originalString) {
        return StringUtils.removeEnd(StringUtils.removeStart(originalString, "\""), "\"");
    }

    private static String cleanBrackets(String originalString) {
        return StringUtils.removeEnd(StringUtils.removeStart(originalString, "["), "]");
    }
}
