package com.project.ieum.controller;

import com.project.ieum.dto.*;
import com.project.ieum.entity.CommunicationMethod;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.Region;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.service.MasterDataService;
import com.project.ieum.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/register")
public class AuthController {

    private final UserService userService;
    private final MasterDataService masterDataService;

    private static final String REGISTRATION_SESSION_KEY = "registrationSession";

    // ─── 유형 선택 ──────────────────────────────────────────────────────────────

    @GetMapping("/type-select")
    public String registerTypeSelect() {
        return "register/type-select";
    }

    @PostMapping("/type")
    public String selectType(@RequestParam String type, HttpSession session) {
        UserRole userRole = "caregiver".equalsIgnoreCase(type) ? UserRole.CAREGIVER : UserRole.USER;

        RegistrationSessionDTO sessionData = RegistrationSessionDTO.builder()
                .userType(userRole)
                .currentStep(1)
                .build();

        session.setAttribute(REGISTRATION_SESSION_KEY, sessionData);
        return "redirect:/register/" + type + "/step1";
    }

    // ─── STEP 1: 기본 정보 ──────────────────────────────────────────────────────

    @GetMapping("/{type}/step1")
    public String step1(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getOrCreateSession(session, type);

        model.addAttribute("basicInfo",
                sessionData.getBasicInfo() != null ? sessionData.getBasicInfo() : new BasicInfoDTO());
        model.addAttribute("userType", type);
        return "register/step1";
    }

    @PostMapping("/{type}/step1")
    public String step1Submit(
            @PathVariable String type,
            @Valid @ModelAttribute("basicInfo") BasicInfoDTO basicInfo,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("userType", type);
            return "register/step1";
        }

        if (userService.existsByEmail(basicInfo.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
            model.addAttribute("userType", type);
            return "register/step1";
        }

        if (userService.existsByPhone(basicInfo.getPhone())) {
            bindingResult.rejectValue("phone", "duplicate", "이미 사용 중인 전화번호입니다.");
            model.addAttribute("userType", type);
            return "register/step1";
        }

        RegistrationSessionDTO sessionData = getOrCreateSession(session, type);
        sessionData.setBasicInfo(basicInfo);
        sessionData.incrementStep();

        return "redirect:/register/" + type + "/step2";
    }

    // ─── STEP 2 ─────────────────────────────────────────────────────────────────

    @GetMapping("/{type}/step2")
    public String step2(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/" + type + "/step1";

        if ("disabled".equals(type)) {
            model.addAttribute("disabilityTypes", masterDataService.getAllDisabilityTypes());
            model.addAttribute("disabilityInfo",
                    sessionData.getDisabilityInfo() != null ? sessionData.getDisabilityInfo() : new DisabilityInfoDTO());
            return "register/disabled-step2";
        } else {
            model.addAttribute("certificationInfo",
                    sessionData.getCertificationInfo() != null ? sessionData.getCertificationInfo() : new CertificationDTO());
            return "register/caregiver-step2";
        }
    }

    @PostMapping("/disabled/step2")
    public String disabledStep2Submit(
            @ModelAttribute DisabilityInfoDTO disabilityInfo,
            @RequestParam(defaultValue = "forward") String direction,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/disabled/step1";
        sessionData.setDisabilityInfo(disabilityInfo);
        return "back".equals(direction)
                ? "redirect:/register/disabled/step1"
                : "redirect:/register/disabled/step3";
    }

    @PostMapping("/caregiver/step2")
    public String caregiverStep2Submit(
            @ModelAttribute CertificationDTO certificationInfo,
            @RequestParam(defaultValue = "forward") String direction,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/caregiver/step1";
        sessionData.setCertificationInfo(certificationInfo);
        return "back".equals(direction)
                ? "redirect:/register/caregiver/step1"
                : "redirect:/register/caregiver/step3";
    }

    // ─── STEP 3 ─────────────────────────────────────────────────────────────────

    @GetMapping("/{type}/step3")
    public String step3(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/" + type + "/step1";

        if ("disabled".equals(type)) {
            model.addAttribute("communicationMethods", masterDataService.getAllCommunicationMethods());
            model.addAttribute("communicationInfo",
                    sessionData.getCommunicationInfo() != null ? sessionData.getCommunicationInfo() : new CommunicationDTO());
            return "register/disabled-step3";
        } else {
            model.addAttribute("regions", masterDataService.getAllRegions());
            model.addAttribute("activityInfo",
                    sessionData.getActivityInfo() != null ? sessionData.getActivityInfo() : new ActivityInfoDTO());
            return "register/caregiver-step3";
        }
    }

    @PostMapping("/disabled/step3")
    public String disabledStep3Submit(
            @ModelAttribute CommunicationDTO communicationInfo,
            @RequestParam(defaultValue = "forward") String direction,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/disabled/step1";
        sessionData.setCommunicationInfo(communicationInfo);
        return "back".equals(direction)
                ? "redirect:/register/disabled/step2"
                : "redirect:/register/disabled/step4";
    }

    @PostMapping("/caregiver/step3")
    public String caregiverStep3Submit(
            @ModelAttribute ActivityInfoDTO activityInfo,
            @RequestParam(defaultValue = "forward") String direction,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/caregiver/step1";
        sessionData.setActivityInfo(activityInfo);
        return "back".equals(direction)
                ? "redirect:/register/caregiver/step2"
                : "redirect:/register/caregiver/step4";
    }

    // ─── STEP 4: 성향 태그 ───────────────────────────────────────────────────────

    @GetMapping("/{type}/step4")
    public String step4(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/" + type + "/step1";

        model.addAttribute("personalityTags", masterDataService.getAllPersonalityTags());
        model.addAttribute("personalityTagInfo",
                sessionData.getPersonalityTags() != null ? sessionData.getPersonalityTags() : new PersonalityTagDTO());
        model.addAttribute("userType", type);
        return "register/step4";
    }

    @PostMapping("/{type}/step4")
    public String step4Submit(
            @PathVariable String type,
            @ModelAttribute PersonalityTagDTO personalityTagInfo,
            @RequestParam(defaultValue = "forward") String direction,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/" + type + "/step1";

        sessionData.setPersonalityTags(personalityTagInfo);

        if ("back".equals(direction)) {
            return "redirect:/register/" + type + "/step3";
        }
        return "redirect:/register/" + type + "/complete";
    }

    // ─── 완료 ───────────────────────────────────────────────────────────────────

    @GetMapping("/{type}/complete")
    public String complete(@PathVariable String type, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        RegistrationSessionDTO sessionData = getSessionOrRedirect(session);
        if (sessionData == null) return "redirect:/register/" + type + "/step1";

        return completeRegistration(type, sessionData, session, redirectAttributes);
    }

    private String completeRegistration(String type, RegistrationSessionDTO sessionData,
                                        HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            if ("disabled".equals(type)) {
                userService.registerDisabledUser(sessionData);
            } else {
                userService.registerCaregiverUser(sessionData);
            }

            session.removeAttribute(REGISTRATION_SESSION_KEY);
            return "redirect:/";

        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "회원가입 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/register/" + type + "/step4";
        }
    }

    // ─── 세션 유틸 ───────────────────────────────────────────────────────────────

    /** 세션 없으면 null 반환 (호출 측에서 redirect 처리) */
    private RegistrationSessionDTO getSessionOrRedirect(HttpSession session) {
        return (RegistrationSessionDTO) session.getAttribute(REGISTRATION_SESSION_KEY);
    }

    /** 세션 없으면 새로 생성 */
    private RegistrationSessionDTO getOrCreateSession(HttpSession session, String type) {
        RegistrationSessionDTO sessionData =
                (RegistrationSessionDTO) session.getAttribute(REGISTRATION_SESSION_KEY);
        if (sessionData == null) {
            UserRole userRole = "caregiver".equalsIgnoreCase(type) ? UserRole.CAREGIVER : UserRole.USER;
            sessionData = RegistrationSessionDTO.builder()
                    .userType(userRole)
                    .currentStep(1)
                    .build();
            session.setAttribute(REGISTRATION_SESSION_KEY, sessionData);
        }
        return sessionData;
    }
}
