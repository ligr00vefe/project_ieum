package com.project.ieum.entity.inquiry;

public enum InquiryCategory {
    MATCHING, ACCOUNT, SERVICE, ETC;

    public String label() {
        return switch (this) {
            case MATCHING -> "매칭";
            case ACCOUNT  -> "계정";
            case SERVICE  -> "서비스";
            case ETC      -> "기타";
        };
    }
}
