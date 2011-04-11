package net.awired.visuwall.plugin.bamboo.it;

import static net.awired.visuwall.plugin.bamboo.it.IntegrationTestData.BAMBOO_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import net.awired.visuwall.api.domain.Build;
import net.awired.visuwall.api.domain.Project;
import net.awired.visuwall.api.domain.ProjectId;
import net.awired.visuwall.api.domain.ProjectStatus.State;
import net.awired.visuwall.api.domain.TestResult;
import net.awired.visuwall.api.exception.BuildNotFoundException;
import net.awired.visuwall.api.exception.ProjectNotFoundException;
import net.awired.visuwall.plugin.bamboo.BambooPlugin;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

public class BambooPluginIT {

    private static final String BAMBOO_ID = "BAMBOO_ID";

    static BambooPlugin bambooPlugin = new BambooPlugin();

    @BeforeClass
    public static void init() {
        bambooPlugin.setUrl(BAMBOO_URL);
        bambooPlugin.init();
    }

    @Test
    public void should_find_all_projects() {
        List<ProjectId> projects = bambooPlugin.findAllProjects();
        assertFalse(projects.isEmpty());
    }

    @Test
    public void should_find_project() throws ProjectNotFoundException {
        ProjectId projectId = struts2ProjectId();
        Project project = bambooPlugin.findProject(projectId);
        assertNotNull(project);
    }

    @Test
    public void should_find_last_build_number() throws ProjectNotFoundException, BuildNotFoundException {
        ProjectId projectId = strutsProjectId();
        int buildNumber = bambooPlugin.getLastBuildNumber(projectId);
        assertEquals(3, buildNumber);
    }

    @Test
    public void should_find_build_by_name_and_build_number() throws BuildNotFoundException, ProjectNotFoundException {
        ProjectId projectId = strutsProjectId();
        Build build = bambooPlugin.findBuildByBuildNumber(projectId, 3);
        assertNotNull(build);
        assertEquals(3, build.getBuildNumber());
        assertEquals(30181, build.getDuration());

        DateTime dateTime = new DateTime(2011, 4, 8, 19, 03, 45, 69);
        assertEquals(dateTime.toDate(), build.getStartTime());

        assertEquals(State.SUCCESS, build.getState());
        TestResult testResult = build.getTestResult();
        assertEquals(0, testResult.getFailCount());
        assertEquals(331, testResult.getPassCount());
    }


    @Test
    public void should_verify_not_building_project() throws ProjectNotFoundException {
        ProjectId projectId = strutsProjectId();
        boolean building = bambooPlugin.isBuilding(projectId);
        assertFalse(building);
    }

    @Test
    public void should_verify_success_state() throws ProjectNotFoundException {
        ProjectId projectId = strutsProjectId();
        State state = bambooPlugin.getState(projectId);
        assertEquals(State.SUCCESS, state);
    }

    @Test
    public void should_verify_failure_state() throws ProjectNotFoundException {
        ProjectId projectId = struts2ProjectId();
        State state = bambooPlugin.getState(projectId);
        assertEquals(State.FAILURE, state);
    }

    @Test
    public void should_populate_project() throws ProjectNotFoundException {
        ProjectId projectId = strutsProjectId();
        Project project = bambooPlugin.findProject(projectId);
        project.setProjectId(projectId);
        bambooPlugin.populate(project);
        assertEquals("struts", project.getName());
    }

    private ProjectId strutsProjectId() {
        ProjectId projectId = new ProjectId();
        projectId.addId(BAMBOO_ID, "STRUTS-STRUTS");
        return projectId;
    }

    private ProjectId struts2ProjectId() {
        ProjectId projectId = new ProjectId();
        projectId.addId(BAMBOO_ID, "STRUTS2INSTABLE-STRUTS2INSTABLE");
        return projectId;
    }
}
