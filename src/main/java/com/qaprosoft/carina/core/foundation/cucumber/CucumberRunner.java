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

import com.qaprosoft.carina.core.foundation.AbstractTest;
import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.Configuration;

import net.masterthought.cucumber.ReportBuilder;

import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import io.cucumber.testng.PickleWrapper;
import io.cucumber.testng.TestNGCucumberRunner;

public abstract class CucumberRunner extends AbstractTest {
    private TestNGCucumberRunner testNGCucumberRunner;

    protected static final Logger LOGGER = Logger.getLogger(CucumberRunner.class);

    public CucumberRunner() {
        this.testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
    }

    @BeforeClass(alwaysRun = true)
    public void setUpClass() throws Exception {
        this.testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
    }

    @Test(groups = { "cucumber" }, description = "Runs Cucumber Feature", dataProvider = "features")
    public void feature(PickleWrapper pickleWrapper) {
        this.testNGCucumberRunner.runScenario(pickleWrapper.getPickle());
    }

    @DataProvider
    public Object[][] features() {
        Object[][] scenarios = this.testNGCucumberRunner.provideScenarios();
        Object[][] result = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            Object[] scenario = scenarios[i];
            result[i] = new Object[1];
            for (int j = 0; j < scenario.length; j++) {
                result[i][0] = scenario[0];
            }
        }
        return result;
    }

    @AfterClass
    public void tearDownClass(ITestContext context) throws Exception {
        LOGGER.info("In  @AfterClass tearDownClass");
        this.testNGCucumberRunner.finish();
        generateCucumberReport();
    }

    /**
     * Generate Cucumber Report
     */
    private void generateCucumberReport() {
        String buildNumber = Configuration.get(Configuration.Parameter.APP_VERSION);
        //TODO: adjust test/suiteName

        try {
            // String RootDir = System.getProperty("user.dir");
            File file = ReportContext.getArtifactsFolder();

            File reportOutputDirectory = new File(String.format("%s/%s", file, SpecialKeywords.CUCUMBER_REPORT_FOLDER));

            File dir = new File("target/");

            File[] finder = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".json");
                }
            });

            List<String> list = new ArrayList<String>();

            for (File fl : finder) {
                LOGGER.info("Report json: " + fl.getName());
                list.add("target/" + fl.getName());
            }

            // buildNumber should be parsable Integer
            buildNumber = buildNumber.replace(".", "").replace(",", "");

            if (list.size() > 0) {
                // String buildNumber = "1";
                // String buildProject = "CUCUMBER";
                boolean skippedFails = true;
                boolean pendingFails = true;
                boolean undefinedFails = true;
                boolean missingFails = true;

                net.masterthought.cucumber.Configuration configuration = new net.masterthought.cucumber.Configuration(reportOutputDirectory,
                        "Cucumber Test Results");
                configuration.setStatusFlags(skippedFails, pendingFails, undefinedFails, missingFails);
                // configuration.setParallelTesting(parallelTesting);
                // configuration.setJenkinsBasePath(jenkinsBasePath);
                // configuration.setRunWithJenkins(runWithJenkins);
                configuration.setBuildNumber(buildNumber);

                ReportBuilder reportBuilder = new ReportBuilder(list, configuration);
                reportBuilder.generateReports();
            } else {
                LOGGER.info("There are no json files for cucumber report.");
                return;
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Check that CucumberReport Folder exists.
     *
     * @return boolean
     */
    public static boolean isCucumberReportFolderExists() {
        try {
            File reportOutputDirectory = new File(String.format("%s/%s", ReportContext.getArtifactsFolder(), SpecialKeywords.CUCUMBER_REPORT_FOLDER));
            if (reportOutputDirectory.exists() && reportOutputDirectory.isDirectory()) {
                if (reportOutputDirectory.list().length > 0) {
                    LOGGER.debug("Cucumber Report Folder is not empty!");
                    return true;
                } else {
                    LOGGER.error("Cucumber Report Folder is empty!");
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error happen during checking that CucumberReport Folder exists or not. Error: " + e.getMessage());
        }
        return false;
    }
}
