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

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import com.zebrunner.carina.cucumber.config.CucumberConfiguration;
import io.cucumber.testng.CucumberPropertiesProvider;
import io.cucumber.testng.Pickle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.zebrunner.agent.core.config.ConfigurationHolder;
import com.zebrunner.agent.core.registrar.Artifact;
import com.zebrunner.agent.testng.core.testname.TestNameResolverRegistry;
import com.zebrunner.carina.core.AbstractTest;
import com.zebrunner.carina.core.config.ReportConfiguration;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.ReportContext;

import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import io.cucumber.testng.TestNGCucumberRunner;
import net.masterthought.cucumber.ReportBuilder;
import org.testng.xml.XmlTest;

public abstract class CucumberRunner extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CUCUMBER_REPORT_NAME = "Cucumber report";
    private static final String ZAFIRA_REPORT_CI = "ZafiraReport";
    private static final String CUCUMBER_REPORT_CI = "CucumberReport";
    private TestNGCucumberRunner testNGCucumberRunner;

    protected CucumberRunner() {
        TestNameResolverRegistry.set(new CucumberNameResolver());
    }

    /**
     * @deprecated useless method
     */
    @Deprecated(forRemoval = true, since = "1.1.5")
    public TestNGCucumberRunner getTestNGCucumberRunner() {
        return testNGCucumberRunner;
    }

    /**
     * @deprecated useless method
     */
    @Deprecated(forRemoval = true, since = "1.1.5")
    public void setTestNGCucumberRunner(TestNGCucumberRunner testNGCucumberRunner) {
        this.testNGCucumberRunner = testNGCucumberRunner;
    }

    @BeforeClass(alwaysRun = true)
    public void setUpClass(ITestContext context) {
        XmlTest currentXmlTest = context.getCurrentXmlTest();
        CucumberPropertiesProvider properties = currentXmlTest::getParameter;
        testNGCucumberRunner = new TestNGCucumberRunner(this.getClass(), properties);
    }

    @Test(groups = { "cucumber" }, description = "Runs Cucumber Feature", dataProvider = "features")
    public void feature(PickleWrapper pickleWrapper, FeatureWrapper featureWrapper, @SuppressWarnings("unused") String uniqueId) {
        if (Configuration.getRequired(CucumberConfiguration.Parameter.CUSTOM_TESTDIR_NAMING, Boolean.class)) {
            ReportContext.setCustomTestDirName(
                    CucumberNameResolver.generateTestName(pickleWrapper, featureWrapper, this.testNGCucumberRunner.provideScenarios().length));
        }
        this.testNGCucumberRunner.runScenario(pickleWrapper.getPickle());
    }

    @DataProvider(parallel = true)
    public Object[][] features(ITestContext context) {
        context.setAttribute(CucumberNameResolver.SCENARIO_COUNT_PARAMETER, this.testNGCucumberRunner.provideScenarios().length);
        Object[][] parameters = testNGCucumberRunner.provideScenarios();
        Object[][] newParams = new Object[parameters.length][1];
        for (int i = 0; i < parameters.length; i++) {
            newParams[i] = new Object[3];
            newParams[i][0] = parameters[i][0];
            newParams[i][1] = parameters[i][1];

            PickleWrapper pickleWrapper = (PickleWrapper) parameters[i][0];
            Pickle pickle = pickleWrapper.getPickle();
            // set unique id for correct reporting (it guarantee that same test will be used for rerunning (retry)
            newParams[i][2] = String.format("%s.%s.%s.%s", pickle.getUri(), pickle.getScenarioLine(), pickle.getLine(), pickle.getName());
        }
        return newParams;
    }

    @AfterClass
    public void tearDownClass(ITestContext context) {
        LOGGER.info("Finishing test execution and generating Cucumber report.");
        this.testNGCucumberRunner.finish();
        generateCucumberReport();
    }

    /**
     * Generate Cucumber Report
     */
    private void generateCucumberReport() {
        try {
            File file = ReportContext.getBaseDir();
            File reportOutputDirectory = new File(String.format("%s/%s", file, SpecialKeywords.CUCUMBER_REPORT_FOLDER));
            File dir = new File("target/");
            File[] finder = dir.listFiles((dir1, filename) -> filename.endsWith(".json"));
            List<String> list = new ArrayList<>();
            for (File fl : finder) {
                LOGGER.info("Report json: {}", fl.getName());
                list.add("target/" + fl.getName());
            }

            if (!list.isEmpty()) {
                net.masterthought.cucumber.Configuration configuration = new net.masterthought.cucumber.Configuration(reportOutputDirectory,
                        "Cucumber Test Results");
                // configuration.setStatusFlags(skippedFails, pendingFails, undefinedFails, missingFails);
                // configuration.setParallelTesting(parallelTesting);
                // configuration.setJenkinsBasePath(jenkinsBasePath);
                // configuration.setRunWithJenkins(runWithJenkins);

                Configuration.get(ReportConfiguration.Parameter.APP_VERSION)
                        // buildNumber should be parsable Integer
                        .map(appVersion -> appVersion.replace(".", "").replace(",", ""))
                        .ifPresent(configuration::setBuildNumber);

                ReportBuilder reportBuilder = new ReportBuilder(list, configuration);
                reportBuilder.generateReports();

                Configuration.get(ReportConfiguration.Parameter.CI_BUILD_URL)
                        .ifPresent(ciBuildURL -> {
                            if (ConfigurationHolder.isReportingEnabled()) {
                                if (ciBuildURL.endsWith(ZAFIRA_REPORT_CI)) {
                                    Artifact.attachReferenceToTestRun(CUCUMBER_REPORT_NAME, ciBuildURL.replace(ZAFIRA_REPORT_CI, CUCUMBER_REPORT_CI));
                                } else {
                                    Artifact.attachReferenceToTestRun(CUCUMBER_REPORT_NAME, ReportConfiguration.getCucumberReportLink());
                                }
                            }
                        });
            } else {
                LOGGER.info("There are no json files for cucumber report.");
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
            File reportOutputDirectory = new File(String.format("%s/%s", ReportContext.getBaseDir(), SpecialKeywords.CUCUMBER_REPORT_FOLDER));
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
            LOGGER.debug("Error happen during checking that CucumberReport Folder exists or not. Error: {}", e.getMessage());
        }
        return false;
    }

}
