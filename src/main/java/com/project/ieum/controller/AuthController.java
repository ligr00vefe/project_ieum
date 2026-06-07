package com.project.ieum.controller;

import com.project.ieum.dto.*;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.entity.CommunicationMethod;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.Region;
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

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/register")
public class AuthController {

    private final UserService userService;
    private final MasterDataService masterDataService;

    private static final String REGISTRATION_SESSION_KEY = "registrationSession";

    @GetMapping
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

    @GetMapping("/{type}/step1")
    public String step1(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getOrCreateSession(session, type);

        if (sessionData.getBasicInfo() == null) {
            model.addAttribute("basicInfo", new BasicInfoDTO());
        } else {
            model.addAttribute("basicInfo", sessionData.getBasicInfo());
        }

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

        RegistrationSessionDTO sessionData = getSession(session);
        sessionData.setBasicInfo(basicInfo);
        sessionData.incrementStep();

        return "redirect:/register/" + type + "/step2";
    }

    @GetMapping("/{type}/step2")
    public String step2(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSession(session);

        if ("disabled".equals(type)) {
            List<DisabilityType> disabilityTypes = masterDataService.getAllDisabilityTypes();
            model.addAttribute("disabilityTypes", disabilityTypes);

            if (sessionData.getDisabilityInfo() == null) {
                model.addAttribute("disabilityInfo", new DisabilityInfoDTO());
            } else {
                model.addAttribute("disabilityInfo", sessionData.getDisabilityInfo());
            }

            return "register/disabled-step2";
        } else {
            if (sessionData.getCertificationInfo() == null) {
                model.addAttribute("certificationInfo", new CertificationDTO());
            } else {
                model.addAttribute("certificationInfo", sessionData.getCertificationInfo());
            }

            return "register/caregiver-step2";
        }
    }

    @PostMapping("/{type}/step2")
    public String step2Submit(
            @PathVariable String type,
            @ModelAttribute DisabilityInfoDTO disabilityInfo,
            @ModelAttribute CertificationDTO certificationInfo,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSession(session);

        if ("disabled".equals(type)) {
            sessionData.setDisabilityInfo(disabilityInfo);
        } else {
            sessionData.setCertificationInfo(certificationInfo);
        }

        sessionData.incrementStep();
        return "redirect:/register/" + type + "/step3";
    }

    @GetMapping("/{type}/step3")
    public String step3(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSession(session);

        if ("disabled".equals(type)) {
            List<CommunicationMethod> communicationMethods = masterDataService.getAllCommunicationMethods();
            model.addAttribute("communicationMethods", communicationMethods);

            if (sessionData.getCommunicationInfo() == null) {
                model.addAttribute("communicationInfo", new CommunicationDTO());
            } else {
                model.addAttribute("communicationInfo", sessionData.getCommunicationInfo());
            }

            return "register/disabled-step3";
        } else {
            List<Region> regions = masterDataService.getAllRegions();
            model.addAttribute("regions", regions);

            if (sessionData.getActivityInfo() == null) {
                model.addAttribute("activityInfo", new ActivityInfoDTO());
            } else {
                model.addAttribute("activityInfo", sessionData.getActivityInfo());
            }

            return "register/caregiver-step3";
        }
    }

    @PostMapping("/{type}/step3")
    public String step3Submit(
            @PathVariable String type,
            @ModelAttribute CommunicationDTO communicationInfo,
            @ModelAttribute ActivityInfoDTO activityInfo,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSession(session);

        if ("disabled".equals(type)) {
            sessionData.setCommunicationInfo(communicationInfo);
        } else {
            sessionData.setActivityInfo(activityInfo);
        }

        sessionData.incrementStep();
        return "redirect:/register/" + type + "/step4";
    }

    @GetMapping("/{type}/step4")
    public String step4(@PathVariable String type, Model model, HttpSession session) {
        RegistrationSessionDTO sessionData = getSession(session);

        List<PersonalityTag> personalityTags = masterDataService.getAllPersonalityTags();
        model.addAttribute("personalityTags", personalityTags);

        if (sessionData.getPersonalityTags() == null) {
            model.addAttribute("personalityTagInfo", new PersonalityTagDTO());
        } else {
            model.addAttribute("personalityTagInfo", sessionData.getPersonalityTags());
        }

        model.addAttribute("userType", type);
        return "register/step4";
    }

    @PostMapping("/{type}/step4")
    public String step4Submit(
            @PathVariable String type,
            @ModelAttribute PersonalityTagDTO personalityTagInfo,
            HttpSession session) {

        RegistrationSessionDTO sessionData = getSession(session);
        sessionData.setPersonalityTags(personalityTagInfo);

        return completeRegistration(type, sessionData, session);
    }

    @GetMapping("/{type}/complete")
    public String complete(@PathVariable String type, HttpSession session) {
        RegistrationSessionDTO sessionData = (RegistrationSessionDTO) session.getAttribute(REGISTRATION_SESSION_KEY);
        if (sessionData == null) {
            return "redirect:/";
        }

        return completeRegistration(type, sessionData, session);
    }

    private String completeRegistration(String type, RegistrationSessionDTO sessionData, HttpSession session) {
        try {
            if ("disabled".equals(type)) {
                completeDisabledRegistration(sessionData);
            } else {
                completeCaregiverRegistration(sessionData);
            }

            session.removeAttribute(REGISTRATION_SESSION_KEY);
            return "redirect:/";

        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return "redirect:/register/" + type + "/step4?error=true";
        }
    }

    private void completeDisabledRegistration(RegistrationSessionDTO sessionData) {
        BasicInfoDTO basicInfo = sessionData.getBasicInfo();
        DisabilityInfoDTO disabilityInfo = sessionData.getDisabilityInfo();
        CommunicationDTO communicationInfo = sessionData.getCommunicationInfo();
        PersonalityTagDTO personalityTagInfo = sessionData.getPersonalityTags();

        var user = userService.registerUser(basicInfo, UserRole.USER);
        var profile = userService.registerUserProfile(user, basicInfo);

        if (disabilityInfo != null) {
            userService.addDisabilityTypes(profile, disabilityInfo);
        }

        if (communicationInfo != null) {
            userService.addCommunicationMethods(profile, communicationInfo);
        }

        if (personalityTagInfo != null) {
            userService.addPersonalityTagsForUser(profile, personalityTagInfo);
        }
    }

    private void completeCaregiverRegistration(RegistrationSessionDTO sessionData) {
        BasicInfoDTO basicInfo = sessionData.getBasicInfo();
        CertificationDTO certificationInfo = sessionData.getCertificationInfo();
        ActivityInfoDTO activityInfo = sessionData.getActivityInfo();
        PersonalityTagDTO personalityTagInfo = sessionData.getPersonalityTags();

        var user = userService.registerUser(basicInfo, UserRole.CAREGIVER);
        var profile = userService.registerCaregiverProfile(user, basicInfo);

        if (certificationInfo != null) {
            userService.updateCertificationInfo(profile, certificationInfo);
        }

        if (activityInfo != null) {
            userService.updateActivityInfo(profile, activityInfo);
        }

        if (personalityTagInfo != null) {
            userService.addPersonalityTagsForCaregiver(profile, personalityTagInfo);
        }
    }

    private RegistrationSessionDTO getSession(HttpSession session) {
        RegistrationSessionDTO sessionData = (RegistrationSessionDTO) session.getAttribute(REGISTRATION_SESSION_KEY);
        if (sessionData == null) {
            throw new IllegalStateException("세션이 만료되었습니다. 다시 시작해주세요.");
        }
        return sessionData;
    }

    private RegistrationSessionDTO getOrCreateSession(HttpSession session, String type) {
        RegistrationSessionDTO sessionData = (RegistrationSessionDTO) session.getAttribute(REGISTRATION_SESSION_KEY);
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
