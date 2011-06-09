/**
 *     Copyright (C) 2010 Julien SMADJA <julien dot smadja at gmail dot com> - Arnaud LEMAIRE <alemaire at norad dot fr>
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

package net.awired.visuwall.teamcityclient;

import java.util.List;

import net.awired.visuwall.teamcityclient.builder.TeamCityUrlBuilder;
import net.awired.visuwall.teamcityclient.resource.TeamCityBuild;
import net.awired.visuwall.teamcityclient.resource.TeamCityProject;
import net.awired.visuwall.teamcityclient.resource.TeamCityProjects;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class TeamCityJerseyClient {

    private Client client;

	private TeamCityUrlBuilder urlBuilder;

	public TeamCityJerseyClient(Client client, TeamCityUrlBuilder urlBuilder) {
        this.client = client;
		this.urlBuilder = urlBuilder;
    }

    public List<TeamCityProject> getProjects() {
		String projectsUrl = urlBuilder.getProjects();
		WebResource resource = client.resource(projectsUrl);
        TeamCityProjects teamCityProjects = resource.get(TeamCityProjects.class);
		return teamCityProjects.getProjects();
    }

    public TeamCityProject getProject(String projectId) {
		String projectUrl = urlBuilder.getProject(projectId);
		WebResource resource = client.resource(projectUrl);
        TeamCityProject teamCityProject = resource.get(TeamCityProject.class);
        return teamCityProject;
    }

	public TeamCityBuild getBuild(int buildNumber) {
		String buildUrl = urlBuilder.getBuild(buildNumber);
		WebResource resource = client.resource(buildUrl);
		TeamCityBuild teamCityBuild = resource.get(TeamCityBuild.class);
		return teamCityBuild;
	}

}