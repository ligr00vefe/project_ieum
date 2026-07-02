package com.project.ieum.controller.admin;

import com.project.ieum.dto.CaregiverEditDTO;
import com.project.ieum.dto.DisabledEditDTO;
import com.project.ieum.dto.admin.AdminMatchingRow;
import com.project.ieum.dto.admin.AdminUserRow;
import com.project.ieum.entity.MbtiType;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.profile.Profile;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ReviewVisibility;
import com.project.ieum.repository.CaregiverPersonalityTagRepository;
import com.project.ieum.repository.CaregiverProfileRepository;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.ReviewRepository;
import com.project.ieum.repository.UserPersonalityTagRepository;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.UserService;
import com.project.ieum.service.admin.AdminDashboardService;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserService currentUserService;
    private final UserProfileRepository userProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final UserPersonalityTagRepository userPersonalityTagRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final UserService userService;
    private final MasterDataService masterDataService;

    @Value("${tmap.app-key:}")
    private String tmapAppKey;

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
                           @RequestParam(defaultValue = "false") boolean warnedOnly,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        var pageable = PageRequest.of(page, 10);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasRole = role != null && !role.isBlank();

        org.springframework.data.domain.Page<com.project.ieum.entity.User> userPage;
        if (warnedOnly) {
            userPage = hasKeyword
                    ? userRepository.findByStatusAndEmailContainingAdmin(UserStatus.WARNED, keyword, pageable)
                    : userRepository.findByStatusPagedAdmin(UserStatus.WARNED, pageable);
        } else {
            userPage = (hasRole && hasKeyword)
                    ? userRepository.findByRoleAndEmailContainingAdmin(UserRole.valueOf(role), keyword, pageable)
                    : hasKeyword
                    ? userRepository.findByEmailContainingAdmin(keyword, pageable)
                    : hasRole
                    ? userRepository.findByRolePagedAdmin(UserRole.valueOf(role), pageable)
                    : userRepository.findAllPagedAdmin(pageable);
        }

        var users = userPage.getContent().stream().map(AdminUserRow::from).toList();
        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role);
        model.addAttribute("keyword", keyword);
        model.addAttribute("warnedOnly", warnedOnly);
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
        addProfileAttrs(user, model);
        model.addAttribute("requests", helpRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().filter(r -> r.getRequester() != null
                        && r.getRequester().getUserId().equals(id)).toList());
        model.addAttribute("activeMenu", "users");
        model.addAttribute("title", "회원 상세");
        return "admin/users/detail";
    }

    // 역할별 구체 프로필을 조회해 뷰에 주입. User.profile은 LAZY 프록시라 instanceof 분기가
    // 불안정하므로 서브타입 리포지토리로 직접 조회한다. 성향 태그는 프로필 엔티티에 역방향 매핑이
    // 없어 전용 리포지토리로 별도 조회한다.
    private Profile addProfileAttrs(User user, Model model) {
        Profile profile = null;
        if (user.getRole() == UserRole.USER) {
            var p = userProfileRepository.findById(user.getId()).orElse(null);
            model.addAttribute("userProfile", p);
            if (p != null) {
                model.addAttribute("userPersonalityTags",
                        userPersonalityTagRepository.findByUser(p).stream()
                                .map(upt -> upt.getTag()).toList());
            }
            profile = p;
        } else if (user.getRole() == UserRole.CAREGIVER) {
            var p = caregiverProfileRepository.findById(user.getId()).orElse(null);
            model.addAttribute("caregiverProfile", p);
            if (p != null) {
                model.addAttribute("caregiverPersonalityTags",
                        caregiverPersonalityTagRepository.findByCaregiver(p).stream()
                                .map(cpt -> cpt.getTag()).toList());
            }
            profile = p;
        }
        model.addAttribute("profile", profile);
        return profile;
    }

    @GetMapping("/users/{id}/edit")
    public String userEditForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        model.addAttribute("user", user);
        addProfileAttrs(user, model);

        // 역할별 수정 폼에 현재 값(editDTO)과 마스터 데이터(선택지 목록) 주입.
        // 기존 마이페이지 수정 경로(UserService)를 그대로 재사용하므로 편집 범위가 일관된다.
        if (user.getRole() == UserRole.USER) {
            model.addAttribute("editDTO", userService.loadDisabledEditDTO(user));
            model.addAttribute("disabilityTypes", masterDataService.getAllDisabilityTypes());
            model.addAttribute("communicationMethods", masterDataService.getAllCommunicationMethods());
            model.addAttribute("personalityTags", masterDataService.getUserPersonalityTags());
            model.addAttribute("mbtiTypes", MbtiType.values());
        } else if (user.getRole() == UserRole.CAREGIVER) {
            model.addAttribute("editDTO", userService.loadCaregiverEditDTO(user));
            model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
            model.addAttribute("serviceCategories", masterDataService.getAllServiceCategories());
            model.addAttribute("mbtiTypes", MbtiType.values());
        }

        model.addAttribute("activeMenu", "users");
        model.addAttribute("title", "회원 정보 수정");
        return "admin/users/edit";
    }

    // 이용자(USER) 프로필 수정 — 기본/상세/성향 정보를 마이페이지와 동일한 서비스 경로로 저장.
    @PostMapping("/users/{id}/edit/disabled")
    public String userEditDisabled(@PathVariable Long id,
                                   @ModelAttribute DisabledEditDTO dto,
                                   RedirectAttributes ra) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        if (user.getRole() != UserRole.USER) {
            ra.addFlashAttribute("error", "이용자 회원이 아닙니다.");
            return "redirect:/admin/users/" + id;
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            ra.addFlashAttribute("error", "이름은 비워둘 수 없습니다.");
            return "redirect:/admin/users/" + id + "/edit";
        }
        try {
            userService.updateDisabledUser(user, dto, null, null);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "이미 사용 중인 전화번호입니다.");
            return "redirect:/admin/users/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
        ra.addFlashAttribute("message", "회원 정보가 수정되었습니다.");
        return "redirect:/admin/users/" + id;
    }

    // 활동지원사(CAREGIVER) 프로필 수정.
    @PostMapping("/users/{id}/edit/caregiver")
    public String userEditCaregiver(@PathVariable Long id,
                                    @ModelAttribute CaregiverEditDTO dto,
                                    RedirectAttributes ra) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        if (user.getRole() != UserRole.CAREGIVER) {
            ra.addFlashAttribute("error", "활동지원사 회원이 아닙니다.");
            return "redirect:/admin/users/" + id;
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            ra.addFlashAttribute("error", "이름은 비워둘 수 없습니다.");
            return "redirect:/admin/users/" + id + "/edit";
        }
        try {
            userService.updateCaregiverUser(user, dto, null, null);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "이미 사용 중인 전화번호입니다.");
            return "redirect:/admin/users/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
        ra.addFlashAttribute("message", "회원 정보가 수정되었습니다.");
        return "redirect:/admin/users/" + id;
    }

    // 관리자(ADMIN) 등 프로필이 없는 계정 — 전화번호만 수정.
    @PostMapping("/users/{id}/edit/basic")
    public String userEditBasic(@PathVariable Long id,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes ra) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        user.changePhone(blankToNull(phone));
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "이미 사용 중인 전화번호입니다.");
            return "redirect:/admin/users/" + id + "/edit";
        }
        ra.addFlashAttribute("message", "회원 정보가 수정되었습니다.");
        return "redirect:/admin/users/" + id;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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

    @PostMapping("/users/{id}/warn/reset")
    public String resetWarn(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("회원을 찾을 수 없습니다."));
        if (user.getStatus() == UserStatus.WARNED) {
            user.changeStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            ra.addFlashAttribute("message", "경고 상태가 초기화되어 활성 회원으로 변경되었습니다.");
        }
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
        model.addAttribute("request", helpRequestRepository.findAdminDetail(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("요청을 찾을 수 없습니다.")));
        model.addAttribute("tmapAppKey", tmapAppKey);
        model.addAttribute("loadTmapSdk", true);
        model.addAttribute("activeMenu", "matching");
        model.addAttribute("title", "매칭 상세");
        return "admin/matching/detail";
    }

    @GetMapping("/matching/{id}/edit")
    public String matchingEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("request", helpRequestRepository.findAdminDetail(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("요청을 찾을 수 없습니다.")));
        model.addAttribute("activeMenu", "matching");
        model.addAttribute("title", "매칭 정보 수정");
        return "admin/matching/edit";
    }

    @PostMapping("/matching/{id}/edit")
    public String matchingEdit(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam(required = false) String body,
                               @RequestParam
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desiredStartDatetime,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desiredEndDatetime,
                               @RequestParam(required = false) String departureAddress,
                               @RequestParam(required = false) String destinationAddress,
                               @RequestParam(required = false) String specialNotes,
                               RedirectAttributes ra) {
        var request = helpRequestRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("요청을 찾을 수 없습니다."));
        if (title == null || title.isBlank()) {
            ra.addFlashAttribute("error", "제목은 비워둘 수 없습니다.");
            return "redirect:/admin/matching/" + id + "/edit";
        }
        if (desiredEndDatetime != null && desiredEndDatetime.isBefore(desiredStartDatetime)) {
            ra.addFlashAttribute("error", "종료 시간은 시작 시간 이후여야 합니다.");
            return "redirect:/admin/matching/" + id + "/edit";
        }
        request.updateByAdmin(title.trim(), blankToNull(body),
                desiredStartDatetime, desiredEndDatetime,
                blankToNull(departureAddress), blankToNull(destinationAddress), blankToNull(specialNotes));
        helpRequestRepository.save(request);
        ra.addFlashAttribute("message", "요청 정보가 수정되었습니다.");
        return "redirect:/admin/matching/" + id;
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

    @GetMapping("/reviews/{id}")
    public String reviewDetail(@PathVariable Long id, Model model) {
        var review = reviewRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("리뷰를 찾을 수 없습니다."));
        model.addAttribute("review", review);
        model.addAttribute("activeMenu", "reviews");
        model.addAttribute("title", "리뷰 상세");
        return "admin/reviews/detail";
    }

    @PostMapping("/reviews/{id}/visibility")
    public String toggleVisibility(@PathVariable Long id,
                                   @RequestParam String visibility,
                                   @RequestParam(required = false) String from,
                                   RedirectAttributes ra) {
        var review = reviewRepository.findById(id)
                .orElseThrow(() -> new com.project.ieum.exception.NotFoundException("리뷰를 찾을 수 없습니다."));
        review.changeVisibility(ReviewVisibility.valueOf(visibility));
        reviewRepository.save(review);
        ra.addFlashAttribute("message", "리뷰 공개 상태가 변경되었습니다.");
        return "detail".equals(from) ? "redirect:/admin/reviews/" + id : "redirect:/admin/reviews";
    }
}
