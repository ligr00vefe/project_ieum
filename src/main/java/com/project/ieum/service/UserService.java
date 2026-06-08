package com.project.ieum.service;

import com.project.ieum.dto.*;
import com.project.ieum.entity.*;
import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.user.*;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
    private final UserDisabilityTypeRepository userDisabilityTypeRepository;
    private final UserCommunicationMethodRepository userCommunicationMethodRepository;
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final UserPersonalityTagRepository userPersonalityTagRepository;
    private final CaregiverAvailabilityMapper caregiverAvailabilityMapper;
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

        /* 2. caregiver_profiles — @MapsId에 위임, .userId() 명시 금지.
         * available_time_slots(문자열) 컬럼은 #15로 제거되어 빌더에서 빠졌다(가용시간은 caregiver_availability로). */
        CaregiverProfile profile = caregiverProfileRepository.save(CaregiverProfile.builder()
                .user(user)
                .fullName(info.getName())
                .birthDate(info.getBirthDate())
                .gender(info.getGender())
                .hasCertification(hasCert)
                .certificationType(certType)
                .experience(experience)
                .serviceCategories(serviceCategories)
                .build());

        /* 3. caregiver_availability — 정규화 가용시간 적재(#15).
         * 우선: 구조화 입력(availabilityTimes)을 결정적으로 행 변환. 없으면 레거시 칩 폴백(조합 규칙 미확정 스텁→경고). */
        List<CaregiverAvailability> availabilities = caregiverAvailabilityMapper.toRows(
                (activity != null) ? activity.getAvailabilityTimes() : null, profile);
        if (availabilities.isEmpty()) {
            availabilities = caregiverAvailabilityMapper.fromTimeSlotChips(
                    (activity != null) ? activity.getAvailableTimeSlots() : null, profile);
        }
        if (!availabilities.isEmpty()) {
            caregiverAvailabilityRepository.saveAll(availabilities);
        }

        /* 활동 가능 지역: CaregiverServiceRegion 제거(#11, 전역 매칭+근처 정렬로 전환)로 더 이상 저장하지 않는다.
         * TODO(caregiver-region-handoff): 프론트 step3 지역 섹션 제거 시 regionIds·이 블록 정리.
         * 그 전까지 들어오는 regionIds는 무음 손실 방지 차원에서 경고만 남긴다. */
        List<Long> droppedRegionIds = (activity != null) ? activity.getRegionIds() : null;
        if (droppedRegionIds != null && !droppedRegionIds.isEmpty()) {
            log.warn("[register-caregiver] 지역 선택 {}건은 더 이상 저장되지 않습니다(서비스권역 정규화 제거) — 프론트 지역 섹션 제거 대기",
                    droppedRegionIds.size());
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
    // 장애인 수정 데이터 로드
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional(readOnly = true)
    public DisabledEditDTO loadDisabledEditDTO(User user) {
        UserProfile profile = userProfileRepository.findById(user.getId()).orElseThrow();

        List<Long> disabilityIds = profile.getDisabilityTypes().stream()
                .map(udt -> udt.getDisabilityType().getId())
                .collect(Collectors.toList());

        List<Long> commIds = profile.getCommunicationMethods().stream()
                .map(ucm -> ucm.getCommunicationMethod().getId())
                .collect(Collectors.toList());

        List<Long> tagIds = userPersonalityTagRepository.findByUser(profile).stream()
                .map(upt -> upt.getTag().getId())
                .collect(Collectors.toList());

        return DisabledEditDTO.builder()
                .name(profile.getFullName())
                .phone(user.getPhone())
                .birthDate(profile.getBirthDate())
                .gender(profile.getGender())
                .guardianName(profile.getGuardianName())
                .guardianPhone(profile.getGuardianPhone())
                .disabilityTypeIds(disabilityIds)
                .activityRange(profile.getActivityRange())
                .avoidSituations(profile.getAvoidSituations())
                .communicationMethodIds(commIds)
                .personalityTagIds(tagIds)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 활동지원사 수정 데이터 로드
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional(readOnly = true)
    public CaregiverEditDTO loadCaregiverEditDTO(User user) {
        CaregiverProfile profile = caregiverProfileRepository.findById(user.getId()).orElseThrow();

        List<Long> tagIds = caregiverPersonalityTagRepository.findByCaregiver(profile).stream()
                .map(cpt -> cpt.getTag().getId())
                .collect(Collectors.toList());

        List<String> serviceCategories = profile.getServiceCategories() != null
                ? List.of(profile.getServiceCategories().split(","))
                : List.of();
        // 가능 시간대는 caregiver_availability로 정규화(#15). 행→칩 역매핑 규칙이 미확정이라
        // 현재는 빈 칩으로 표시한다(저장 데이터는 caregiver_availability에 보존).
        // TODO(availability-chip-mapping): 역매핑 확정 시 mapper.toTimeSlotChips(rows)로 채움.
        List<String> timeSlots = List.of();
        // 활동 지역은 CaregiverServiceRegion 제거(#11, 전역 매칭)로 더 이상 보유하지 않는다.
        // TODO(caregiver-region-handoff): 프론트 지역 섹션 제거 시 regionIds 바인딩과 함께 정리.
        List<Long> regionIds = List.of();

        return CaregiverEditDTO.builder()
                .name(profile.getFullName())
                .phone(user.getPhone())
                .birthDate(profile.getBirthDate())
                .gender(profile.getGender())
                .introShort(profile.getIntroShort())
                .introLong(profile.getIntroLong())
                .hasCertification(profile.getHasCertification())
                .certificationType(profile.getCertificationType())
                .experience(profile.getExperience())
                .serviceCategories(serviceCategories)
                .availableTimeSlots(timeSlots)
                .regionIds(regionIds)
                .personalityTagIds(tagIds)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 장애인 회원정보 수정
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional
    public void updateDisabledUser(User user, DisabledEditDTO dto, MultipartFile profileImage) {
        UserProfile profile = userProfileRepository.findById(user.getId()).orElseThrow();

        // 기본 정보 업데이트
        profile.updateBasicInfo(dto.getName(), dto.getBirthDate(), dto.getGender());
        profile.updateGuardian(dto.getGuardianName(), dto.getGuardianPhone());
        profile.updateActivityInfo(dto.getActivityRange(), dto.getAvoidSituations());
        user.changePhone(dto.getPhone());

        // 프로필 이미지
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = saveProfileImage(user.getId(), profileImage);
            profile.updateProfileImage(imageUrl);
        }

        // 장애 유형 교체 (orphanRemoval flush 후 insert)
        profile.getDisabilityTypes().clear();
        userProfileRepository.saveAndFlush(profile);
        if (dto.getDisabilityTypeIds() != null && !dto.getDisabilityTypeIds().isEmpty()) {
            List<DisabilityType> types = disabilityTypeRepository.findAllById(dto.getDisabilityTypeIds());
            for (DisabilityType type : types) {
                userDisabilityTypeRepository.save(
                        UserDisabilityType.builder().user(profile).disabilityType(type).build());
            }
        }

        // 의사소통 방식 교체
        profile.getCommunicationMethods().clear();
        userProfileRepository.saveAndFlush(profile);
        if (dto.getCommunicationMethodIds() != null && !dto.getCommunicationMethodIds().isEmpty()) {
            List<CommunicationMethod> methods = communicationMethodRepository.findAllById(dto.getCommunicationMethodIds());
            for (CommunicationMethod method : methods) {
                userCommunicationMethodRepository.save(
                        UserCommunicationMethod.builder().user(profile).communicationMethod(method).build());
            }
        }

        // 성향 태그 교체
        userPersonalityTagRepository.deleteByUser(profile);
        if (dto.getPersonalityTagIds() != null && !dto.getPersonalityTagIds().isEmpty()) {
            List<PersonalityTag> tags = personalityTagRepository.findAllById(dto.getPersonalityTagIds());
            for (PersonalityTag tag : tags) {
                userPersonalityTagRepository.save(
                        UserPersonalityTag.builder().user(profile).tag(tag).build());
            }
        }

        userRepository.save(user);
        log.info("장애인 회원정보 수정 완료 - userId={}", user.getId());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 활동지원사 회원정보 수정
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Transactional
    public void updateCaregiverUser(User user, CaregiverEditDTO dto, MultipartFile profileImage) {
        CaregiverProfile profile = caregiverProfileRepository.findById(user.getId()).orElseThrow();

        // 기본 정보 업데이트
        profile.updateBasicInfo(dto.getName(), dto.getBirthDate(), dto.getGender());
        user.changePhone(dto.getPhone());

        // 자기소개
        profile.updateIntro(dto.getIntroShort(), dto.getIntroLong());

        // 자격증
        profile.updateCertification(dto.getHasCertification(), dto.getCertificationType());

        // 활동 정보 (가능 시간대는 caregiver_availability로 분리되어 여기서 다루지 않음)
        String serviceCategories = dto.getServiceCategories() != null
                ? String.join(",", dto.getServiceCategories()) : null;
        profile.updateActivity(dto.getExperience(), serviceCategories);

        // 가능 시간대: 칩 입력은 조합 규칙 미확정이라 영속 보류(스텁→경고). 구조화 입력 도입 시 교체 저장.
        // TODO(availability-chip-mapping): 규칙 확정 후 기존 행 삭제+재삽입(교체 저장)으로 활성화.
        List<CaregiverAvailability> availabilities =
                caregiverAvailabilityMapper.fromTimeSlotChips(dto.getAvailableTimeSlots(), profile);
        if (!availabilities.isEmpty()) {
            caregiverAvailabilityRepository.saveAll(availabilities);
        }

        // 프로필 이미지
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = saveProfileImage(user.getId(), profileImage);
            profile.updateProfileImage(imageUrl);
        }

        // 활동 지역: CaregiverServiceRegion 제거(#11, 전역 매칭)로 더 이상 저장하지 않는다.
        // TODO(caregiver-region-handoff): 프론트 지역 섹션 제거 시 regionIds 바인딩과 함께 정리.
        if (dto.getRegionIds() != null && !dto.getRegionIds().isEmpty()) {
            log.warn("[update-caregiver] 지역 선택 {}건은 더 이상 저장되지 않습니다(서비스권역 정규화 제거) — 프론트 지역 섹션 제거 대기",
                    dto.getRegionIds().size());
        }

        // 성향 태그 교체
        caregiverPersonalityTagRepository.deleteByCaregiver(profile);
        if (dto.getPersonalityTagIds() != null && !dto.getPersonalityTagIds().isEmpty()) {
            List<PersonalityTag> tags = personalityTagRepository.findAllById(dto.getPersonalityTagIds());
            for (PersonalityTag tag : tags) {
                caregiverPersonalityTagRepository.save(
                        CaregiverPersonalityTag.builder().caregiver(profile).tag(tag).build());
            }
        }

        caregiverProfileRepository.save(profile);
        userRepository.save(user);
        log.info("활동지원사 회원정보 수정 완료 - userId={}", user.getId());
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

    private String saveProfileImage(Long userId, MultipartFile file) {
        try {
            Path uploadDir = Paths.get("uploads/profiles");
            Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "user_" + userId + extension;
            Path targetPath = uploadDir.resolve(filename);
            Files.write(targetPath, file.getBytes());

            return "/uploads/profiles/" + filename;
        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패 - userId={}", userId, e);
            throw new RuntimeException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
