package com.qaprosoft.carina.core.foundation.cucumber;

import java.util.Arrays;
import java.util.Map;

import com.zebrunner.carina.utils.commons.SpecialKeywords;
import org.apache.commons.lang3.StringUtils;
import org.testng.ITestResult;

import com.zebrunner.agent.testng.core.testname.TestNameResolver;

import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;

public class CucumberNameResolver implements TestNameResolver {

    private final static String FEATURE_NAME_OPTIONAL = "Optional";

    @Override
    @SuppressWarnings({ "unlikely-arg-type" })
    public String resolve(ITestResult result) {
        String name = "";

        if (result.getTestContext() == null) {
            throw new RuntimeException("Unable to set Test name without testContext!");
        }
        @SuppressWarnings("unchecked")
        Map<Object[], String> testnameMap = (Map<Object[], String>) result.getTestContext().getAttribute(SpecialKeywords.TEST_NAME_ARGS_MAP);

        if (testnameMap != null) {
            String testHash = String.valueOf(Arrays.hashCode(result.getParameters()));
            if (testnameMap.containsKey(testHash)) {
                name = testnameMap.get(testHash);
            }
        }

        if (name.isEmpty()) {
            name = result.getTestContext().getCurrentXmlTest().getName();
        }

        return name;
    }

    public static String prepareTestName(String strFormat, PickleWrapper pickleWrapper, FeatureWrapper featureWrapper) {
        String featureName = cleanQuotes(featureWrapper.toString());
        if (featureName.startsWith(FEATURE_NAME_OPTIONAL + "[")) {
            featureName = featureName.replace(FEATURE_NAME_OPTIONAL, "");
        }
        return String.format(strFormat, cleanBrackets(featureName), cleanQuotes(pickleWrapper.toString()));
    }

    private static String cleanQuotes(String originalString) {
        String res = StringUtils.removeEnd(StringUtils.removeStart(originalString, "\""), "\"");
        return res;
    }

    private static String cleanBrackets(String originalString) {
        String res = StringUtils.removeEnd(StringUtils.removeStart(originalString, "["), "]");
        return res;
    }

}
