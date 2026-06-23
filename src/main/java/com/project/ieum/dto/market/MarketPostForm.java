package com.project.ieum.dto.market;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MarketPostForm {

    @NotBlank(message = "상품명을 입력해주세요.")
    @Size(max = 120, message = "상품명은 120자 이하로 입력해주세요.")
    private String title;

    @Size(max = 5000, message = "상품 설명은 5000자 이하로 입력해주세요.")
    private String description;

    @NotNull(message = "가격을 입력해주세요.")
    @DecimalMin(value = "0", message = "가격은 0원 이상이어야 합니다.")
    private BigDecimal price;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Long categoryId;

    // ── 주소 필드 (카카오 주소 API가 채워줌 — HelpRequestForm과 완전히 동일한 구조) ──
    @NotBlank(message = "만남 장소를 입력해주세요.")
    private String roadAddress;

    private String addressDetail;

    @NotBlank
    private String sido;

    @NotBlank
    private String sigungu;

    private String bname;
    private String zonecode;

    // 이미지 파일 목록 (최대 5장, 필수 아님 — 이미지 없이도 등록 가능)
    private List<MultipartFile> images;

    // 수정 시 삭제할 기존 이미지 ID 목록
    private List<Long> deleteImageIds;
}