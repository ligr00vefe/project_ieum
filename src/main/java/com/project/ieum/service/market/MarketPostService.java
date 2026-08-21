package com.project.ieum.service.market;

import com.project.ieum.dto.market.MarketPostForm;
import com.project.ieum.dto.market.MarketPostSearchCondition;
import com.project.ieum.entity.User;
import com.project.ieum.entity.market.*;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.market.MarketCategoryRepository;
import com.project.ieum.repository.market.MarketChatRepository;
import com.project.ieum.repository.market.MarketPostImageRepository;
import com.project.ieum.repository.market.MarketPostRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.geocoding.GeoPoint;
import com.project.ieum.service.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import com.project.ieum.util.UploadSecurityValidator;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketPostService {

    // 한 게시글에 올릴 수 있는 이미지 최대 수
    private static final int MAX_IMAGE_COUNT = 5;

    // 이미지 저장 경로 (WebMvcConfig에서 이 경로를 정적 리소스로 등록해야 함 — Phase 6에서 처리)
    private static final String UPLOAD_DIR = "uploads/market/";

    private final MarketPostRepository marketPostRepository;
    private final MarketPostImageRepository marketPostImageRepository;
    private final MarketChatRepository marketChatRepository;
    private final MarketCategoryRepository marketCategoryRepository; // Phase2에서 누락됨, 아래 참고
    private final CurrentUserService currentUserService;
    private final GeocodingService geocodingService; // 기존 TmapGeocodingService 재사용

    // ── 게시글 등록 ──
    // HelpRequestService.create()와 동일한 패턴: 지오코딩 → 엔티티 저장 → 이미지 저장
    public MarketPost create(MarketPostForm form) {
        User currentUser = currentUserService.getCurrentUser();

        // 이미지 장수 검증 (5장 초과 방지)
        if (!CollectionUtils.isEmpty(form.getImages()) && form.getImages().size() > MAX_IMAGE_COUNT) {
            throw new BadRequestException("이미지는 최대 " + MAX_IMAGE_COUNT + "장까지 등록할 수 있습니다.");
        }

        // 카테고리 유효성 검증
        MarketCategory category = marketCategoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new BadRequestException("존재하지 않는 카테고리입니다."));

        // 지오코딩: 도로명주소 → 위도/경도
        // geocodingService는 기존 TmapGeocodingService가 주입됨 (변경 없음)
        // 좌표 확보 실패(Optional.empty)여도 게시글 등록은 허용 — 거리 정렬에서만 null 처리됨
        GeoPoint geoPoint = geocodingService.geocode(form.getRoadAddress()).orElse(null);

        // 나눔이면 가격 0 강제
        BigDecimal price = form.isSharing() ? BigDecimal.ZERO : form.getPrice();

        // 게시글 엔티티 저장
        // @Builder.Default로 status = ACTIVE 자동 설정
        MarketPost post = marketPostRepository.save(MarketPost.builder()
                .seller(currentUser)
                .category(category)
                .sharing(form.isSharing())
                .title(form.getTitle())
                .description(form.getDescription())
                .price(price)
                .roadAddress(form.getRoadAddress())
                .addressDetail(form.getAddressDetail())
                .sido(form.getSido())
                .sigungu(form.getSigungu())
                .bname(form.getBname())
                .zonecode(form.getZonecode())
                .latitude(geoPoint != null ? geoPoint.latitude() : null)
                .longitude(geoPoint != null ? geoPoint.longitude() : null)
                .build());

        // 이미지 저장 (파일 업로드 + DB 레코드 생성)
        if (!CollectionUtils.isEmpty(form.getImages())) {
            saveImages(post, form.getImages());
        }

        return post;
    }

    // ── 게시글 수정 ──
    // ACTIVE 상태에서만 수정 허용 (채팅 중 / 판매완료된 게시글은 수정 불가)
    public void update(Long postId, MarketPostForm form) {
        User currentUser = currentUserService.getCurrentUser();
        MarketPost post = getOwnedPost(postId, currentUser.getId());

        if (post.getStatus() != MarketPostStatus.ACTIVE) {
            throw new IllegalStateException("판매중 상태의 게시글만 수정할 수 있습니다.");
        }

        BigDecimal updatePrice = form.isSharing() ? BigDecimal.ZERO : form.getPrice();
        post.update(form.getTitle(), form.getDescription(), updatePrice, form.isSharing());

        // 기존 이미지 삭제
        if (!CollectionUtils.isEmpty(form.getDeleteImageIds())) {
            for (Long imgId : form.getDeleteImageIds()) {
                marketPostImageRepository.findById(imgId).ifPresent(img -> {
                    deleteImageFile(img.getImageUrl());
                    marketPostImageRepository.delete(img);
                });
            }
        }

        // 새 이미지 추가 (기존 + 신규 합산 5장 초과 방지)
        if (!CollectionUtils.isEmpty(form.getImages())) {
            int existing = marketPostImageRepository.findByPost_IdOrderByDisplayOrderAsc(postId).size();
            int incoming = (int) form.getImages().stream().filter(f -> f != null && !f.isEmpty()).count();
            if (existing + incoming > MAX_IMAGE_COUNT) {
                throw new BadRequestException("이미지는 최대 " + MAX_IMAGE_COUNT + "장까지 등록할 수 있습니다.");
            }
            saveImages(post, form.getImages());
        }
    }

    // ── 게시글 삭제 (soft delete) ──
    // REMOVED 상태로 전환 + 진행 중인 채팅방 모두 종료
    public void remove(Long postId) {
        User currentUser = currentUserService.getCurrentUser();
        MarketPost post = getOwnedPost(postId, currentUser.getId());
        post.remove();
        marketChatRepository.findByPost_Id(postId).forEach(MarketChat::close);
    }

    // ── 상태 변경: 예약 처리 ──
    public void reserve(Long postId) {
        User currentUser = currentUserService.getCurrentUser();
        MarketPost post = getOwnedPost(postId, currentUser.getId());
        post.reserve();
    }

    // ── 상태 변경: 예약 취소 ──
    public void cancelReservation(Long postId) {
        User currentUser = currentUserService.getCurrentUser();
        MarketPost post = getOwnedPost(postId, currentUser.getId());
        post.cancelReservation();
    }

    // ── 목록 조회 (동적 검색 + 페이지네이션) ──
    // 기본적으로 ACTIVE 상태만 표시 (REMOVED 제외는 SearchRepositoryImpl에서 처리)
    @Transactional(readOnly = true)
    public Page<MarketPost> search(MarketPostSearchCondition condition, Pageable pageable,
                                   Double lat, Double lng) {
        // 기본 상태를 ACTIVE로 고정 (검색 조건에 상태가 없으면 전체 판매중만 노출)
        if (condition.getStatus() == null) {
            condition.setStatus(MarketPostStatus.ACTIVE);
        }
        return marketPostRepository.searchMarketPosts(condition, pageable, lat, lng);
    }

    // ── 상세 조회 ──
    @Transactional(readOnly = true)
    public MarketPost getDetail(Long postId) {
        return marketPostRepository.findWithDetailById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    // ── 상세 조회 + 이미지 목록 함께 반환 ──
    @Transactional(readOnly = true)
    public List<MarketPostImage> getImages(Long postId) {
        return marketPostImageRepository.findByPost_IdOrderByDisplayOrderAsc(postId);
    }

    // ── 내가 등록한 게시글 목록 ──
    @Transactional(readOnly = true)
    public List<MarketPost> getMyPosts() {
        User currentUser = currentUserService.getCurrentUser();
        return marketPostRepository.findBySellerAndStatusNotInOrderByCreatedAtDesc(
                currentUser, java.util.List.of(MarketPostStatus.REMOVED, MarketPostStatus.SOLD));
    }

    // ── 카테고리 목록 조회 (등록 폼에서 드롭다운용) ──
    @Transactional(readOnly = true)
    public List<MarketCategory> getAllCategories() {
        return marketCategoryRepository.findAll();
    }

    // ──────────────────────────────────────────────
    // private 헬퍼 메서드
    // ──────────────────────────────────────────────

    // 이미지 파일 저장 + DB 레코드 생성
    // UserService.saveCustomProfileImage() 패턴 참고
    private void saveImages(MarketPost post, List<MultipartFile> files) {
        // 업로드 폴더 없으면 생성
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // 빈 파일(파일을 선택하지 않은 input) 스킵
            if (file == null || file.isEmpty()) continue;
            UploadSecurityValidator.validateImage(file);

            // 파일 확장자 추출 (예: "jpg", "png")
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            // 고유 파일명 생성: 타임스탬프_postId_순서.확장자
            // 예: 1720000000000_1_0.jpg
            String fileName = System.currentTimeMillis() + "_" + post.getId() + "_" + i + ext;
            Path savePath = Paths.get(UPLOAD_DIR + fileName);

            try {
                Files.copy(file.getInputStream(), savePath);
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 중 오류가 발생했습니다.", e);
            }

            // DB에 이미지 경로와 순서 저장
            marketPostImageRepository.save(MarketPostImage.builder()
                    .post(post)
                    .imageUrl("/uploads/market/" + fileName) // 브라우저에서 접근할 URL
                    .displayOrder(i)                         // 0번이 대표 이미지
                    .build());
        }
    }

    // 이미지 파일 삭제
    private void deleteImageFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/market/")) return;
        String filename = imageUrl.substring("/uploads/market/".length());
        try {
            java.nio.file.Files.deleteIfExists(Paths.get(UPLOAD_DIR + filename));
        } catch (java.io.IOException e) {
            // 파일 삭제 실패는 무시 (DB 레코드 삭제는 계속 진행)
        }
    }

    // 게시글 조회 + 소유자 검증
    private MarketPost getOwnedPost(Long postId, Long userId) {
        MarketPost post = marketPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
        if (!post.getSeller().getId().equals(userId)) {
            throw new ForbiddenException("본인의 게시글만 수정/삭제할 수 있습니다.");
        }
        return post;
    }
}
