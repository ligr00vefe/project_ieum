package com.project.ieum.entity;

public enum MbtiType {
    INTJ, INTP, ENTJ, ENTP,
    INFJ, INFP, ENFJ, ENFP,
    ISTJ, ISFJ, ESTJ, ESFJ,
    ISTP, ISFP, ESTP, ESFP;

    public String getLabel() {
        return this.name();
    }
}
