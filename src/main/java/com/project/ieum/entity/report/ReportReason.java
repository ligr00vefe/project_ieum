package com.project.ieum.entity.report;

public enum ReportReason {
    ABUSE("욕설/폭언"),
    FRAUD("사기/금전 요구"),
    NO_SHOW("약속 불이행(노쇼)"),
    INAPPROPRIATE("부적절한 언행/행위"),
    ETC("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
