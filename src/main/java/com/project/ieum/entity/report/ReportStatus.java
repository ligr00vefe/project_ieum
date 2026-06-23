package com.project.ieum.entity.report;

public enum ReportStatus {
    RECEIVED("접수"),
    REVIEWING("검토 중"),
    RESOLVED("처리 완료"),
    REJECTED("반려");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
