package com.project.ieum.service;

import com.project.ieum.dto.*;
import com.project.ieum.entity.*;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.caregiver.CaregiverServiceRegion;
import com.project.ieum.entity.user.*;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final DisabilityTypeRepository disabilityTypeRepository;
    private final CommunicationMethodRepository communicationMethodRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final RegionRepository regionRepository;
    private final UserDisabilityTypeRepository userDisabilityTypeRepository;
    private final UserCommunicationMethodRepository userCommunicationMethodRepository;
    private final CaregiverServiceRegionRepository caregiverServiceRegionRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final UserPersonalityTagRepository userPersonalityTagRepository;
    private final PasswordEncoder passwordEncoder;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 장애인 전체 등록 (단일 트랜잭션)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional
    public void registerDisabledUser(RegistrationSessionDTO session) {
        BasicInfoDTO info       = session.getBasicInfo();
        DisabilityInfoDTO dis   = session.getDisabilityInfo();
        CommunicationDTO comm   = session.getCommunicationInfo();
        PersonalityTagDTO tags  = session.getPersonalityTags();

        /* 1. users */
        User user = userRepository.save(User.builder()
                .email(info.getEmail())
                .passwordHash(passwordEncoder.encode(info.getPassword()))
                .phone(info.getPhone())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        /* 2. user_profiles
         * @MapsId가 user.id → userId 를 자동 설정하므로 .userId() 명시 금지
         * (명시하면 isNew=false → merge() 호출 → merged entity의 userId가 null이 되는 버그)
         */
        UserProfile profile = userProfileRepository.save(UserProfile.builder()
                .user(user)
                .fullName(info.getName())
                .birthDate(info.getBirthDate())
                .gender(info.getGender())
                .guardianName(info.getGuardianName() != null ? info.getGuardianName() : "")
                .guardianPhone(info.getGuardianPhone() != null ? info.getGuardianPhone() : "")
                .build());

        /* 3. user_disability_types + activity info */
        if (dis != null) {
            if (dis.getDisabilityTypeIds() != null && !dis.getDisabilityTypeIds().isEmpty()) {
                List<DisabilityType> types = disabilityTypeRepository.findAllById(dis.getDisabilityTypeIds());
                for (DisabilityType type : types) {
                    userDisabilityTypeRepository.save(
                            UserDisabilityType.builder().user(profile).disabilityType(type).build());
                }
            }
            // activity_range, avoid_situations → user_profiles 업데이트
            profile.updateActivityInfo(dis.getActivityRange(), dis.getAvoidSituations());
            userProfileRepository.save(profile);
        }

        /* 4. user_communication_methods */
        if (comm != null && comm.getCommunicationMethodIds() != null && !comm.getCommunicationMethodIds().isEmpty()) {
            List<CommunicationMethod> methods = communicationMethodRepository.findAllById(comm.getCommunicationMethodIds());
            for (CommunicationMethod method : methods) {
                userCommunicationMethodRepository.save(UserCommunicationMethod.builder()
                        .user(profile).communicationMethod(method).build());
            }
        }

        /* 5. user_personality_tags */
        if (tags != null && tags.getPersonalityTagIds() != null && !tags.getPersonalityTagIds().isEmpty()) {
            List<PersonalityTag> tagList = personalityTagRepository.findAllById(tags.getPersonalityTagIds());
            for (PersonalityTag tag : tagList) {
                userPersonalityTagRepository.save(
                        UserPersonalityTag.builder().user(profile).tag(tag).build());
            }
        }

        log.info("장애인 회원가입 완료 - userId={}", user.getId());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 활동지원사 전체 등록 (단일 트랜잭션)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional
    public void registerCaregiverUser(RegistrationSessionDTO session) {
        BasicInfoDTO info            = session.getBasicInfo();
        CertificationDTO cert        = session.getCertificationInfo();
        ActivityInfoDTO activity     = session.getActivityInfo();
        PersonalityTagDTO tags       = session.getPersonalityTags();

        /* 1. users */
        User user = userRepository.save(User.builder()
                .email(info.getEmail())
                .passwordHash(passwordEncoder.encode(info.getPassword()))
                .phone(info.getPhone())
                .role(UserRole.CAREGIVER)
                .status(UserStatus.ACTIVE)
                .build());

        /* 2. caregiver_profiles (기본 + 자격증 + 경력 + 가능업무/시간) */
        Boolean hasCert = (cert != null && cert.getHasCertification() != null)
                ? cert.getHasCertification() : false;
        String certType = (cert != null) ? cert.getCertificationType() : null;

        String experience      = (activity != null) ? activity.getExperience() : null;
        String serviceCategories = (activity != null && activity.getServiceCategories() != null)
                ? String.join(",", activity.getServiceCategories()) : null;
        String availableSlots = (activity != null && activity.getAvailableTimeSlots() != null)
                ? String.join(",", activity.getAvailableTimeSlots()) : null;

        /* 2. caregiver_profiles — @MapsId에 위임, .userId() 명시 금지 */
        CaregiverProfile profile = caregiverProfileRepository.save(CaregiverProfile.builder()
                .user(user)
                .fullName(info.getName())
                .birthDate(info.getBirthDate())
                .gender(info.getGender())
                .hasCertification(hasCert)
                .certificationType(certType)
                .experience(experience)
                .serviceCategories(serviceCategories)
                .availableTimeSlots(availableSlots)
                .build());

        /* 3. caregiver_service_regions */
        if (activity != null && activity.getRegionIds() != null && !activity.getRegionIds().isEmpty()) {
            List<Region> regions = regionRepository.findAllById(activity.getRegionIds());
            for (Region region : regions) {
                caregiverServiceRegionRepository.save(
                        CaregiverServiceRegion.builder().caregiver(profile).region(region).build());
            }
        }

        /* 4. caregiver_personality_tags */
        if (tags != null && tags.getPersonalityTagIds() != null && !tags.getPersonalityTagIds().isEmpty()) {
            List<PersonalityTag> tagList = personalityTagRepository.findAllById(tags.getPersonalityTagIds());
            for (PersonalityTag tag : tagList) {
                caregiverPersonalityTagRepository.save(
                        CaregiverPersonalityTag.builder().caregiver(profile).tag(tag).build());
            }
        }

        log.info("활동지원사 회원가입 완료 - userId={}", user.getId());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 유틸리티
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
