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
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        // 좌표는 이후 Phase 5(프론트)에서 Geolocation API로 받아 전달 — 지금은 null로 처리
        Page<MarketPost> posts = marketPostService.search(condition, pageable, null, null);

        // 각 게시글의 대표 이미지(0번) URL 조회 후 DTO 변환
        Page<MarketPostResponse> responses = posts.map(post -> {
            List<MarketPostImage> images = marketPostService.getImages(post.getId());
            String thumbnail = images.isEmpty() ? null : images.get(0).getImageUrl();
            return MarketPostResponse.from(post, thumbnail);
        });

        model.addAttribute("title", "이음 마켓");
        model.addAttribute("posts", responses);
        model.addAttribute("condition", condition);
        model.addAttribute("categories", marketPostService.getAllCategories());
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
        try {
            User currentUser = currentUserService.getCurrentUser();
            isSeller = post.getSeller().getId().equals(currentUser.getId());
        } catch (Exception ignored) {
            // 비로그인 접근 허용 — 상세 보기는 누구나 가능
        }

        int chatCount = marketChatService.getChatCountByPost(postId);

        model.addAttribute("title", post.getTitle());
        model.addAttribute("post", MarketPostResponse.from(post, thumbnail, chatCount));
        model.addAttribute("images", images);   // 슬라이드용 전체 이미지 목록
        model.addAttribute("isSeller", isSeller);
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