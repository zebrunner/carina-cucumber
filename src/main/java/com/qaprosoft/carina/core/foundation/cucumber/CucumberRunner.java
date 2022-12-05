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

import java.io.File;
import java.io.FilenameFilter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.qaprosoft.carina.core.foundation.utils.R;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.qaprosoft.carina.core.foundation.AbstractTest;
import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.zebrunner.agent.core.registrar.Artifact;
import com.zebrunner.agent.testng.core.testname.TestNameResolverRegistry;

import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import io.cucumber.testng.TestNGCucumberRunner;
import net.masterthought.cucumber.ReportBuilder;

public abstract class CucumberRunner extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private TestNGCucumberRunner testNGCucumberRunner;

    private final static String STR_FORMAT_TEST_NAME = "%s (%s)";
    private final static String STR_FORMAT_TEST_FOLDER_NAME = "%s_%s";
    private final static String EXAMPLE_FILE_NAME_FORMAT = "_ex%04d";
    private final static String EXAMPLE_FILE_NAME_REGEX = "(_ex\\d+){0,1}";
    private final static String EXAMPLE_TEST_NAME_FORMAT = " EX%04d";
    private final static String EXAMPLE_TEST_NAME_REGEX = "( EX\\d+){0,1}";

    private final static String CUCUMBER_REPORT_NAME = "Cucumber report";
    private final static String ZAFIRA_REPORT_CI = "ZafiraReport";
    private final static String CUCUMBER_REPORT_CI = "CucumberReport";

    List<String> testNamesList = Collections.synchronizedList(new ArrayList<String>());

    public CucumberRunner() {
        this.testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
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
        if(R.CONFIG.getBoolean("custom_testdir_naming")) {
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
        String buildNumber = Configuration.get(Parameter.APP_VERSION);

        try {
            // String RootDir = System.getProperty("user.dir");
            File file = ReportContext.getBaseDir();

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
                // boolean skippedFails = true;
                // boolean pendingFails = true;
                // boolean undefinedFails = true;
                // boolean missingFails = true;

                net.masterthought.cucumber.Configuration configuration = new net.masterthought.cucumber.Configuration(reportOutputDirectory,
                        "Cucumber Test Results");
                // configuration.setStatusFlags(skippedFails, pendingFails, undefinedFails, missingFails);
                // configuration.setParallelTesting(parallelTesting);
                // configuration.setJenkinsBasePath(jenkinsBasePath);
                // configuration.setRunWithJenkins(runWithJenkins);
                configuration.setBuildNumber(buildNumber);

                ReportBuilder reportBuilder = new ReportBuilder(list, configuration);
                reportBuilder.generateReports();

                if (!Configuration.isNull(Parameter.REPORT_URL)) {
                    String reportUrl = Configuration.get(Parameter.REPORT_URL);
                    if (reportUrl.endsWith(ZAFIRA_REPORT_CI)) {
                        Artifact.attachReferenceToTestRun(CUCUMBER_REPORT_NAME, reportUrl.replace(ZAFIRA_REPORT_CI, CUCUMBER_REPORT_CI));
                    } else {
                        Artifact.attachReferenceToTestRun(CUCUMBER_REPORT_NAME, ReportContext.getCucumberReportLink());
                    }
                }
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
            LOGGER.debug("Error happen during checking that CucumberReport Folder exists or not. Error: " + e.getMessage());
        }
        return false;
    }

}
