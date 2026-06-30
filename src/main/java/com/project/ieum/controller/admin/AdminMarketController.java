package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.AdminMarketChatRow;
import com.project.ieum.dto.admin.AdminMarketPostRow;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.market.MarketChatRepository;
import com.project.ieum.repository.market.MarketPostImageRepository;
import com.project.ieum.repository.market.MarketPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/market")
@RequiredArgsConstructor
public class AdminMarketController {

    private final MarketPostRepository marketPostRepository;
    private final MarketPostImageRepository marketPostImageRepository;
    private final MarketChatRepository marketChatRepository;

    // ── 거래 관리 목록 ─────────────────────────────────────────────
    @GetMapping("/posts")
    public String posts(
            @RequestParam(required = false) Boolean sharing,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        MarketPostStatus statusEnum = parseStatus(status);
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;
        String kw = blankToNull(keyword);

        var pageable = PageRequest.of(page, 20);
        Page<AdminMarketPostRow> posts = marketPostRepository
                .findAdminPosts(sharing, statusEnum, kw, fromDt, toDt, pageable)
                .map(AdminMarketPostRow::from);

        model.addAttribute("posts", posts);
        model.addAttribute("sharing", sharing);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageBaseUrl", buildPostsBaseUrl(sharing, status, keyword, from, to));
        model.addAttribute("activeMenu", "market-posts");
        model.addAttribute("title", "거래 관리");
        addPageAttrs(model, posts, page);
        return "admin/market/posts";
    }

    // ── 거래 관리 상세 ─────────────────────────────────────────────
    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        MarketPost post = marketPostRepository.findWithDetailById(id)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));

        var images = marketPostImageRepository.findByPost_IdOrderByDisplayOrderAsc(id);
        var chats  = marketChatRepository.findByPostIdForAdmin(id);

        model.addAttribute("post", post);
        model.addAttribute("images", images);
        model.addAttribute("chats", chats);
        model.addAttribute("activeMenu", "market-posts");
        model.addAttribute("title", "거래 상세");
        return "admin/market/detail";
    }

    // ── 관리자 상품 삭제 (soft delete) ─────────────────────────────
    @PostMapping("/posts/{id}/remove")
    public String removePost(@PathVariable Long id, RedirectAttributes ra) {
        MarketPost post = marketPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));
        if (post.getStatus() != MarketPostStatus.REMOVED && post.getStatus() != MarketPostStatus.SOLD) {
            post.remove();
            marketPostRepository.save(post);
            marketChatRepository.findByPost_Id(id).forEach(c -> {
                c.close();
                marketChatRepository.save(c);
            });
            ra.addFlashAttribute("message", "상품이 삭제 처리되었습니다.");
        } else {
            ra.addFlashAttribute("error", "이미 삭제됨 또는 판매완료 상태입니다.");
        }
        return "redirect:/admin/market/posts/" + id;
    }

    // ── 채팅 관리 목록 ─────────────────────────────────────────────
    @GetMapping("/chats")
    public String chats(
            @RequestParam(required = false) Boolean sharing,
            @RequestParam(required = false) String postStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        MarketPostStatus statusEnum = parseStatus(postStatus);
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;
        String kw = blankToNull(keyword);

        var pageable = PageRequest.of(page, 20);
        Page<AdminMarketChatRow> chats = marketChatRepository
                .findAdminChats(sharing, statusEnum, kw, fromDt, toDt, pageable)
                .map(AdminMarketChatRow::from);

        model.addAttribute("chats", chats);
        model.addAttribute("sharing", sharing);
        model.addAttribute("selectedStatus", postStatus);
        model.addAttribute("keyword", keyword);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageBaseUrl", buildChatsBaseUrl(sharing, postStatus, keyword, from, to));
        model.addAttribute("activeMenu", "market-chats");
        model.addAttribute("title", "채팅 관리");
        addPageAttrs(model, chats, page);
        return "admin/market/chats";
    }

    // ── helpers ──
    private MarketPostStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return MarketPostStatus.valueOf(s); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private void addPageAttrs(Model model, Page<?> p, int page) {
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", p.getTotalPages());
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(p.getTotalPages() - 1, page + 2));
    }

    /** 페이지네이션 base URL — page 파라미터만 빼고 모든 필터를 안전하게 인코딩 */
    private String buildPostsBaseUrl(Boolean sharing, String status, String keyword,
                                     LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("/admin/market/posts?");
        if (sharing != null)             sb.append("sharing=").append(sharing).append('&');
        if (status  != null && !status.isBlank())  sb.append("status=").append(enc(status)).append('&');
        if (keyword != null && !keyword.isBlank()) sb.append("keyword=").append(enc(keyword)).append('&');
        if (from    != null)             sb.append("from=").append(from).append('&');
        if (to      != null)             sb.append("to=").append(to).append('&');
        return sb.toString();
    }

    private String buildChatsBaseUrl(Boolean sharing, String postStatus, String keyword,
                                     LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("/admin/market/chats?");
        if (sharing    != null)                       sb.append("sharing=").append(sharing).append('&');
        if (postStatus != null && !postStatus.isBlank()) sb.append("postStatus=").append(enc(postStatus)).append('&');
        if (keyword    != null && !keyword.isBlank())    sb.append("keyword=").append(enc(keyword)).append('&');
        if (from       != null)                       sb.append("from=").append(from).append('&');
        if (to         != null)                       sb.append("to=").append(to).append('&');
        return sb.toString();
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
