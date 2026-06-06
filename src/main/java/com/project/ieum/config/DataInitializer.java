package com.project.ieum.config;

import com.project.ieum.entity.*;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.caregiver.CaregiverServiceRegion;
import com.project.ieum.entity.user.*;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DisabilityTypeRepository disabilityTypeRepository;
    private final CommunicationMethodRepository communicationMethodRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final UserDisabilityTypeRepository userDisabilityTypeRepository;
    private final UserCommunicationMethodRepository userCommunicationMethodRepository;
    private final UserPersonalityTagRepository userPersonalityTagRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final CaregiverServiceRegionRepository caregiverServiceRegionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (disabilityTypeRepository.count() == 0) {
            initDisabilityTypes();
        }
        if (communicationMethodRepository.count() == 0) {
            initCommunicationMethods();
        }
        if (personalityTagRepository.count() == 0) {
            initPersonalityTags();
        }
        if (regionRepository.count() == 0) {
            initRegions();
        }
        initAdminAccount();
        initTestAccounts();
    }

    private void initDisabilityTypes() {
        disabilityTypeRepository.save(DisabilityType.builder().code("DT001").name("지체장애").sortOrder((short) 1).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT002").name("시각장애").sortOrder((short) 2).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT003").name("청각장애").sortOrder((short) 3).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT004").name("언어장애").sortOrder((short) 4).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT005").name("자폐성장애").sortOrder((short) 5).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT006").name("지적장애").sortOrder((short) 6).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT007").name("뇌병변장애").sortOrder((short) 7).build());
        log.info("장애 유형 초기화 완료");
    }

    private void initCommunicationMethods() {
        communicationMethodRepository.save(CommunicationMethod.builder().name("구어").description("말로 충분히 의사소통을 할 수 있어요").sortOrder((short) 1).build());
        communicationMethodRepository.save(CommunicationMethod.builder().name("수어").description("수화를 사용하는 활동지원사를 선호해요").sortOrder((short) 2).build());
        communicationMethodRepository.save(CommunicationMethod.builder().name("글자").description("글로 의사소통을 해요").sortOrder((short) 3).build());
        communicationMethodRepository.save(CommunicationMethod.builder().name("그림").description("그림이나 AAC를 활용해요").sortOrder((short) 4).build());
        communicationMethodRepository.save(CommunicationMethod.builder().name("전자기기").description("전자기기(태블릿, 앱 등)를 이용해요").sortOrder((short) 5).build());
        log.info("의사소통 방식 초기화 완료");
    }

    private void initPersonalityTags() {
        personalityTagRepository.save(PersonalityTag.builder().name("차분함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("활발함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("친절함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("세심함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("유머러스함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("책임감").build());
        personalityTagRepository.save(PersonalityTag.builder().name("인내심").build());
        personalityTagRepository.save(PersonalityTag.builder().name("적극성").build());
        personalityTagRepository.save(PersonalityTag.builder().name("예민함").build());
        personalityTagRepository.save(PersonalityTag.builder().name("산만함").build());
        log.info("성향 태그 초기화 완료");
    }

    private void initRegions() {
        regionRepository.save(Region.builder().code("1100000000").sido("서울특별시").sigungu("강남구").dong("역삼동").build());
        regionRepository.save(Region.builder().code("1100000001").sido("서울특별시").sigungu("강남구").dong("삼성동").build());
        regionRepository.save(Region.builder().code("1100000002").sido("서울특별시").sigungu("서초구").dong("서초동").build());
        regionRepository.save(Region.builder().code("1100000003").sido("서울특별시").sigungu("송파구").dong("잠실동").build());
        regionRepository.save(Region.builder().code("1100000004").sido("서울특별시").sigungu("마포구").dong("합정동").build());
        regionRepository.save(Region.builder().code("1100000005").sido("서울특별시").sigungu("용산구").dong("한남동").build());
        regionRepository.save(Region.builder().code("2100000000").sido("부산광역시").sigungu("해운대구").dong("우동").build());
        regionRepository.save(Region.builder().code("2100000001").sido("부산광역시").sigungu("남구").dong("대연동").build());
        regionRepository.save(Region.builder().code("3100000000").sido("대구광역시").sigungu("수성구").dong("범어동").build());
        regionRepository.save(Region.builder().code("4100000000").sido("인천광역시").sigungu("남동구").dong("구월동").build());
        log.info("지역 데이터 초기화 완료");
    }

    // ─── 테스트 계정 ────────────────────────────────────────────────────────────

    @Transactional
    public void initTestAccounts() {
        initTestDisabledUser();
        initTestCaregiverUser();
    }

    private void initTestDisabledUser() {
        String email = "d01@test.com";
        if (userRepository.existsByEmail(email)) {
            log.info("장애인 테스트 계정이 이미 존재합니다 - email={}", email);
            return;
        }

        // 1. users
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("1"))
                .phone("010-2222-2222")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        // 2. user_profiles
        UserProfile profile = userProfileRepository.save(UserProfile.builder()
                .user(user)
                .fullName("장애인 테스트")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.F)
                .guardianName("보호자01")
                .guardianPhone("010-5555-5555")
                .build());

        // 3. 활동 가능 범위 / 피해야 할 상황
        profile.updateActivityInfo("집 근처", "큰 소음");
        userProfileRepository.save(profile);

        // 4. 장애 유형: 시각장애(DT002), 언어장애(DT004)
        disabilityTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(t -> t.getCode().equals("DT002") || t.getCode().equals("DT004"))
                .forEach(t -> userDisabilityTypeRepository.save(
                        UserDisabilityType.builder().user(profile).disabilityType(t).build()));

        // 5. 의사소통 방식: 구어, 글자
        communicationMethodRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(m -> m.getName().equals("구어") || m.getName().equals("글자"))
                .forEach(m -> userCommunicationMethodRepository.save(
                        UserCommunicationMethod.builder().user(profile).communicationMethod(m).build()));

        // 6. 성향: 예민함, 산만함
        personalityTagRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> t.getName().equals("예민함") || t.getName().equals("산만함"))
                .forEach(t -> userPersonalityTagRepository.save(
                        UserPersonalityTag.builder().user(profile).tag(t).build()));

        log.info("장애인 테스트 계정 생성 완료 - email={}", email);
    }

    private void initTestCaregiverUser() {
        String email = "c01@test.com";
        if (userRepository.existsByEmail(email)) {
            log.info("활동지원사 테스트 계정이 이미 존재합니다 - email={}", email);
            return;
        }

        // 1. users
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("1"))
                .phone("010-3333-3333")
                .role(UserRole.CAREGIVER)
                .status(UserStatus.ACTIVE)
                .build());

        // 2. caregiver_profiles
        CaregiverProfile profile = caregiverProfileRepository.save(CaregiverProfile.builder()
                .user(user)
                .fullName("활동지원사 테스트")
                .birthDate(LocalDate.of(2001, 2, 2))
                .gender(Gender.F)
                .hasCertification(true)
                .certificationType("사회복지사 1급")
                .experience("1-3년")
                .serviceCategories("이동 보조,병원 동행,야간 보호")
                .availableTimeSlots("오전 (09-12),오후 (12-18),야간")
                .build());

        // 3. 활동 가능 지역: 부산광역시 남구 대연동
        regionRepository.findAllByOrderBySidoAscSigunguAscDongAsc().stream()
                .filter(r -> r.getSido().equals("부산광역시") && r.getSigungu().equals("남구"))
                .forEach(r -> caregiverServiceRegionRepository.save(
                        CaregiverServiceRegion.builder().caregiver(profile).region(r).build()));

        // 4. 성향: 차분함, 친절함, 세심함, 인내심, 책임감, 적극성
        List<String> caregiverTagNames = List.of("차분함", "친절함", "세심함", "인내심", "책임감", "적극성");
        personalityTagRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> caregiverTagNames.contains(t.getName()))
                .forEach(t -> caregiverPersonalityTagRepository.save(
                        CaregiverPersonalityTag.builder().caregiver(profile).tag(t).build()));

        log.info("활동지원사 테스트 계정 생성 완료 - email={}", email);
    }

    // ─── 관리자 계정 ────────────────────────────────────────────────────────────

    private void initAdminAccount() {
        String adminEmail = "admin@test.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("1"))
                    .phone(null)
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build());
            log.info("관리자 계정 생성 완료 - email={}", adminEmail);
        } else {
            log.info("관리자 계정이 이미 존재합니다 - email={}", adminEmail);
        }
    }
}
