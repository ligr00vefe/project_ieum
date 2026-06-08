package com.project.ieum.dto;

import com.project.ieum.entity.UserRole;
import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationSessionDTO {

    private UserRole userType;

    private Integer currentStep;

    private BasicInfoDTO basicInfo;

    private DisabilityInfoDTO disabilityInfo;

    private CommunicationDTO communicationInfo;

    private CertificationDTO certificationInfo;

    private ActivityInfoDTO activityInfo;

    private PersonalityTagDTO personalityTags;

    public void incrementStep() {
        this.currentStep++;
    }

    public void setStep(int step) {
        this.currentStep = step;
    }
}
