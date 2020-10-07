package com.qaprosoft.carina.core.foundation.cucumber;

import java.util.Arrays;
import java.util.Map;

import org.testng.ITestResult;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.zebrunner.agent.testng.core.testname.TestNameResolver;

public class CucumberNameResolver implements TestNameResolver {

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

        if (result.getTestContext().getCurrentXmlTest().getAllParameters().containsKey(SpecialKeywords.EXCEL_DS_CUSTOM_PROVIDER) ||
                result.getTestContext().getCurrentXmlTest().getAllParameters().containsKey(SpecialKeywords.DS_CUSTOM_PROVIDER)) {
            // AUTO-274 "Passing status set on emailable report when a test step fails"
            String methodUID = "";
            for (int i = 0; i < result.getParameters().length; i++) {
                if (result.getParameters()[i] != null) {
                    if (result.getParameters()[i].toString().contains(SpecialKeywords.TUID + ":")) {
                        methodUID = result.getParameters()[i].toString().replace(SpecialKeywords.TUID + ":", "");
                        break; // first TUID: parameter is used
                    }
                }
            }
            if (!methodUID.isEmpty()) {
                name = methodUID + " - " + name;
            }
        }

        name = appendDataProviderLine(result, name);

        return name;
    }

    private static String appendDataProviderLine(ITestResult testResult, String testName) {
        if (testResult.getMethod().isDataDriven() && testResult.getMethod().getDataProviderMethod().getMethod().getModifiers() > 1) {
            // adding extra zero at the beginning of the data provider line number
            int indexMaxLength = Integer.toString(testResult.getMethod().getDataProviderMethod().getMethod().getModifiers()).length() + 1;
            String lineFormat = " [L%0" + indexMaxLength + "d]";
            int index = testResult.getMethod().getParameterInvocationCount() + 1;
            testName += String.format(lineFormat, index);
        }
        return testName;
    }

}
