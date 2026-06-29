package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketPostForm;
import com.project.ieum.dto.market.MarketPostResponse;
import com.project.ieum.dto.market.MarketPostSearchCondition;
import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/market")
public class MarketController {

    private final MarketPostService marketPostService;
    private final MarketChatService marketChatService;
    private final CurrentUserService currentUserService;

    // ── 게시글 목록 ──
    // GET /market
    // 기존 HelpRequestController.list()와 동일한 구조
    @GetMapping
    public String list(
            MarketPostSearchCondition condition,   // 쿼리 파라미터 자동 바인딩 (?keyword=&sido=&categoryId=)
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {

        // 판매/나눔 기본값: 파라미터 없으면 판매(false)만 표시
        if (condition.getSharing() == null) condition.setSharing(false);

        // 좌표는 이후 Phase 5(프론트)에서 Geolocation API로 받아 전달 — 지금은 null로 처리
        Page<MarketPost> posts = marketPostService.search(condition, pageable, null, null);

        // 각 게시글의 대표 이미지(0번) URL 조회 후 DTO 변환
        Page<MarketPostResponse> responses = posts.map(post -> {
            List<MarketPostImage> images = marketPostService.getImages(post.getId());
            String thumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();
            return MarketPostResponse.from(post, thumbnail);
        });

        // 현재 로그인 사용자 ID (비로그인은 null — 목록에서 내 상품 구분용)
        Long currentUserId = null;
        try {
            currentUserId = currentUserService.getCurrentUser().getId();
        } catch (Exception ignored) {}

        // 페이지네이션 URL (sharing + 기타 필터 파라미터 보존)
        StringBuilder pageUrlBuilder = new StringBuilder("/market?sharing=").append(condition.getSharing()).append("&");
        if (condition.getCategoryId() != null) pageUrlBuilder.append("categoryId=").append(condition.getCategoryId()).append("&");
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank())
            pageUrlBuilder.append("keyword=").append(java.net.URLEncoder.encode(condition.getKeyword(), java.nio.charset.StandardCharsets.UTF_8)).append("&");
        if (condition.getMinPrice() != null) pageUrlBuilder.append("minPrice=").append(condition.getMinPrice()).append("&");
        if (condition.getMaxPrice() != null) pageUrlBuilder.append("maxPrice=").append(condition.getMaxPrice()).append("&");

        int currentPage = responses.getNumber();
        int totalPages  = responses.getTotalPages();

        model.addAttribute("title", "이음 마켓");
        model.addAttribute("description", "이음 마켓에서 돌봄·복지 관련 용품을 사고팔거나 나눔하세요. 휠체어, 보조기기 등 다양한 중고 물품을 만나보세요.");
        model.addAttribute("posts", responses);
        model.addAttribute("condition", condition);
        model.addAttribute("paginationUrl", pageUrlBuilder.toString());
        model.addAttribute("categories", marketPostService.getAllCategories());
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("startPage", Math.max(0, currentPage - 2));
        model.addAttribute("endPage", Math.min(totalPages - 1, currentPage + 2));

        // 로그인 사용자의 상품을 채팅 수 포함해 별도로 조회 (내 상품 섹션 우선 표시용)
        List<MarketPostResponse> myPosts = new ArrayList<>();
        if (currentUserId != null) {
            myPosts = marketPostService.getMyPosts().stream()
                    .filter(p -> condition.getSharing() == null || p.isSharing() == condition.getSharing())
                    .map(p -> {
                        List<MarketPostImage> imgs = marketPostService.getImages(p.getId());
                        String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                        int cnt = marketChatService.getChatCountByPost(p.getId());
                        return MarketPostResponse.from(p, thumb, cnt);
                    })
                    .toList();
        }
        model.addAttribute("myPosts", myPosts);

        return "market/list";  // templates/market/list.html
    }

    // ── 게시글 상세 ──
    // GET /market/{postId}
    @GetMapping("/{postId}")
    public String detail(@PathVariable Long postId, Model model) {
        MarketPost post = marketPostService.getDetail(postId);
        List<MarketPostImage> images = marketPostService.getImages(postId);
        String thumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();

        // 현재 로그인 사용자가 판매자인지 여부 — 템플릿에서 수정/삭제 버튼 표시 여부 결정
        boolean isSeller = false;
        User currentUser = null;
        try {
            currentUser = currentUserService.getCurrentUser();
            isSeller = post.getSeller().getId().equals(currentUser.getId());
        } catch (Exception ignored) {
            // 비로그인 접근 허용 — 상세 보기는 누구나 가능
        }

        int chatCount = marketChatService.getChatCountByPost(postId);

        model.addAttribute("title", post.getTitle());
        model.addAttribute("description", post.getTitle() + " — 이음 마켓 중고 물품 상세 페이지입니다.");
        model.addAttribute("post", MarketPostResponse.from(post, thumbnail, chatCount));
        model.addAttribute("images", images);   // 슬라이드용 전체 이미지 목록
        model.addAttribute("isSeller", isSeller);
        model.addAttribute("currentUser", currentUser);
        return "market/detail";  // templates/market/detail.html
    }

    // ── 게시글 등록 폼 ──
    // GET /market/new
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "상품 등록");
        model.addAttribute("form", new MarketPostForm());
        model.addAttribute("categories", marketPostService.getAllCategories());
        return "market/form";  // templates/market/form.html
    }

    // ── 게시글 등록 처리 ──
    // POST /market
    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") MarketPostForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // 검증 실패 시 폼 다시 표시 — 카테고리 목록도 다시 넘겨줘야 함
            model.addAttribute("title", "상품 등록");
            model.addAttribute("categories", marketPostService.getAllCategories());
            return "market/form";
        }

        MarketPost post = marketPostService.create(form);
        redirectAttributes.addFlashAttribute("message", "상품이 등록되었습니다.");
        return "redirect:/market/" + post.getId();
    }

    // ── 게시글 수정 폼 ──
    // GET /market/{postId}/edit
    @GetMapping("/{postId}/edit")
    public String editForm(@PathVariable Long postId, Model model) {
        MarketPost post = marketPostService.getDetail(postId);

        // 폼에 기존 값 프리필
        MarketPostForm form = new MarketPostForm();
        form.setSharing(post.isSharing());
        form.setTitle(post.getTitle());
        form.setDescription(post.getDescription());
        form.setPrice(post.getPrice());
        form.setCategoryId(post.getCategory().getId());
        form.setRoadAddress(post.getRoadAddress());
        form.setAddressDetail(post.getAddressDetail());
        form.setSido(post.getSido());
        form.setSigungu(post.getSigungu());
        form.setBname(post.getBname());
        form.setZonecode(post.getZonecode());

        // 기존 이미지 목록 (id + url) — 수정 폼에서 미리보기 및 삭제 처리용
        List<MarketPostImage> images = marketPostService.getImages(postId);
        List<java.util.Map<String, Object>> existingImages = images.stream()
                .map(img -> java.util.Map.<String, Object>of("id", img.getId(), "url", img.getImageUrl()))
                .toList();

        model.addAttribute("title", "상품 수정");
        model.addAttribute("form", form);
        model.addAttribute("postId", postId);
        model.addAttribute("existingImages", existingImages);
        model.addAttribute("categories", marketPostService.getAllCategories());
        return "market/form";
    }

    // ── 게시글 수정 처리 ──
    // POST /market/{postId}/edit
    @PostMapping("/{postId}/edit")
    public String update(
            @PathVariable Long postId,
            @Valid @ModelAttribute("form") MarketPostForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "상품 수정");
            model.addAttribute("postId", postId);
            model.addAttribute("categories", marketPostService.getAllCategories());
            return "market/form";
        }

        marketPostService.update(postId, form);
        redirectAttributes.addFlashAttribute("message", "수정되었습니다.");
        return "redirect:/market/" + postId;
    }

    // ── 게시글 삭제 ──
    // POST /market/{postId}/remove
    @PostMapping("/{postId}/remove")
    public String remove(@PathVariable Long postId, RedirectAttributes redirectAttributes) {
        marketPostService.remove(postId);
        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/market/my";
    }

    // ── 예약 처리 ──
    // POST /market/{postId}/reserve
    @PostMapping("/{postId}/reserve")
    public String reserve(@PathVariable Long postId, RedirectAttributes redirectAttributes) {
        marketPostService.reserve(postId);
        redirectAttributes.addFlashAttribute("message", "예약 처리되었습니다.");
        return "redirect:/market/" + postId;
    }

    // ── 예약 취소 ──
    // POST /market/{postId}/cancel-reserve
    @PostMapping("/{postId}/cancel-reserve")
    public String cancelReservation(@PathVariable Long postId, RedirectAttributes redirectAttributes) {
        marketPostService.cancelReservation(postId);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        return "redirect:/market/" + postId;
    }

    // ── 무한 스크롤용 REST API ──
    // GET /api/market/posts  (CSRF 비활성화 경로 — SecurityConfig 참고)
    @GetMapping("/api/posts")
    @ResponseBody
    public org.springframework.data.domain.Page<MarketPostResponse> apiList(
            MarketPostSearchCondition condition,
            @PageableDefault(size = 3) Pageable pageable) {

        if (condition.getSharing() == null) condition.setSharing(false);
        Page<MarketPost> posts = marketPostService.search(condition, pageable, null, null);
        Long currentUserId = null;
        try { currentUserId = currentUserService.getCurrentUser().getId(); } catch (Exception ignored) {}
        final Long uid = currentUserId;
        return posts.map(post -> {
            List<MarketPostImage> images = marketPostService.getImages(post.getId());
            String thumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();
            MarketPostResponse r = MarketPostResponse.from(post, thumbnail);
            return r;
        });
    }

    // ── 내 게시글 목록 ──
    // GET /my/market
    @GetMapping("/my")
    public String myPosts(Model model) {
        model.addAttribute("title", "내 판매 목록");
        List<MarketPost> posts = marketPostService.getMyPosts();
        List<MarketPostResponse> responses = posts.stream()
                .map(post -> {
                    List<MarketPostImage> images = marketPostService.getImages(post.getId());
                    String thumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();
                    return MarketPostResponse.from(post, thumbnail);
                })
                .toList();
        model.addAttribute("posts", responses);
        return "market/my-list";  // templates/market/my-list.html
    }
}