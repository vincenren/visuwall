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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static fr.norad.visuwall.api.domain.BuildState.UNKNOWN;
import static fr.norad.visuwall.plugin.hudson.States.asVisuwallState;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.norad.visuwall.providers.hudson.Hudson;
import fr.norad.visuwall.providers.hudson.domain.HudsonBuild;
import fr.norad.visuwall.providers.hudson.domain.HudsonCommiter;
import fr.norad.visuwall.providers.hudson.domain.HudsonJob;
import fr.norad.visuwall.providers.hudson.domain.HudsonTestResult;
import fr.norad.visuwall.providers.hudson.exception.HudsonBuildNotFoundException;
import fr.norad.visuwall.providers.hudson.exception.HudsonJobNotFoundException;
import fr.norad.visuwall.providers.hudson.exception.HudsonViewNotFoundException;
import fr.norad.visuwall.api.domain.BuildState;
import fr.norad.visuwall.api.domain.BuildTime;
import fr.norad.visuwall.api.domain.Commiter;
import fr.norad.visuwall.api.domain.ProjectKey;
import fr.norad.visuwall.api.domain.SoftwareProjectId;
import fr.norad.visuwall.api.domain.TestResult;
import fr.norad.visuwall.api.exception.BuildIdNotFoundException;
import fr.norad.visuwall.api.exception.BuildNotFoundException;
import fr.norad.visuwall.api.exception.MavenIdNotFoundException;
import fr.norad.visuwall.api.exception.ProjectNotFoundException;
import fr.norad.visuwall.api.exception.ViewNotFoundException;
import fr.norad.visuwall.api.plugin.capability.BuildCapability;
import fr.norad.visuwall.api.plugin.capability.TestCapability;
import fr.norad.visuwall.api.plugin.capability.ViewCapability;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class HudsonConnection implements BuildCapability, ViewCapability, TestCapability {

    private static final Logger LOG = LoggerFactory.getLogger(HudsonConnection.class);

    private static final Collection<String> DEFAULT_VIEWS = asList("Alle", "Todo", "Tous", "\u3059\u3079\u3066",
            "Tudo", "\u0412\u0441\u0435", "Hepsi", "All");

    @VisibleForTesting
    Hudson hudson;

    private boolean connected;

    @Override
    public void connect(String url, String login, String password) {
        checkNotNull(url, "url is mandatory");
        if (isBlank(url)) {
            throw new IllegalStateException("url can't be null.");
        }
        if (StringUtils.isBlank(login)) {
            hudson = new Hudson(url);
        } else {
            hudson = new Hudson(url, login, password);
        }
        connected = true;
    }

    @Override
    public Map<SoftwareProjectId, String> listSoftwareProjectIds() {
        checkConnected();
        Map<SoftwareProjectId, String> projectIds = new HashMap<SoftwareProjectId, String>();
        List<String> names = hudson.findAllProjectNames();
        for (String name : names) {
            SoftwareProjectId projectId = new SoftwareProjectId(name);
            projectIds.put(projectId, name);
        }
        return projectIds;
    }

    @Override
    public Date getEstimatedFinishTime(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        checkConnected();
        checkSoftwareProjectId(projectId);
        checkBuildId(buildId);
        try {
            String projectName = jobName(projectId);
            return hudson.getEstimatedFinishTime(projectName);
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException(e);
        }
    }

    @Override
    public boolean isBuilding(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        checkConnected();
        checkSoftwareProjectId(projectId);
        checkBuildId(buildId);
        try {
            String projectName = jobName(projectId);
            HudsonBuild build = hudson.findBuild(projectName, Integer.valueOf(buildId));
            if (build.getState() == null) {
                return true;
            }
            if (build.getDuration() == 0) {
                return true;
            }
            return false;
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException(e);
        } catch (NumberFormatException e) {
            throw new BuildNotFoundException(e);
        } catch (HudsonBuildNotFoundException e) {
            throw new BuildNotFoundException(e);
        }
    }

    @Override
    public BuildState getBuildState(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        checkConnected();
        checkSoftwareProjectId(projectId);
        checkBuildId(buildId);
        try {
            String projectName = jobName(projectId);
            HudsonBuild hudsonBuild = hudson.findBuild(projectName, Integer.valueOf(buildId));
            String hudsonState = hudsonBuild.getState();
            if (hudsonState == null) {
                return UNKNOWN;
            }
            return asVisuwallState(hudsonState);
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException(e);
        } catch (HudsonBuildNotFoundException e) {
            throw new ProjectNotFoundException(e);
        }
    }

    @Override
    public String getLastBuildId(SoftwareProjectId projectId) throws ProjectNotFoundException, BuildIdNotFoundException {
        checkConnected();
        checkSoftwareProjectId(projectId);
        try {
            String projectName = jobName(projectId);
            return String.valueOf(hudson.getLastBuildNumber(projectName));
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException(e);
        } catch (HudsonBuildNotFoundException e) {
            throw new BuildIdNotFoundException(e);
        }
    }

    @Override
    public List<String> findViews() {
        checkConnected();
        List<String> views = hudson.findViews();
        views.removeAll(DEFAULT_VIEWS);
        return views;
    }

    @Override
    public List<String> findProjectNamesByView(String viewName) throws ViewNotFoundException {
        checkConnected();
        checkNotNull(viewName, "viewName is mandatory");
        try {
            return hudson.findJobNameByView(viewName);
        } catch (HudsonViewNotFoundException e) {
            throw new ViewNotFoundException("can't find view named :" + viewName, e);
        }
    }

    @Override
    public List<SoftwareProjectId> findSoftwareProjectIdsByViews(List<String> views) {
        checkConnected();
        checkNotNull(views, "views is mandatory");
        Set<SoftwareProjectId> projectIds = new HashSet<SoftwareProjectId>();
        for (String viewName : views) {
            try {
                List<String> projectNames = hudson.findJobNameByView(viewName);
                projectIds.addAll(findSoftwareProjectIdsByNames(projectNames));
            } catch (HudsonViewNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
        return new ArrayList<SoftwareProjectId>(projectIds);
    }

    private List<SoftwareProjectId> findSoftwareProjectIdsByNames(List<String> names) {
        checkConnected();
        checkNotNull(names, "names is mandatory");
        List<SoftwareProjectId> projectIds = new ArrayList<SoftwareProjectId>();
        for (String name : names) {
            SoftwareProjectId projectId = new SoftwareProjectId(name);
            projectIds.add(projectId);
        }
        return projectIds;
    }

    @Override
    public void close() {
        connected = false;
    }

    @Override
    public String getDescription(SoftwareProjectId projectId) throws ProjectNotFoundException {
        checkConnected();
        checkSoftwareProjectId(projectId);
        try {
            String jobName = jobName(projectId);
            return hudson.getDescription(jobName);
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException("Can't find description of project id: " + projectId, e);
        }
    }

    @Override
    public SoftwareProjectId identify(ProjectKey projectKey) throws ProjectNotFoundException {
        checkConnected();
        checkNotNull(projectKey, "projectKey is mandatory");
        String jobName = projectKey.getName();
        if (jobName != null) {
            try {
                hudson.findJob(jobName);
                return new SoftwareProjectId(jobName);
            } catch (HudsonJobNotFoundException e) {
                throw new ProjectNotFoundException("Can't identify job with project key: " + projectKey, e);
            }
        }
        throw new ProjectNotFoundException("Can't identify job with project key: " + projectKey);
    }

    @Override
    public List<String> getBuildIds(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        checkConnected();
        try {
            String jobName = softwareProjectId.getProjectId();
            List<Integer> buildIds = hudson.getBuildNumbers(jobName);
            List<String> res = new ArrayList<String>(buildIds.size());
            for (Integer buildId : buildIds) {
                res.add(String.valueOf(buildId));
            }
            return res;
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException("Can't find build Ids of software project id " + softwareProjectId, e);
        }
    }

    @Override
    public String getMavenId(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException,
            MavenIdNotFoundException {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        String jobName = softwareProjectId.getProjectId();
        try {
            return hudson.findMavenId(jobName);
        } catch (fr.norad.visuwall.providers.common.MavenIdNotFoundException e) {
            throw new MavenIdNotFoundException("Cannot find maven id for " + softwareProjectId, e);
        }
    }

    @Override
    public String getName(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        try {
            String projectName = softwareProjectId.getProjectId();
            HudsonJob project = hudson.findJob(projectName);
            return project.getName();
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException("Can't get name of project " + softwareProjectId, e);
        }
    }

    @Override
    public boolean isClosed() {
        return !connected;
    }

    @Override
    public BuildTime getBuildTime(SoftwareProjectId softwareProjectId, String buildId) throws BuildNotFoundException {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        checkBuildId(buildId);
        try {
            String jobName = softwareProjectId.getProjectId();
            HudsonBuild hudsonBuild = hudson.findBuild(jobName, Integer.valueOf(buildId));
            BuildTime buildTime = new BuildTime();
            buildTime.setDuration(hudsonBuild.getDuration());
            buildTime.setStartTime(hudsonBuild.getStartTime());
            return buildTime;
        } catch (HudsonBuildNotFoundException e) {
            throw new BuildNotFoundException("Can't find build #" + buildId + " of project " + softwareProjectId, e);
        } catch (HudsonJobNotFoundException e) {
            throw new BuildNotFoundException("Can't find project " + softwareProjectId, e);
        }
    }

    @Override
    public boolean isProjectDisabled(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        try {
            String jobName = softwareProjectId.getProjectId();
            HudsonJob job = hudson.findJob(jobName);
            return job.isDisabled();
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException("Can't find job with software project id: " + softwareProjectId, e);
        }
    }

    @Override
    public List<Commiter> getBuildCommiters(SoftwareProjectId softwareProjectId, String buildId)
            throws BuildNotFoundException, ProjectNotFoundException {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        checkBuildId(buildId);
        List<Commiter> commiters = new ArrayList<Commiter>();
        try {
            String jobName = softwareProjectId.getProjectId();
            HudsonBuild build = hudson.findBuild(jobName, Integer.valueOf(buildId));
            Set<HudsonCommiter> commiterSet = build.getCommiters();
            for (HudsonCommiter hudsonCommiter : commiterSet) {
                Commiter commiter = new Commiter(hudsonCommiter.getId());
                commiter.setName(hudsonCommiter.getName());
                commiter.setEmail(hudsonCommiter.getEmail());
                if (!commiters.contains(commiter)) {
                    commiters.add(commiter);
                }
            }
        } catch (HudsonJobNotFoundException e) {
            throw new ProjectNotFoundException("Can't find job with software project id: " + softwareProjectId, e);
        } catch (HudsonBuildNotFoundException e) {
            throw new BuildNotFoundException("Can't find build with software project id: " + softwareProjectId
                    + " and buildId: " + buildId, e);
        }
        return commiters;
    }

    @Override
    public TestResult analyzeUnitTests(SoftwareProjectId softwareProjectId) {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        try {
            String jobName = softwareProjectId.getProjectId();
            int lastBuildId = hudson.getLastBuildNumber(jobName);
            HudsonTestResult unitTestResult = hudson.findUnitTestResult(jobName, lastBuildId);
            return TestResults.createFrom(unitTestResult);
        } catch (HudsonJobNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't analyze integration test of project: " + softwareProjectId, e);
            }
            return new TestResult();
        } catch (HudsonBuildNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't analyze integration test of project: " + softwareProjectId, e);
            }
            return new TestResult();
        }
    }

    @Override
    public TestResult analyzeIntegrationTests(SoftwareProjectId softwareProjectId) {
        checkConnected();
        checkSoftwareProjectId(softwareProjectId);
        try {
            String jobName = softwareProjectId.getProjectId();
            int lastBuildId = hudson.getLastBuildNumber(jobName);
            HudsonTestResult integrationTestResult = hudson.findIntegrationTestResult(jobName, lastBuildId);
            return TestResults.createFrom(integrationTestResult);
        } catch (HudsonJobNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't analyze integration test of project: " + softwareProjectId, e);
            }
            return new TestResult();
        } catch (HudsonBuildNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't analyze integration test of project: " + softwareProjectId, e);
            }
            return new TestResult();
        }
    }

    private String jobName(SoftwareProjectId softwareProjectId) throws HudsonJobNotFoundException {
        String jobName = softwareProjectId.getProjectId();
        if (jobName == null) {
            throw new HudsonJobNotFoundException("Project id " + softwareProjectId + " does not contain id");
        }
        return jobName;
    }

    private void checkBuildId(String buildId) {
        checkNotNull(buildId, "buildId is mandatory");
    }

    private void checkSoftwareProjectId(SoftwareProjectId softwareProjectId) {
        checkNotNull(softwareProjectId, "softwareProjectId is mandatory");
    }

    private void checkConnected() {
        checkState(connected, "You must connect your plugin");
    }

}
