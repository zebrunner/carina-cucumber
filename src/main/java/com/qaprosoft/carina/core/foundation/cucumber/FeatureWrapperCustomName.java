package com.qaprosoft.carina.core.foundation.cucumber;

import org.apache.commons.lang3.StringUtils;

import io.cucumber.testng.FeatureWrapper;

public class FeatureWrapperCustomName {

    private final FeatureWrapper featureWrapper;

    private final static String OPTIONAL_KEY = "Optional";

    FeatureWrapperCustomName(FeatureWrapper featureWrapper) {
        this.featureWrapper = featureWrapper;
    }

    public FeatureWrapper getFeatureWrapper() {
        return featureWrapper;
    }

    @Override
    public String toString() {
        return "\"" + formatName(featureWrapper.toString()) + "\"";
    }

    private String formatName(String originalName) {
        return StringUtils.removeEnd(StringUtils.removeStart(originalName, "\"" + OPTIONAL_KEY + "["), "]\"");
    }

}
