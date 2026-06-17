package com.project.ieum.controller.admin;

import com.project.ieum.dto.admin.AdminMatchingRow;
import com.project.ieum.dto.admin.AdminUserRow;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ReviewVisibility;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.ReviewRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.admin.AdminDashboardService;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserService currentUserService;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    // ── 대시보드 ──────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("title", "대시보드");
        return "admin/dashboard";
    }

    // ── 회원 관리 ─────────────────────────────────────────────────
    @GetMapping("/users")
    public String userList(@RequestParam(required = false) String role,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        var pageable = PageRequest.of(page, 10);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasRole = role != null && !role.isBlank();

        var userPage = (hasRole && hasKeyword)
                ? userRepository.findByRoleAndEmailContainingAdmin(UserRole.valueOf(role), keyword, pageable)
                : hasKeyword
                ? userRepository.findByEmailContainingAdmin(keyword, pageable)
                : hasRole
                ? userRepository.findByRolePagedAdmin(UserRole.valueOf(role), pageable)
                : userRepository.findAllPagedAdmin(pageable);

        var users = userPage.getContent().stream().map(AdminUserRow::from).toList();
        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeMenu", "users");
        model.addAttribute("title", "회원 관리");
        addPageAttrs(model, userPage, page);
        return "admin/users/list";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        model.addAttribute("user", user);
        model.addAttribute("requests", helpRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().filter(r -> r.getRequester() != null
                        && r.getRequester().getUserId().equals(id)).toList());
        model.addAttribute("activeMenu", "users");
        model.addAttribute("title", "회원 상세");
        return "admin/users/detail";
    }

    @PostMapping("/users/{id}/status")
    public String changeUserStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   RedirectAttributes ra) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        user.changeStatus(UserStatus.valueOf(status));
        userRepository.save(user);
        ra.addFlashAttribute("message", "회원 상태가 변경되었습니다.");
        return "redirect:/admin/users/" + id;
    }

    // ── 매칭 관리 ─────────────────────────────────────────────────
    @GetMapping("/matching")
    public String matchingList(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        var pageable = PageRequest.of(page, 10);
        String resolvedStatus = status;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        org.springframework.data.domain.Page<com.project.ieum.entity.request.HelpRequest> requestPage;

        if (status != null && !status.isBlank()) {
            try {
                var statusEnum = HelpRequestStatus.valueOf(status);
                requestPage = hasKeyword
                        ? helpRequestRepository.findByStatusAndTitleContainingAdmin(statusEnum, keyword, pageable)
                        : helpRequestRepository.findByStatusPagedAdminWithRequester(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                requestPage = hasKeyword
                        ? helpRequestRepository.findByTitleContainingAdmin(keyword, pageable)
                        : helpRequestRepository.findAllPagedAdminWithRequester(pageable);
                resolvedStatus = null;
            }
        } else {
            requestPage = hasKeyword
                    ? helpRequestRepository.findByTitleContainingAdmin(keyword, pageable)
                    : helpRequestRepository.findAllPagedAdminWithRequester(pageable);
        }

        var requests = requestPage.getContent().stream().map(AdminMatchingRow::from).toList();
        model.addAttribute("requests", requests);
        model.addAttribute("selectedStatus", resolvedStatus);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeMenu", "matching");
        model.addAttribute("title", "매칭 관리");
        addPageAttrs(model, requestPage, page);
        return "admin/matching/list";
    }

    @GetMapping("/matching/{id}")
    public String matchingDetail(@PathVariable Long id, Model model) {
        model.addAttribute("request", helpRequestRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("요청을 찾을 수 없습니다.")));
        model.addAttribute("activeMenu", "matching");
        model.addAttribute("title", "매칭 상세");
        return "admin/matching/detail";
    }

    @PostMapping("/matching/{id}/close")
    public String forceClose(@PathVariable Long id, RedirectAttributes ra) {
        var request = helpRequestRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("요청을 찾을 수 없습니다."));
        request.forceCloseByAdmin();
        helpRequestRepository.save(request);
        ra.addFlashAttribute("message", "강제 종료되었습니다.");
        return "redirect:/admin/matching/" + id;
    }

    // ── 리뷰 관리 ─────────────────────────────────────────────────
    @GetMapping("/reviews")
    public String reviewList(@RequestParam(defaultValue = "0") int page, Model model) {
        var pageable = PageRequest.of(page, 10);
        var reviewPage = reviewRepository.findAllPagedAdmin(pageable);
        addPageAttrs(model, reviewPage, page);
        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("activeMenu", "reviews");
        model.addAttribute("title", "리뷰 관리");
        return "admin/reviews/list";
    }

    private void addPageAttrs(Model model, org.springframework.data.domain.Page<?> p, int page) {
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", p.getTotalPages());
        model.addAttribute("startPage", Math.max(0, page - 2));
        model.addAttribute("endPage", Math.min(p.getTotalPages() - 1, page + 2));
    }

    @PostMapping("/reviews/{id}/visibility")
    public String toggleVisibility(@PathVariable Long id,
                                   @RequestParam String visibility,
                                   RedirectAttributes ra) {
        var review = reviewRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("리뷰를 찾을 수 없습니다."));
        review.changeVisibility(ReviewVisibility.valueOf(visibility));
        reviewRepository.save(review);
        ra.addFlashAttribute("message", "리뷰 공개 상태가 변경되었습니다.");
        return "redirect:/admin/reviews";
    }
}
