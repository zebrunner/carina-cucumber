package com.qaprosoft.carina.core.foundation.cucumber;

import org.apache.commons.lang3.StringUtils;

import com.zebrunner.carina.core.testng.ZebrunnerNameResolver;

import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;

public class CucumberNameResolver extends ZebrunnerNameResolver {

    private static final String FEATURE_NAME_OPTIONAL = "Optional";

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
