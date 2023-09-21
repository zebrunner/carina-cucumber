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
package com.zebrunner.carina.cucumber;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zebrunner.carina.webdriver.CarinaDriver;
import com.zebrunner.carina.webdriver.Screenshot;
import com.zebrunner.carina.webdriver.ScreenshotType;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class CucumberBaseTest extends CucumberRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
            ConcurrentHashMap<String, CarinaDriver> drivers = getDrivers();

            for (Map.Entry<String, CarinaDriver> entry : drivers.entrySet()) {
                String driverName = entry.getKey();
                // in case of failure
                Screenshot.capture(entry.getValue().getDriver(),
                        ScreenshotType.UNSUCCESSFUL_DRIVER_ACTION,
                        driverName + ": " + scenario.getName())
                        .ifPresent(fileName -> LOGGER.debug("cucumber screenshot generated: {}", fileName));
            }
        }
    }
}
