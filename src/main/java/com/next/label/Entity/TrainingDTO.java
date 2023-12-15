package com.next.label.Entity;

import java.util.List;

public class TrainingDTO {
    private String bestPtDir;
    private String projectIdx;
    private List<String> trainClassNames;


    public String getBestPtDir() {
        return bestPtDir;
    }

    public void setBestPtDir(String bestPtDir) {
        this.bestPtDir = bestPtDir;
    }

    public String getProjectIdx() {
        return projectIdx;
    }

    public void setProjectIdx(String projectIdx) {
        this.projectIdx = projectIdx;
    }

    public List<String> getTrainClassNames() {
        return trainClassNames;
    }

    public void setTrainClassNames(List<String> trainClassNames) {
        this.trainClassNames = trainClassNames;
    }
}
