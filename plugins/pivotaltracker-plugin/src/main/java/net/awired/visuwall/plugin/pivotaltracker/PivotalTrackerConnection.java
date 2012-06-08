package net.awired.visuwall.plugin.pivotaltracker;

import static java.lang.System.getProperty;
import static net.awired.visuwall.api.domain.BuildState.SUCCESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.awired.clients.common.ResourceNotFoundException;
import net.awired.clients.pivotaltracker.PivotalTrackerClient;
import net.awired.clients.pivotaltracker.resource.Project;
import net.awired.clients.pivotaltracker.resource.Stories;
import net.awired.visuwall.api.domain.BuildState;
import net.awired.visuwall.api.domain.BuildTime;
import net.awired.visuwall.api.domain.Commiter;
import net.awired.visuwall.api.domain.ProjectKey;
import net.awired.visuwall.api.domain.SoftwareProjectId;
import net.awired.visuwall.api.exception.BuildIdNotFoundException;
import net.awired.visuwall.api.exception.BuildNotFoundException;
import net.awired.visuwall.api.exception.ConnectionException;
import net.awired.visuwall.api.exception.MavenIdNotFoundException;
import net.awired.visuwall.api.exception.ProjectNotFoundException;
import net.awired.visuwall.api.plugin.capability.BuildCapability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PivotalTrackerConnection implements BuildCapability {

    private static final String DEFAULT_THRESHOLD_SUCCESS = "5";
    private boolean connected;
    private PivotalTrackerClient client;
    private Logger logger = LoggerFactory.getLogger(PivotalTrackerConnection.class);

    private PivotalTrackerState pivotalTrackerState = new PivotalTrackerState();

    private Long id = 1L;

    @Override
    public BuildState getBuildState(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        try {
            Stories stories = client.getStoriesOf(Integer.valueOf(projectId.getProjectId()));
            Map<BuildState, Integer> thresholds = new HashMap<BuildState, Integer>();
            thresholds.put(SUCCESS, successThreshold(projectId));
            return pivotalTrackerState.guessState(thresholds, stories);
        } catch (NumberFormatException e) {
            logger.error("Cannot guess state", e);
        } catch (ResourceNotFoundException e) {
            logger.error("Cannot guess state", e);
        }
        return BuildState.UNKNOWN;
    }

    private Integer successThreshold(SoftwareProjectId projectId) {
        String thresholdSuccessKey = "threshold.success." + projectId.getProjectId();
        return Integer.valueOf(getProperty(thresholdSuccessKey, DEFAULT_THRESHOLD_SUCCESS));
    }

    // ----------------------------------------------------------------------------------------------------------- //

    @Override
    public void connect(String url, String login, String password) throws ConnectionException {
        client = new PivotalTrackerClient(url, login, password);
        connected = true;
    }

    @Override
    public boolean isClosed() {
        return !connected;
    }

    @Override
    public String getName(SoftwareProjectId projectId) throws ProjectNotFoundException {
        try {
            for (Project project : client.getProjects()) {
                if (project.getId().toString().equals(projectId.getProjectId())) {
                    return project.getName();
                }
            }
        } catch (ResourceNotFoundException e) {
            throw new ProjectNotFoundException("Cannot find project with id: " + projectId, e);
        }
        throw new ProjectNotFoundException("Cannot find project with id: " + projectId);
    }

    @Override
    public Map<SoftwareProjectId, String> listSoftwareProjectIds() {
        Map<SoftwareProjectId, String> softwareProjectIds = new HashMap<SoftwareProjectId, String>();
        try {
            for (Project project : client.getProjects()) {
                SoftwareProjectId softwareProjectId = new SoftwareProjectId(project.getId().toString());
                softwareProjectIds.put(softwareProjectId, project.getName());
            }
        } catch (ResourceNotFoundException e) {
            logger.error("Cannot find projects", e);
        }
        return softwareProjectIds;
    }

    @Override
    public void close() {
    }

    @Override
    public String getDescription(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        return "";
    }

    @Override
    public boolean isProjectDisabled(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        return false;
    }

    @Override
    public List<Commiter> getBuildCommiters(SoftwareProjectId softwareProjectId, String buildId)
            throws BuildNotFoundException, ProjectNotFoundException {
        return new ArrayList<Commiter>();
    }

    @Override
    public BuildTime getBuildTime(SoftwareProjectId softwareProjectId, String buildId) throws BuildNotFoundException,
            ProjectNotFoundException {
        BuildTime buildTime = new BuildTime();
        buildTime.setDuration(1L);
        buildTime.setStartTime(new Date());
        return buildTime;
    }

    @Override
    public List<String> getBuildIds(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException {
        return Arrays.asList((id - 1) + "");
    }

    @Override
    public Date getEstimatedFinishTime(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        return new Date();
    }

    @Override
    public boolean isBuilding(SoftwareProjectId projectId, String buildId) throws ProjectNotFoundException,
            BuildNotFoundException {
        return false;
    }

    @Override
    public String getLastBuildId(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException,
            BuildIdNotFoundException {
        return (id++).toString();
    }

    // ----------------------------------------------------------------------------------------------------------- //

    @Override
    public String getMavenId(SoftwareProjectId softwareProjectId) throws ProjectNotFoundException,
            MavenIdNotFoundException {
        throw new ProjectNotFoundException("Not implemented!");
    }

    @Override
    public SoftwareProjectId identify(ProjectKey projectKey) throws ProjectNotFoundException {
        throw new ProjectNotFoundException("Cannot find project with projectKey: " + projectKey);
    }

}
