/**
 *
 *     Copyright (C) norad.fr
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package fr.norad.visuwall.plugin.hudson;

import fr.norad.visuwall.providers.hudson.domain.HudsonTestResult;
import fr.norad.visuwall.api.domain.TestResult;

public class TestResults {

    public static TestResult createFrom(HudsonTestResult hudsonTestResult) {
        TestResult testResult = new TestResult();
        testResult.setFailCount(hudsonTestResult.getFailCount());
        testResult.setPassCount(hudsonTestResult.getPassCount());
        testResult.setSkipCount(hudsonTestResult.getSkipCount());
        return testResult;
    }

}
