package net.awired.visuwall.core.business.process.capabilities;

import java.util.Date;
import java.util.List;
import net.awired.visuwall.api.domain.SoftwareProjectId;
import net.awired.visuwall.api.domain.State;
import net.awired.visuwall.api.exception.BuildNotFoundException;
import net.awired.visuwall.api.exception.BuildNumberNotFoundException;
import net.awired.visuwall.api.exception.ProjectNotFoundException;
import net.awired.visuwall.core.business.domain.Build;
import net.awired.visuwall.core.business.domain.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import com.google.common.base.Preconditions;

@Component
public class BuildCapabilityProcess {

    private static final Logger LOG = LoggerFactory.getLogger(BuildCapabilityProcess.class);

    @Autowired
    TaskScheduler scheduler;

    public void updatePreviousCompletedBuild(Project project) throws ProjectNotFoundException {
        List<Integer> buildNumbers = project.getBuildNumbers();
        if (buildNumbers.size() < 2) {
            return;
        }

        List<Integer> previousBuilds = buildNumbers.subList(1, buildNumbers.size() - 1);
        for (Integer buildNumber : previousBuilds) {
            Build build = project.getBuilds().get(buildNumber);
            if (build == null) {
                updateBuild(project, buildNumber);
                build = project.getBuilds().get(buildNumber);
                if (build == null) {
                    LOG.warn("Build " + buildNumber + " not found after update for project " + project);
                    continue;
                }
            }

            State state = build.getState();
            if (state == State.UNKNOWN || state == State.ABORTED || state == State.NOTBUILT) {
                continue;
            }

            project.setLastCompletedBuildNumber(build.getBuildNumber());
            break;
        }
    }

    public void updateBuild(Project project, Integer buildNumber) throws ProjectNotFoundException {
        try {
            LOG.info("Updating build " + buildNumber + " for project " + project);
            SoftwareProjectId projectId = project.getBuildProjectId();
            Build build = project.findCreatedBuild(buildNumber);

            State state = project.getBuildConnection().getBuildState(projectId, buildNumber);

            build.setState(state);

            //TODO why is it old state ?
            //            boolean building = project.getBuildConnection().isBuilding(projectId, buildNumber);
            //            build.setBuilding(building);
            //            if (building == true) {
            //                Date estimatedFinishTime = project.getBuildConnection()
            //                        .getEstimatedFinishTime(projectId, buildNumber);
            //                build.setEstimatedFinishTime(estimatedFinishTime);
            //            }

            // buildTime

            project.findCreatedBuild(buildNumber);
        } catch (BuildNotFoundException e) {
            LOG.warn("BuildNumber " + buildNumber + " not found in software to update project " + project, e);
            //TODO remove buildNumber from buildNumbers as its removed from software
        }
    }

    public int[] updateStatusAndReturnBuildsToUpdate(Project project) throws ProjectNotFoundException,
            BuildNotFoundException {
        try {
            int lastBuildNumber = project.getBuildConnection().getLastBuildNumber(project.getBuildProjectId());
            boolean lastBuilding = project.getBuildConnection().isBuilding(project.getBuildProjectId(),
                    lastBuildNumber);

            int previousLastBuildNumber = project.getLastBuildNumber();
            boolean previousBuilding = false;
            try {
                previousBuilding = project.getLastBuild().isBuilding();
            } catch (BuildNotFoundException e) {
                LOG.debug("No lastBuild found to say the project was building before refresh " + project);
            }

            Build lastBuild = project.findCreatedBuild(lastBuildNumber);

            try {
                if (previousBuilding == false && lastBuilding == false && previousLastBuildNumber != lastBuildNumber) {
                    LOG.info("there is an already finished new build {}  {}", lastBuildNumber, project);
                    return new int[] { lastBuildNumber };
                }
                if (previousBuilding == false && lastBuilding == true) {
                    LOG.info("Build {} is now running {}", lastBuild.getBuildNumber(), project);
                    Runnable finishTimeRunner = getEstimatedFinishTimeRunner(project, lastBuild);
                    scheduler.schedule(finishTimeRunner, new Date());
                }
                if (previousBuilding == true && lastBuilding == true) {
                    if (previousLastBuildNumber != lastBuildNumber) {
                        LOG.info("Previous build {} is over and a new build {} is already running {}", new Object[] {
                                previousLastBuildNumber, lastBuildNumber, project });
                        project.getBuilds().get(previousLastBuildNumber).setEstimatedFinishTime(null);
                        project.getBuilds().get(previousLastBuildNumber).setBuilding(false);
                        Runnable finishTimeRunner = getEstimatedFinishTimeRunner(project, lastBuild);
                        scheduler.schedule(finishTimeRunner, new Date());
                        return new int[] { previousLastBuildNumber };
                    } else {
                        // building is still running
                    }
                }

                if (previousBuilding == true && lastBuilding == false) {
                    // build is over
                    project.getBuilds().get(previousLastBuildNumber).setEstimatedFinishTime(null);
                    project.getBuilds().get(previousLastBuildNumber).setBuilding(false);
                    if (lastBuildNumber != previousLastBuildNumber) {
                        LOG.info("previous build {} is over and a new build {} is also over {}", new Object[] {
                                previousLastBuildNumber, lastBuildNumber, project });
                        return new int[] { previousLastBuildNumber, lastBuildNumber };
                    } else {
                        LOG.info("Previous build {} is over and no new build ", previousLastBuildNumber, project);
                        return new int[] { previousLastBuildNumber };
                    }
                }
            } finally {
                lastBuild.setBuilding(lastBuilding);
                project.setLastBuildNumber(lastBuildNumber);
            }
        } catch (BuildNumberNotFoundException e) {
            LOG.info("No last build number found to update project " + project);
        }
        return new int[] {};
    }

    ////////////////////////////////////////////////////////////////////////

    /**
     * @return null if no date could be estimated
     * @throws ProjectNotFoundException
     */
    Runnable getEstimatedFinishTimeRunner(final Project project, final Build build) throws ProjectNotFoundException {
        Preconditions.checkNotNull(project, "project is a mandatory parameter");
        return new Runnable() {
            @Override
            public void run() {
                LOG.info("Running getEstimatedFinishTime for project " + project);
                try {
                    Date estimatedFinishTime = project.getBuildConnection().getEstimatedFinishTime(
                            project.getBuildProjectId(), build.getBuildNumber());
                    if (estimatedFinishTime != null) {
                        build.setEstimatedFinishTime(estimatedFinishTime);
                    }
                } catch (ProjectNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (BuildNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
    }

}
