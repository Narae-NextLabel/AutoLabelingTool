package com.next.label.Entity;

import java.util.List;

public class predictDTO{
        private String projectIdx;
        private String resultFolderName;
        private String userName;
        private List<String> classNames;
        private List<String> imageNames;

    public String getProjectIdx() {
        return projectIdx;
    }

    public void setProjectIdx(String projectIdx) {
        this.projectIdx = projectIdx;
    }

    public String getResultFolderName() {
        return resultFolderName;
    }

    public void setResultFolderName(String resultFolderName) {
        this.resultFolderName = resultFolderName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }

    public List<String> getImageNames() {
        return imageNames;
    }

    public void setImageNames(List<String> imageNames) {
        this.imageNames = imageNames;
    }
}
