package com.project.ieum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationDTO {

    @NotNull(message = "자격증 보유 여부는 필수입니다")
    private Boolean hasCertification;

    private String certificationType;
}
