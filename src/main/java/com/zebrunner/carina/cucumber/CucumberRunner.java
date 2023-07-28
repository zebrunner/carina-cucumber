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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.xml.XmlTest;

import com.zebrunner.agent.core.config.ConfigurationHolder;
import com.zebrunner.agent.core.registrar.Artifact;
import com.zebrunner.agent.testng.core.testname.TestNameResolverRegistry;
import com.zebrunner.carina.core.IAbstractTest;
import com.zebrunner.carina.core.config.ReportConfiguration;
import com.zebrunner.carina.cucumber.config.CucumberConfiguration;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.config.Configuration;
import com.zebrunner.carina.utils.report.ReportContext;

import io.cucumber.testng.CucumberPropertiesProvider;
import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import io.cucumber.testng.TestNGCucumberRunner;
import net.masterthought.cucumber.ReportBuilder;

public abstract class CucumberRunner implements IAbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String STR_FORMAT_TEST_NAME = "%s (%s)";
    private static final String STR_FORMAT_TEST_FOLDER_NAME = "%s_%s";
    private static final String EXAMPLE_TEST_NAME_FORMAT = " EX%04d";
    private static final String EXAMPLE_TEST_NAME_REGEX = "( EX\\d+){0,1}";
    private static final String CUCUMBER_REPORT_NAME = "Cucumber report";
    private static final String ZAFIRA_REPORT_CI = "ZafiraReport";
    private TestNGCucumberRunner testNGCucumberRunner;
    List<String> testNamesList = Collections.synchronizedList(new ArrayList<>());

    @BeforeClass(alwaysRun = true)
    public void setUpClass(ITestContext context) {
        XmlTest currentXmlTest = context.getCurrentXmlTest();
        CucumberPropertiesProvider properties = currentXmlTest::getParameter;
        testNGCucumberRunner = new TestNGCucumberRunner(this.getClass(), properties);
        TestNameResolverRegistry.set(new CucumberNameResolver());
    }

    public TestNGCucumberRunner getTestNGCucumberRunner() {
        return testNGCucumberRunner;
    }

    public void setTestNGCucumberRunner(TestNGCucumberRunner testNGCucumberRunner) {
        this.testNGCucumberRunner = testNGCucumberRunner;
    }

    @Test(groups = {"cucumber"}, description = "Runs Cucumber Feature", dataProvider = "features")
    public void feature(PickleWrapper pickleWrapper, FeatureWrapperCustomName featureWrapper, String providerTestName) {
        String testName;
        if (StringUtils.isNoneBlank(providerTestName)) {
            testName = providerTestName;
        } else {
            testName = CucumberNameResolver.prepareTestName(STR_FORMAT_TEST_FOLDER_NAME, pickleWrapper, featureWrapper.getFeatureWrapper());
        }
        if (Configuration.getRequired(CucumberConfiguration.Parameter.CUSTOM_TESTDIR_NAMING, Boolean.class)) {
            ReportContext.setCustomTestDirName(testName);
        }
        testNamesList.add(testName);
        this.testNGCucumberRunner.runScenario(pickleWrapper.getPickle());
        // think about catching IllegalStateException
    }

    @DataProvider(parallel = true)
    public Object[][] features(ITestContext context) {
        Object[][] scenarios = this.testNGCucumberRunner.provideScenarios();
        Object[][] result = new Object[scenarios.length][1];
        Map<String, String> testNameArgsMap = Collections.synchronizedMap(new HashMap<>());
        for (int i = 0; i < scenarios.length; i++) {
            Object[] scenario = scenarios[i];
            result[i] = new Object[3];
            result[i][0] = scenario[0];
            result[i][1] = new FeatureWrapperCustomName((FeatureWrapper) scenario[1]);
            final String testName = CucumberNameResolver.prepareTestName(STR_FORMAT_TEST_NAME, (PickleWrapper) scenario[0],
                    (FeatureWrapper) scenario[1]);
            List<String> exampleNums = testNameArgsMap.values().stream().filter(s -> s.matches(Pattern.quote(testName) + EXAMPLE_TEST_NAME_REGEX))
                    .collect(Collectors.toList());
            if (!exampleNums.isEmpty()) {
                String newTestName = testName.concat(String.format(EXAMPLE_TEST_NAME_FORMAT, exampleNums.size() + 1));
                result[i][2] = newTestName;
                testNameArgsMap.put(String.valueOf(Arrays.hashCode(result[i])), newTestName);
            } else {
                result[i][2] = testName;
                testNameArgsMap.put(String.valueOf(Arrays.hashCode(result[i])), testName);
            }
        }
        context.setAttribute(SpecialKeywords.TEST_NAME_ARGS_MAP, testNameArgsMap);
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
                                    Artifact.attachReferenceToTestRun(CUCUMBER_REPORT_NAME, ciBuildURL.replace(ZAFIRA_REPORT_CI, "CucumberReport"));
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
