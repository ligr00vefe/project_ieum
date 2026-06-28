package com.project.ieum.controller;

import com.project.ieum.dto.CaregiverEditDTO;
import com.project.ieum.dto.DisabledEditDTO;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.CaregiverProfileRepository;
import com.project.ieum.repository.HelpRequestApplicationRepository;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.MatchingService;
import com.project.ieum.service.ReviewService;
import com.project.ieum.service.UserService;
import com.project.ieum.dto.market.MarketChatSummaryResponse;
import com.project.ieum.dto.market.MarketPostResponse;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.repository.market.MarketChatRepository;
import com.project.ieum.repository.market.MarketReviewRepository;
import com.project.ieum.service.admin.NoticeService;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketPostService;
import com.project.ieum.service.market.MarketReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MasterDataService masterDataService;
    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;
    private final ReviewService reviewService;
    private final UserProfileRepository userProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final HelpRequestApplicationRepository applicationRepository;
    private final NoticeService noticeService;
    private final MarketPostService marketPostService;
    private final MarketChatService marketChatService;
    private final MarketReviewService marketReviewService;
    private final MarketChatRepository marketChatRepository;
    private final MarketReviewRepository marketReviewRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("recentNotices", noticeService.getRecentPublicNotices());
        model.addAttribute("title", "메인");
        model.addAttribute("description", "케어메이트와 활동지원사를 신뢰 기반으로 연결하는 케어 매칭 플랫폼 이음(ieum). 장애인 활동 지원 서비스를 쉽고 안전하게 찾아보세요.");
        model.addAttribute("jsonLd", true);
        model.addAttribute("content", "home/index");
        return "layout/layout";
    }

    @GetMapping("/healthz")
    @ResponseBody
    public String healthz() {
        return "ok";
    }

    @GetMapping("/readyz")
    @ResponseBody
    public String readyz() {
        return "ok";
    }

    @GetMapping("/safe-meeting")
    public String safeMeeting(Model model) {
        model.addAttribute("content", "guide/safe-meeting");
        model.addAttribute("title", "첫만남 안심가이드");
        model.addAttribute("description", "이음에서 케어메이트와 첫 만남을 안전하게 준비하는 방법을 안내합니다. 안심 만남 장소, 주의사항 등을 확인하세요.");
        return "layout/layout";
    }

    @GetMapping("/how-to-use")
    public String howToUse(Model model) {
        model.addAttribute("content", "guide/how-to-use");
        model.addAttribute("title", "이용방법");
        model.addAttribute("description", "이음 케어 매칭 플랫폼 이용방법을 단계별로 안내합니다. 활동지원사 찾기부터 매칭 완료까지 쉽게 따라해보세요.");
        return "layout/layout";
    }

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (user.getRole() == UserRole.CAREGIVER) {
            return "redirect:/caregiver/mypage";
        }
        return "redirect:/disabled/mypage";
    }

    @GetMapping("/disabled/mypage")
    public String disabledMypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userName", userService.getDisplayName(user));
        model.addAttribute("profileImageUrl", userService.getProfileImageUrl(user));
        model.addAttribute("profileThumbUrl", userService.getProfileThumbUrl(user));
        model.addAttribute("phone", user.getPhone());

        if (profile != null) {
            if (profile.getRegion() != null) {
                model.addAttribute("region", profile.getRegion().getSido() + " " + profile.getRegion().getSigungu());
            }
            List<String> disabilityTypeNames = profile.getDisabilityTypes().stream()
                    .map(dt -> dt.getDisabilityType().getName())
                    .collect(Collectors.toList());
            model.addAttribute("disabilityTypes", disabilityTypeNames);

            List<String> commMethodNames = profile.getCommunicationMethods().stream()
                    .map(cm -> cm.getCommunicationMethod().getName())
                    .collect(Collectors.toList());
            model.addAttribute("communicationMethods", commMethodNames);
        }

        List<HelpRequest> myRequests = helpRequestService.getMyRequests();
        long totalRequests = myRequests.size();
        long completedMatches = myRequests.stream()
                .filter(r -> r.getStatus() == HelpRequestStatus.COMPLETED).count();
        long writtenReviews = reviewService.countWrittenByUserId(user.getId());

        LocalDateTime now = LocalDateTime.now();
        List<HelpRequest> waitingRequests = myRequests.stream()
                .filter(r -> r.getStatus() == HelpRequestStatus.OPEN && !r.getDesiredStartDatetime().isBefore(now))
                .collect(Collectors.toList());
        List<HelpRequest> matchedRequests = myRequests.stream()
                .filter(r -> r.getStatus() == HelpRequestStatus.MATCHED || r.getStatus() == HelpRequestStatus.IN_PROGRESS)
                .collect(Collectors.toList());
        List<HelpRequest> completedRequestsList = myRequests.stream()
                .filter(r -> r.getStatus() == HelpRequestStatus.COMPLETED)
                .collect(Collectors.toList());
        List<HelpRequest> expiredRequests = myRequests.stream()
                .filter(r -> r.getStatus() == HelpRequestStatus.CLOSED ||
                             (r.getStatus() == HelpRequestStatus.OPEN && r.getDesiredStartDatetime().isBefore(now)))
                .collect(Collectors.toList());

        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("completedMatches", completedMatches);
        model.addAttribute("writtenReviews", writtenReviews);
        model.addAttribute("waitingRequests", waitingRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        model.addAttribute("completedRequestsList", completedRequestsList);
        model.addAttribute("expiredRequests", expiredRequests);
        model.addAttribute("matchingViews", helpRequestService.getMyMatchingViews());

        // 이음마켓 — 내 상품 목록 (채팅 수 포함)
        List<MarketPostResponse> myMarketPosts = marketPostService.getMyPosts().stream()
                .map(p -> {
                    List<MarketPostImage> imgs = marketPostService.getImages(p.getId());
                    String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                    int cnt = marketChatService.getChatCountByPost(p.getId());
                    return MarketPostResponse.from(p, thumb, cnt);
                })
                .toList();
        model.addAttribute("myMarketPosts", myMarketPosts);

        // 이음마켓 — 마켓 후기 통계 + 매너온도
        var marketPublicReviewsD = marketReviewService.getPublicReviews(user.getId());
        double mannerTempD = marketReviewService.getMannerTemperature(user.getId());
        model.addAttribute("mannerTemperature", String.format("%.1f", mannerTempD));
        model.addAttribute("mannerTemperatureRaw", mannerTempD);
        model.addAttribute("marketTotalReviews", marketPublicReviewsD.size());
        model.addAttribute("marketReceivedReviews", marketPublicReviewsD);

        // 이음마켓 — 내 구매 채팅 목록 (후기 작성 버튼 포함)
        List<MarketChatSummaryResponse> myBuyChatsD = marketChatRepository.findByBuyerId(user.getId()).stream()
                .map(chat -> {
                    List<MarketPostImage> imgs = marketPostService.getImages(chat.getPost().getId());
                    String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                    boolean hasReview = marketReviewRepository.existsByChat_Id(chat.getId());
                    return MarketChatSummaryResponse.from(chat, user.getId(), thumb, null, 0, hasReview);
                }).toList();
        model.addAttribute("myBuyChats", myBuyChatsD);

        // 내가 쓴 후기 최신 3개
        var myReviews = reviewService.getMyReviews().stream().limit(3).toList();
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("myReviewCount", reviewService.countWrittenByUserId(user.getId()));

        model.addAttribute("content", "disabled/mypage");
        model.addAttribute("title", "마이페이지");
        return "layout/layout";
    }

    @GetMapping("/caregiver/mypage")
    public String caregiverMypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        CaregiverProfile profile = caregiverProfileRepository.findById(user.getId()).orElse(null);

        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userName", userService.getDisplayName(user));
        model.addAttribute("profileImageUrl", userService.getProfileImageUrl(user));
        model.addAttribute("profileThumbUrl", userService.getProfileThumbUrl(user));
        model.addAttribute("phone", user.getPhone());

        if (profile != null) {
            model.addAttribute("avgRating", profile.getAvgRating());
            model.addAttribute("totalReviews", profile.getTotalReviews());
            model.addAttribute("hasCertification", profile.getHasCertification());
            model.addAttribute("certificationType", profile.getCertificationType());
            model.addAttribute("availabilityStatus", profile.getAvailabilityStatus());

            if (profile.getServiceCategories() != null && !profile.getServiceCategories().isBlank()) {
                List<String> categories = Arrays.stream(profile.getServiceCategories().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                model.addAttribute("serviceCategories", categories);
            }
        }

        List<HelpRequestApplication> allApplications = matchingService.getMyApplications();
        List<HelpRequestApplication> pendingApplications = allApplications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING).collect(Collectors.toList());
        List<HelpRequestApplication> acceptedApplications = allApplications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).collect(Collectors.toList());
        List<HelpRequestApplication> completedApplications = allApplications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.COMPLETED).collect(Collectors.toList());
        // 취소/마감된 지원(거절 기능 폐지 후 무산 지원은 모두 CANCELLED). 과거 REJECTED 데이터도 함께 표시.
        List<HelpRequestApplication> cancelledApplications = allApplications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.CANCELLED
                        || a.getStatus() == ApplicationStatus.REJECTED).collect(Collectors.toList());

        model.addAttribute("completedMatches", completedApplications.size());
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("acceptedApplications", acceptedApplications);
        model.addAttribute("completedApplications", completedApplications);
        model.addAttribute("cancelledApplications", cancelledApplications);
        model.addAttribute("receivedReviews", reviewService.getPublicReviews(user.getId()));

        // 이음마켓 — 내 상품 목록 (채팅 수 포함)
        List<MarketPostResponse> myMarketPosts = marketPostService.getMyPosts().stream()
                .map(p -> {
                    List<MarketPostImage> imgs = marketPostService.getImages(p.getId());
                    String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                    int cnt = marketChatService.getChatCountByPost(p.getId());
                    return MarketPostResponse.from(p, thumb, cnt);
                })
                .toList();
        model.addAttribute("myMarketPosts", myMarketPosts);

        // 이음마켓 — 마켓 후기 통계 + 매너온도
        var marketPublicReviews = marketReviewService.getPublicReviews(user.getId());
        double mannerTemp = marketReviewService.getMannerTemperature(user.getId());
        model.addAttribute("mannerTemperature", String.format("%.1f", mannerTemp));
        model.addAttribute("mannerTemperatureRaw", mannerTemp);
        model.addAttribute("marketTotalReviews", marketPublicReviews.size());
        model.addAttribute("marketReceivedReviews", marketPublicReviews);

        // 이음마켓 — 내 구매 채팅 목록 (후기 작성 버튼 포함)
        List<MarketChatSummaryResponse> myBuyChats = marketChatRepository.findByBuyerId(user.getId()).stream()
                .map(chat -> {
                    List<MarketPostImage> imgs = marketPostService.getImages(chat.getPost().getId());
                    String thumb = imgs.isEmpty() ? null : imgs.get(0).getImageUrl();
                    boolean hasReview = marketReviewRepository.existsByChat_Id(chat.getId());
                    return MarketChatSummaryResponse.from(chat, user.getId(), thumb, null, 0, hasReview);
                }).toList();
        model.addAttribute("myBuyChats", myBuyChats);

        model.addAttribute("content", "caregiver/mypage");
        model.addAttribute("title", "마이페이지");
        return "layout/layout";
    }

    // ─── 회원정보 수정 진입 ─────────────────────────────────────

    @GetMapping("/mypage/edit")
    public String mypageEdit(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (user.getRole() == UserRole.CAREGIVER) {
            return "redirect:/caregiver/mypage/edit";
        }
        return "redirect:/disabled/mypage/edit";
    }

    // ─── 장애인 회원정보 수정 ────────────────────────────────────

    @GetMapping("/disabled/mypage/edit")
    public String disabledMypageEdit(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        DisabledEditDTO editDTO = userService.loadDisabledEditDTO(user);

        model.addAttribute("editDTO", editDTO);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("disabilityTypes", masterDataService.getAllDisabilityTypes());
        model.addAttribute("communicationMethods", masterDataService.getAllCommunicationMethods());
        model.addAttribute("personalityTags", masterDataService.getAllPersonalityTags());
        model.addAttribute("content", "disabled/edit");
        model.addAttribute("title", "회원정보 수정");
        return "layout/layout";
    }

    @PostMapping("/disabled/mypage/edit")
    public String disabledMypageEditSubmit(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute("editDTO") DisabledEditDTO editDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "presetImage", required = false) String presetImage,
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.updateDisabledUser(user, editDTO, profileImage, presetImage);
        redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        return "redirect:/disabled/mypage";
    }

    // ─── 활동지원사 회원정보 수정 ────────────────────────────────

    @GetMapping("/caregiver/mypage/edit")
    public String caregiverMypageEdit(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        CaregiverEditDTO editDTO = userService.loadCaregiverEditDTO(user);

        model.addAttribute("editDTO", editDTO);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("personalityTags", masterDataService.getCaregiverPersonalityTags());
        model.addAttribute("regions", masterDataService.getAllRegions());
        model.addAttribute("content", "caregiver/edit");
        model.addAttribute("title", "회원정보 수정");
        return "layout/layout";
    }

    @PostMapping("/caregiver/mypage/edit")
    public String caregiverMypageEditSubmit(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute("editDTO") CaregiverEditDTO editDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "presetImage", required = false) String presetImage,
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.updateCaregiverUser(user, editDTO, profileImage, presetImage);
        redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        return "redirect:/caregiver/mypage";
    }

    // ─── 비밀번호 변경 ────────────────────────────────────────────

    @GetMapping("/mypage/password")
    public String passwordChangePage(Model model) {
        model.addAttribute("content", "mypage/password");
        model.addAttribute("title", "비밀번호 변경");
        return "layout/layout";
    }

    @PostMapping("/mypage/password")
    public String passwordChangeSubmit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/mypage/password";
        }
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호는 영문·숫자·특수문자(@$!%*#?&)를 포함한 8자 이상이어야 합니다.");
            return "redirect:/mypage/password";
        }
        try {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            userService.changePassword(user, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
            return "redirect:/mypage";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mypage/password";
        }
    }

    // ─── 회원 탈퇴 ────────────────────────────────────────────

    @PostMapping("/mypage/withdraw")
    public String withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("password") String password,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            userService.withdraw(user, password);
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                    .logout(request, response, null);
            redirectAttributes.addFlashAttribute("successMessage", "탈퇴가 완료되었습니다.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return user.getRole() == UserRole.CAREGIVER ? "redirect:/caregiver/mypage" : "redirect:/disabled/mypage";
        }
    }
}
