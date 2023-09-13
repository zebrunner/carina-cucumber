package com.zebrunner.carina.cucumber.config;

import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.config.IParameter;

public class CucumberConfiguration extends Configuration {

    public enum Parameter implements IParameter {

        /**
         * Determines whether the ability to set a custom test directory (report folder) name is enabled. <b>Default: false</b>
         */
        CUSTOM_TESTDIR_NAMING("custom_testdir_naming");

        private final String key;

        Parameter(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Override
    public String toString() {
        return asString(Parameter.values())
                .map(s -> "\n============= Cucumber configuration ==============\n" + s)
                .orElse("");
    }
}
