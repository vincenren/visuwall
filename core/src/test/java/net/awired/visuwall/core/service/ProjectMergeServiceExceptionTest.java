package net.awired.visuwall.core.service;

import net.awired.visuwall.api.domain.Project;
import net.awired.visuwall.api.plugin.BuildConnectionPlugin;
import net.awired.visuwall.core.service.ProjectMergeService;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;


public class ProjectMergeServiceExceptionTest {

    ProjectMergeService projectMergeService = new ProjectMergeService();

    @Ignore
    @Test(expected = IllegalStateException.class)
    public void should_merge_one_project_with_one_build_plugin() {
        Project project = new Project("test");
        BuildConnectionPlugin buildPlugin = Mockito.mock(BuildConnectionPlugin.class);
        projectMergeService.merge(project, buildPlugin);
    }

}