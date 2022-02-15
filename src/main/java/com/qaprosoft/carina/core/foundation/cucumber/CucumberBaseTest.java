/*******************************************************************************
 * Copyright 2013-2018 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.carina.core.foundation.cucumber;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.carina.core.foundation.webdriver.CarinaDriver;
import com.qaprosoft.carina.core.foundation.webdriver.Screenshot;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;


public class CucumberBaseTest extends CucumberRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Check is it Cucumber Test or not.
     *
     * @throws Throwable java.lang.Throwable
     */
    @Before
    public void beforeScenario() throws Throwable {
        LOGGER.info("CucumberBaseTest->beforeScenario");
    }

    /**
     * take Screenshot Of Failure - this step should be added manually in common step definition
     * files if it will not be executed automatically
     *
     * @param scenario Scenario
     */
    @After
    public void takeScreenshotOfFailure(Scenario scenario) {
        LOGGER.info("In  @After takeScreenshotOfFailure");
        if (scenario.isFailed()) {
            LOGGER.error("Cucumber Scenario FAILED! Creating screenshot.");
            String screenId = "";

            ConcurrentHashMap<String, CarinaDriver> drivers = getDrivers();

            for (Map.Entry<String, CarinaDriver> entry : drivers.entrySet()) {
                String driverName = entry.getKey();
                WebDriver drv = entry.getValue().getDriver();

                if (drv instanceof EventFiringWebDriver) {
                    drv = ((EventFiringWebDriver) drv).getWrappedDriver();
                }

                screenId = Screenshot.capture(drv, driverName + ": " + scenario.getName()); // in case of failure
                LOGGER.debug("cucumber screenshot generated: " + screenId);
            }
        }
    }
}
