package com.project.ieum.controller;

import com.project.ieum.dto.CaregiverEditDTO;
import com.project.ieum.dto.DisabledEditDTO;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MasterDataService masterDataService;

    @GetMapping("/how-to-use")
    public String howToUse(Model model) {
        model.addAttribute("content", "guide/how-to-use");
        model.addAttribute("title", "이용방법");
        return "layout/layout";
    }

    @GetMapping("/disabled/home")
    public String disabledHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("email", userDetails.getUsername());
        return "home/disabled-home";
    }

    @GetMapping("/caregiver/home")
    public String caregiverHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("email", userDetails.getUsername());
        return "home/caregiver-home";
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
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("content", "disabled/mypage");
        model.addAttribute("title", "마이페이지");
        return "layout/layout";
    }

    @GetMapping("/caregiver/mypage")
    public String caregiverMypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("userEmail", user.getEmail());
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
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.updateDisabledUser(user, editDTO, profileImage);
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
        model.addAttribute("personalityTags", masterDataService.getAllPersonalityTags());
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
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.updateCaregiverUser(user, editDTO, profileImage);
        redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        return "redirect:/caregiver/mypage";
    }
}
