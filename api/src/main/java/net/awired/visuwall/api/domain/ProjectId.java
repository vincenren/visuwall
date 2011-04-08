package net.awired.visuwall.api.domain;

import java.util.HashMap;
import java.util.Map;

public class ProjectId {

    private final Map<String, String> ids = new HashMap<String, String>();

    private String name;

    private String artifactId;

    public void addId(String idName, String idValue) {
        ids.put(idName, idValue);
    }

    public String getId(String idName) {
        return ids.get(idName);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
}