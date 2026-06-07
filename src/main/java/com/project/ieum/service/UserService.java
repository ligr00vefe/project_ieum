package com.project.ieum.service;

import com.project.ieum.dto.*;
import com.project.ieum.entity.*;
import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.caregiver.CaregiverServiceRegion;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.entity.user.UserCommunicationMethod;
import com.project.ieum.entity.user.UserDisabilityType;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
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
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final CaregiverServiceRegionRepository caregiverServiceRegionRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(BasicInfoDTO basicInfo, UserRole role) {
        User user = User.builder()
                .email(basicInfo.getEmail())
                .passwordHash(passwordEncoder.encode(basicInfo.getPassword()))
                .phone(basicInfo.getPhone())
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    public UserProfile registerUserProfile(User user, BasicInfoDTO basicInfo) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName(basicInfo.getName())
                .birthDate(basicInfo.getBirthDate())
                .gender(basicInfo.getGender())
                .guardianName(blankIfNull(basicInfo.getGuardianName()))
                .guardianPhone(blankIfNull(basicInfo.getGuardianPhone()))
                .build();

        return userProfileRepository.save(profile);
    }

    public CaregiverProfile registerCaregiverProfile(User user, BasicInfoDTO basicInfo) {
        CaregiverProfile profile = CaregiverProfile.builder()
                .user(user)
                .fullName(basicInfo.getName())
                .birthDate(basicInfo.getBirthDate())
                .gender(basicInfo.getGender())
                .build();

        return caregiverProfileRepository.save(profile);
    }

    public void addDisabilityTypes(UserProfile profile, DisabilityInfoDTO disabilityInfo) {
        if (disabilityInfo.getDisabilityTypeIds() == null || disabilityInfo.getDisabilityTypeIds().isEmpty()) {
            return;
        }

        List<DisabilityType> types = disabilityTypeRepository.findAllById(disabilityInfo.getDisabilityTypeIds());

        for (DisabilityType type : types) {
            UserDisabilityType udt = UserDisabilityType.builder()
                    .user(profile)
                    .disabilityType(type)
                    .build();
            userDisabilityTypeRepository.save(udt);
        }

        profile.updateActivityInfo(disabilityInfo.getActivityRange(), disabilityInfo.getAvoidSituations());
        userProfileRepository.save(profile);
    }

    public void addCommunicationMethods(UserProfile profile, CommunicationDTO communicationInfo) {
        if (communicationInfo.getCommunicationMethodIds() == null || communicationInfo.getCommunicationMethodIds().isEmpty()) {
            return;
        }

        List<CommunicationMethod> methods = communicationMethodRepository.findAllById(communicationInfo.getCommunicationMethodIds());

        for (CommunicationMethod method : methods) {
            UserCommunicationMethod ucm = UserCommunicationMethod.builder()
                    .user(profile)
                    .communicationMethod(method)
                    .build();
            userCommunicationMethodRepository.save(ucm);
        }
    }

    public void updateCertificationInfo(CaregiverProfile profile, CertificationDTO certificationInfo) {
        profile.setHasCertification(certificationInfo.getHasCertification());
        profile.setCertificationType(certificationInfo.getCertificationType());
        caregiverProfileRepository.save(profile);
    }

    public void updateActivityInfo(CaregiverProfile profile, ActivityInfoDTO activityInfo) {
        profile.setExperience(activityInfo.getExperience());
        caregiverProfileRepository.save(profile);

        if (activityInfo.getAvailabilityTimes() != null) {
            for (ActivityInfoDTO.AvailabilityTimeDTO time : activityInfo.getAvailabilityTimes()) {
                CaregiverAvailability availability = CaregiverAvailability.builder()
                        .caregiver(profile)
                        .dayOfWeek(time.getDayOfWeek())
                        .startTime(time.getStartTime())
                        .endTime(time.getEndTime())
                        .build();
                caregiverAvailabilityRepository.save(availability);
            }
        }

        if (activityInfo.getRegionIds() != null && !activityInfo.getRegionIds().isEmpty()) {
            List<Region> regions = regionRepository.findAllById(activityInfo.getRegionIds());

            for (Region region : regions) {
                CaregiverServiceRegion csr = CaregiverServiceRegion.builder()
                        .caregiver(profile)
                        .region(region)
                        .build();
                caregiverServiceRegionRepository.save(csr);
            }
        }
    }

    public void addPersonalityTagsForUser(UserProfile profile, PersonalityTagDTO personalityTagInfo) {
        if (personalityTagInfo.getPersonalityTagIds() == null || personalityTagInfo.getPersonalityTagIds().isEmpty()) {
            return;
        }

        List<PersonalityTag> tags = personalityTagRepository.findAllById(personalityTagInfo.getPersonalityTagIds());

        for (PersonalityTag tag : tags) {
            com.project.ieum.entity.request.HelpRequestPersonalityTag hrpt = 
                com.project.ieum.entity.request.HelpRequestPersonalityTag.builder()
                    .helpRequest(null)
                    .tag(tag)
                    .build();
        }
    }

    public void addPersonalityTagsForCaregiver(CaregiverProfile profile, PersonalityTagDTO personalityTagInfo) {
        if (personalityTagInfo.getPersonalityTagIds() == null || personalityTagInfo.getPersonalityTagIds().isEmpty()) {
            return;
        }

        List<PersonalityTag> tags = personalityTagRepository.findAllById(personalityTagInfo.getPersonalityTagIds());

        for (PersonalityTag tag : tags) {
            CaregiverPersonalityTag cpt = CaregiverPersonalityTag.builder()
                    .caregiver(profile)
                    .tag(tag)
                    .build();
            caregiverPersonalityTagRepository.save(cpt);
        }
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
