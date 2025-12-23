package com.mycompany.model;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
    "processId",
    "projectName",
    "engineName",
    "restartedFromCheckpoint",
    "trackingInfo",
    "customId"
})
public class ProcessContext {

    @XmlElement(name = "ProcessId", required = true)
    private long processId;

    @XmlElement(name = "ProjectName", required = true)
    private String projectName;

    @XmlElement(name = "EngineName", required = true)
    private String engineName;

    @XmlElement(name = "RestartedFromCheckpoint", required = true)
    private boolean restartedFromCheckpoint;

    @XmlElement(name = "TrackingInfo")
    private List<String> trackingInfo = new ArrayList<>();

    @XmlElement(name = "CustomId")
    private String customId;

    // getters / setters
    public long getProcessId() { return processId; }
    public void setProcessId(long processId) { this.processId = processId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getEngineName() { return engineName; }
    public void setEngineName(String engineName) { this.engineName = engineName; }

    public boolean isRestartedFromCheckpoint() { return restartedFromCheckpoint; }
    public void setRestartedFromCheckpoint(boolean restartedFromCheckpoint) {
        this.restartedFromCheckpoint = restartedFromCheckpoint;
    }

    public List<String> getTrackingInfo() { return trackingInfo; }

    public String getCustomId() { return customId; }
    public void setCustomId(String customId) { this.customId = customId; }
}
