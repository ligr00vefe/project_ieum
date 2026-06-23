package com.project.ieum.config;

import com.project.ieum.entity.*;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.request.*;
import com.project.ieum.entity.user.*;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DisabilityTypeRepository disabilityTypeRepository;
    private final CommunicationMethodRepository communicationMethodRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final RegionRepository regionRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final UserDisabilityTypeRepository userDisabilityTypeRepository;
    private final UserCommunicationMethodRepository userCommunicationMethodRepository;
    private final UserPersonalityTagRepository userPersonalityTagRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository helpRequestApplicationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
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
        if (serviceCategoryRepository.count() == 0) {
            initServiceCategories();
        }
        initAdminAccount();
        initTestAccounts();
        initDummyAccounts();
        if (helpRequestRepository.count() == 0) {
            initDummyHelpRequests();
        }
    }

    // ─── 기준 데이터 ─────────────────────────────────────────────────────────────

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
        // 활동지원사 전용 태그
        personalityTagRepository.save(PersonalityTag.builder().name("차분함").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("활발함").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("친절함").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("세심함").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("유머러스함").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("책임감").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("인내심").isCaregiversOnly(true).build());
        personalityTagRepository.save(PersonalityTag.builder().name("적극성").isCaregiversOnly(true).build());
        // 공통/장애인 성향 태그
        personalityTagRepository.save(PersonalityTag.builder().name("예민함").isCaregiversOnly(false).build());
        personalityTagRepository.save(PersonalityTag.builder().name("산만함").isCaregiversOnly(false).build());
        log.info("성향 태그 초기화 완료");
    }

    private void initServiceCategories() {
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC001").name("이동 보조").description("이동 및 외출 시 동행·보조").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC002").name("병원 동행").description("병원 방문 및 진료 동행").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC003").name("약복용 보조").description("복약 확인 및 투약 보조").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC004").name("대기 동행").description("대기 중 동행 및 보조").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC005").name("가사 도움").description("청소·세탁·식사 준비 등 가사 보조").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC006").name("외출 지원").description("쇼핑·나들이 등 외출 동행").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC007").name("수어 통역").description("수어 통역 및 의사소통 보조").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("SC008").name("기타").description("그 외 도움 요청").build());
        log.info("서비스 카테고리 초기화 완료");
    }

    private void initRegions() {
        regionRepository.save(Region.builder().code("1100000000").sido("서울특별시").sigungu("강남구").dong("역삼동")
                .latitude(new BigDecimal("37.500636")).longitude(new BigDecimal("127.036503")).build());
        regionRepository.save(Region.builder().code("1100000001").sido("서울특별시").sigungu("강남구").dong("삼성동")
                .latitude(new BigDecimal("37.513200")).longitude(new BigDecimal("127.062800")).build());
        regionRepository.save(Region.builder().code("1100000002").sido("서울특별시").sigungu("서초구").dong("서초동")
                .latitude(new BigDecimal("37.483950")).longitude(new BigDecimal("127.032500")).build());
        regionRepository.save(Region.builder().code("1100000003").sido("서울특별시").sigungu("송파구").dong("잠실동")
                .latitude(new BigDecimal("37.513700")).longitude(new BigDecimal("127.100000")).build());
        regionRepository.save(Region.builder().code("1100000004").sido("서울특별시").sigungu("마포구").dong("합정동")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100")).build());
        regionRepository.save(Region.builder().code("1100000005").sido("서울특별시").sigungu("용산구").dong("한남동")
                .latitude(new BigDecimal("37.534605")).longitude(new BigDecimal("126.994304")).build());
        regionRepository.save(Region.builder().code("1100000006").sido("서울특별시").sigungu("종로구").dong("혜화동")
                .latitude(new BigDecimal("37.582551")).longitude(new BigDecimal("127.001650")).build());
        regionRepository.save(Region.builder().code("1100000007").sido("서울특별시").sigungu("서대문구").dong("신촌동")
                .latitude(new BigDecimal("37.555283")).longitude(new BigDecimal("126.937046")).build());
        regionRepository.save(Region.builder().code("1100000008").sido("서울특별시").sigungu("성동구").dong("성수동")
                .latitude(new BigDecimal("37.544571")).longitude(new BigDecimal("127.055739")).build());
        regionRepository.save(Region.builder().code("1100000009").sido("서울특별시").sigungu("노원구").dong("월계동")
                .latitude(new BigDecimal("37.617559")).longitude(new BigDecimal("127.063050")).build());
        regionRepository.save(Region.builder().code("1100000010").sido("서울특별시").sigungu("영등포구").dong("여의도동")
                .latitude(new BigDecimal("37.521000")).longitude(new BigDecimal("126.924000")).build());
        regionRepository.save(Region.builder().code("1100000011").sido("서울특별시").sigungu("광진구").dong("능동")
                .latitude(new BigDecimal("37.546000")).longitude(new BigDecimal("127.084491")).build());
        regionRepository.save(Region.builder().code("2100000000").sido("부산광역시").sigungu("해운대구").dong("우동")
                .latitude(new BigDecimal("35.163200")).longitude(new BigDecimal("129.163600")).build());
        regionRepository.save(Region.builder().code("2100000001").sido("부산광역시").sigungu("남구").dong("대연동")
                .latitude(new BigDecimal("35.134050")).longitude(new BigDecimal("129.084300")).build());
        regionRepository.save(Region.builder().code("3100000000").sido("대구광역시").sigungu("수성구").dong("범어동")
                .latitude(new BigDecimal("35.858300")).longitude(new BigDecimal("128.630300")).build());
        regionRepository.save(Region.builder().code("4100000000").sido("인천광역시").sigungu("남동구").dong("구월동")
                .latitude(new BigDecimal("37.449150")).longitude(new BigDecimal("126.730800")).build());
        log.info("지역 데이터 초기화 완료");
    }

    // ─── 관리자 계정 ─────────────────────────────────────────────────────────────

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

    // ─── 기본 테스트 계정 ─────────────────────────────────────────────────────────

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

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("1"))
                .phone("010-2222-2222")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        UserProfile profile = userProfileRepository.save(UserProfile.builder()
                .user(user)
                .fullName("장애인 테스트")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.F)
                .guardianName("보호자01")
                .guardianPhone("010-5555-5555")
                .build());

        profile.updateActivityInfo("집 근처", "큰 소음");
        userProfileRepository.save(profile);

        disabilityTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(t -> t.getCode().equals("DT002") || t.getCode().equals("DT004"))
                .forEach(t -> userDisabilityTypeRepository.save(
                        UserDisabilityType.builder().user(profile).disabilityType(t).build()));

        communicationMethodRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(m -> m.getName().equals("구어") || m.getName().equals("글자"))
                .forEach(m -> userCommunicationMethodRepository.save(
                        UserCommunicationMethod.builder().user(profile).communicationMethod(m).build()));

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

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("1"))
                .phone("010-3333-3333")
                .role(UserRole.CAREGIVER)
                .status(UserStatus.ACTIVE)
                .build());

        CaregiverProfile profile = caregiverProfileRepository.save(CaregiverProfile.builder()
                .user(user)
                .fullName("활동지원사 테스트")
                .birthDate(LocalDate.of(2001, 2, 2))
                .gender(Gender.F)
                .hasCertification(true)
                .certificationType("사회복지사 1급")
                .experience("1-3년")
                .serviceCategories("이동 보조,병원 동행,야간 보호")
                .introShort("친절하고 성실한 활동지원사입니다.")
                .introLong("장애인 분들의 일상을 함께하며 최선을 다하겠습니다.")
                .build());

        List<String> caregiverTagNames = List.of("차분함", "친절함", "세심함", "인내심", "책임감", "적극성");
        personalityTagRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> caregiverTagNames.contains(t.getName()))
                .forEach(t -> caregiverPersonalityTagRepository.save(
                        CaregiverPersonalityTag.builder().caregiver(profile).tag(t).build()));

        log.info("활동지원사 테스트 계정 생성 완료 - email={}", email);
    }

    // ─── 더미 계정 (d02~d05, c02~c05) ───────────────────────────────────────────

    @Transactional
    public void initDummyAccounts() {
        initDummyDisabledUsers();
        initDummyCaregiverUsers();
    }

    private void initDummyDisabledUsers() {
        record DummyUser(String email, String fullName, Gender gender, LocalDate birth, String phone, String dt, String guardianName, String guardianPhone) {}
        List<DummyUser> users = List.of(
                new DummyUser("d02@test.com", "박민준", Gender.M, LocalDate.of(1988, 3, 15), "010-1111-1001", "DT001", "박보호자", "010-9001-1001"),
                new DummyUser("d03@test.com", "이수진", Gender.F, LocalDate.of(1995, 7, 22), "010-1111-1002", "DT002", "이보호자", "010-9001-1002"),
                new DummyUser("d04@test.com", "최재원", Gender.M, LocalDate.of(1979, 11, 8), "010-1111-1003", "DT007", "최보호자", "010-9001-1003"),
                new DummyUser("d05@test.com", "김지은", Gender.F, LocalDate.of(1993, 4, 30), "010-1111-1004", "DT001", "김보호자", "010-9001-1004")
        );

        for (DummyUser du : users) {
            if (userRepository.existsByEmail(du.email())) continue;

            User user = userRepository.save(User.builder()
                    .email(du.email())
                    .passwordHash(passwordEncoder.encode("1"))
                    .phone(du.phone())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build());

            UserProfile profile = userProfileRepository.save(UserProfile.builder()
                    .user(user)
                    .fullName(du.fullName())
                    .birthDate(du.birth())
                    .gender(du.gender())
                    .guardianName(du.guardianName())
                    .guardianPhone(du.guardianPhone())
                    .build());

            disabilityTypeRepository.findAllByOrderBySortOrderAsc().stream()
                    .filter(t -> t.getCode().equals(du.dt()))
                    .forEach(t -> userDisabilityTypeRepository.save(
                            UserDisabilityType.builder().user(profile).disabilityType(t).build()));

            log.info("더미 이용자 계정 생성 완료 - email={}", du.email());
        }
    }

    private void initDummyCaregiverUsers() {
        record DummyCaregiver(String email, String fullName, Gender gender, LocalDate birth, String phone,
                              boolean hasCert, String certType, String exp, String services, String introShort) {}
        List<DummyCaregiver> caregivers = List.of(
                new DummyCaregiver("c02@test.com", "정현우", Gender.M, LocalDate.of(1985, 6, 10), "010-2222-2001",
                        true, "사회복지사 2급", "3-5년", "이동 보조,병원 동행,외출 지원",
                        "3년 이상의 활동 경험을 갖춘 활동지원사입니다."),
                new DummyCaregiver("c03@test.com", "오세영", Gender.F, LocalDate.of(1991, 9, 25), "010-2222-2002",
                        true, "요양보호사 1급", "1-3년", "병원 동행,가사 도움,약복용 보조",
                        "따뜻한 마음으로 함께하겠습니다."),
                new DummyCaregiver("c04@test.com", "한동민", Gender.M, LocalDate.of(1987, 12, 3), "010-2222-2003",
                        false, null, "1년 미만", "이동 보조,대기 동행",
                        "성실하게 최선을 다하는 활동지원사입니다."),
                new DummyCaregiver("c05@test.com", "임수경", Gender.F, LocalDate.of(1993, 2, 17), "010-2222-2004",
                        true, "사회복지사 1급", "5년 이상", "이동 보조,병원 동행,수어 통역,외출 지원",
                        "5년 이상의 풍부한 경험으로 최고의 서비스를 제공합니다.")
        );

        List<String> tagNames = List.of("차분함", "친절함", "세심함", "책임감");
        for (DummyCaregiver dc : caregivers) {
            if (userRepository.existsByEmail(dc.email())) continue;

            User user = userRepository.save(User.builder()
                    .email(dc.email())
                    .passwordHash(passwordEncoder.encode("1"))
                    .phone(dc.phone())
                    .role(UserRole.CAREGIVER)
                    .status(UserStatus.ACTIVE)
                    .build());

            CaregiverProfile profile = caregiverProfileRepository.save(CaregiverProfile.builder()
                    .user(user)
                    .fullName(dc.fullName())
                    .birthDate(dc.birth())
                    .gender(dc.gender())
                    .hasCertification(dc.hasCert())
                    .certificationType(dc.certType())
                    .experience(dc.exp())
                    .serviceCategories(dc.services())
                    .introShort(dc.introShort())
                    .build());

            personalityTagRepository.findAllByOrderByIdAsc().stream()
                    .filter(t -> tagNames.contains(t.getName()))
                    .forEach(t -> caregiverPersonalityTagRepository.save(
                            CaregiverPersonalityTag.builder().caregiver(profile).tag(t).build()));

            log.info("더미 활동지원사 계정 생성 완료 - email={}", dc.email());
        }
    }

    // ─── 더미 도움 요청 (서울 지역, 지도용) ─────────────────────────────────────────

    @Transactional
    public void initDummyHelpRequests() {
        Map<String, ServiceCategory> scMap = serviceCategoryRepository.findAllByOrderByIdAsc()
                .stream().collect(Collectors.toMap(ServiceCategory::getCode, c -> c));

        UserProfile d01 = getUserProfile("d01@test.com");
        UserProfile d02 = getUserProfile("d02@test.com");
        UserProfile d03 = getUserProfile("d03@test.com");
        UserProfile d04 = getUserProfile("d04@test.com");
        UserProfile d05 = getUserProfile("d05@test.com");

        CaregiverProfile c01 = getCaregiverProfile("c01@test.com");
        CaregiverProfile c02 = getCaregiverProfile("c02@test.com");
        CaregiverProfile c03 = getCaregiverProfile("c03@test.com");
        CaregiverProfile c04 = getCaregiverProfile("c04@test.com");

        // OPEN 요청 10개 (지도에 표시될 요청)
        HelpRequest openReq1 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC002"),
                "강남세브란스 병원 동행 요청", "오전 외래 진료 동행 부탁드립니다. 휠체어 이동 가능하신 분 선호합니다.",
                LocalDateTime.of(2026, 6, 20, 9, 0), LocalDateTime.of(2026, 6, 20, 12, 0),
                "서울 강남구 언주로 211", "강남구 역삼동 강남세브란스병원", "서울특별시", "강남구", "역삼동", "06273",
                new BigDecimal("37.500636"), new BigDecimal("127.036503"),
                "서울 강남구 역삼동 648 역삼현대아파트", "서울 강남구 언주로 211 강남세브란스병원",
                "휠체어 탑승 보조 필요", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d02, scMap.get("SC001"),
                "혜화동 외출 이동 보조 요청", "전동휠체어 이동 보조가 필요합니다. 지하철 혜화역 이용 예정입니다.",
                LocalDateTime.of(2026, 6, 21, 14, 0), LocalDateTime.of(2026, 6, 21, 17, 0),
                "서울 종로구 창경궁로 35", null, "서울특별시", "종로구", "혜화동", "03086",
                new BigDecimal("37.582551"), new BigDecimal("127.001650"),
                "서울 종로구 혜화동 혜화역 1번 출구", "서울 성북구 보문동 성신여대입구역 4번 출구",
                "경사로 이동 보조 가능하신 분", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d03, scMap.get("SC006"),
                "홍대 근처 쇼핑 외출 지원", "시각장애 안내 경험 있는 분을 선호합니다. 홍대 상권 쇼핑 동행입니다.",
                LocalDateTime.of(2026, 6, 22, 13, 0), LocalDateTime.of(2026, 6, 22, 16, 0),
                "서울 마포구 양화로 188", null, "서울특별시", "마포구", "합정동", "04033",
                new BigDecimal("37.549200"), new BigDecimal("126.914100"),
                "서울 마포구 합정동 합정역 8번 출구", "서울 마포구 와우산로 29나길 20 홍대입구역 9번 출구",
                "시각 안내 경험자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d04, scMap.get("SC005"),
                "성수동 가사 도움 요청", "청소 및 빨래 보조 부탁드립니다. 아파트 12층입니다.",
                LocalDateTime.of(2026, 6, 23, 10, 0), LocalDateTime.of(2026, 6, 23, 13, 0),
                "서울 성동구 성수일로 89", "12층", "서울특별시", "성동구", "성수동1가", "04780",
                new BigDecimal("37.544571"), new BigDecimal("127.055739"), null, null,
                "엘리베이터 있음", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d05, scMap.get("SC002"),
                "삼성서울병원 외래 동행", "CT 검사 및 진료 동행입니다. 오전 일찍 출발해야 합니다.",
                LocalDateTime.of(2026, 6, 25, 8, 30), LocalDateTime.of(2026, 6, 25, 12, 0),
                "서울 강남구 일원로 81", null, "서울특별시", "강남구", "일원동", "06351",
                new BigDecimal("37.488400"), new BigDecimal("127.085700"),
                "서울 강남구 개포동 개포주공5단지아파트", "서울 강남구 일원로 81 삼성서울병원",
                "전동휠체어 보조 가능하신 분", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "서초구 외출 이동 보조", "지하철 및 버스 이동 시 보조가 필요합니다.",
                LocalDateTime.of(2026, 6, 26, 15, 0), LocalDateTime.of(2026, 6, 26, 18, 0),
                "서울 서초구 서초대로 77길 33", null, "서울특별시", "서초구", "서초동", "06614",
                new BigDecimal("37.483950"), new BigDecimal("127.032500"),
                "서울 서초구 서초동 서초역 1번 출구", "서울 서초구 강남대로 27 교대역 12번 출구",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d02, scMap.get("SC004"),
                "잠실 아산병원 대기 동행", "장시간 대기가 예상됩니다. 함께 기다려 주실 분을 찾습니다.",
                LocalDateTime.of(2026, 6, 27, 10, 0), LocalDateTime.of(2026, 6, 27, 14, 0),
                "서울 송파구 올림픽로 43길 88", null, "서울특별시", "송파구", "신천동", "05551",
                new BigDecimal("37.513700"), new BigDecimal("127.100000"),
                "서울 송파구 잠실동 잠실역 8번 출구", "서울 송파구 올림픽로 43길 88 서울아산병원",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d03, scMap.get("SC007"),
                "한남동 주민센터 수어 통역 요청", "행정 업무 처리 시 수어 통역이 필요합니다.",
                LocalDateTime.of(2026, 6, 28, 13, 0), LocalDateTime.of(2026, 6, 28, 15, 0),
                "서울 용산구 한강대로 14가길 35", null, "서울특별시", "용산구", "한남동", "04418",
                new BigDecimal("37.534605"), new BigDecimal("126.994304"),
                "서울 용산구 한남동 한강진역 2번 출구", "서울 용산구 한남대로27길 7 한남동주민센터",
                "수어 통역 전문 자격증 소지자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d04, scMap.get("SC006"),
                "노원구 마트 장보기 지원", "대형마트 쇼핑 및 귀가 보조입니다. 짐 운반도 도움이 필요합니다.",
                LocalDateTime.of(2026, 6, 30, 11, 0), LocalDateTime.of(2026, 6, 30, 14, 0),
                "서울 노원구 동일로 1238", null, "서울특별시", "노원구", "월계동", "01796",
                new BigDecimal("37.617559"), new BigDecimal("127.063050"),
                "서울 노원구 월계동 광운대역 1번 출구", "서울 노원구 동일로 1238 홈플러스 노원점",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d05, scMap.get("SC003"),
                "신촌 약복용 보조 요청", "정해진 시간에 투약 보조가 필요합니다. 아침 복약입니다.",
                LocalDateTime.of(2026, 7, 1, 8, 0), LocalDateTime.of(2026, 7, 1, 9, 0),
                "서울 서대문구 신촌로 81", null, "서울특별시", "서대문구", "창천동", "03756",
                new BigDecimal("37.555283"), new BigDecimal("126.937046"), null, null,
                "투약 보조 경험자 우대", HelpRequestStatus.OPEN));

        // openReq1 지원 더미: c02 (firstMessage로 대화 있음), c04 (대화 없음 → POST 경로 테스트용)
        HelpRequestApplication openApp1 = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(openReq1).caregiver(c02).status(ApplicationStatus.PENDING).build());
        Conversation openConv1 = conversationRepository.save(Conversation.builder()
                .application(openApp1).requester(d01).caregiver(c02)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(openConv1).sender(c02.getUser())
                .body("안녕하세요! 강남세브란스병원 동행 지원합니다. 휠체어 보조 경험이 있습니다.")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(openReq1).caregiver(c04).status(ApplicationStatus.PENDING).build());

        // MATCHED 요청 1개 (c02가 수락, 대화 있음)
        HelpRequest matchedReq = helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "여의도 공원 산책 동행", "휠체어 이동 보조가 필요합니다. 한강공원 산책 예정입니다.",
                LocalDateTime.of(2026, 6, 18, 10, 0), LocalDateTime.of(2026, 6, 18, 13, 0),
                "서울 영등포구 여의공원로 68", null, "서울특별시", "영등포구", "여의도동", "07340",
                new BigDecimal("37.521000"), new BigDecimal("126.924000"),
                "서울 영등포구 여의도동 여의나루역 1번 출구", "서울 영등포구 여의공원로 68 여의도한강공원",
                null, HelpRequestStatus.MATCHED));

        HelpRequestApplication matchedApp = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(matchedReq).caregiver(c02).status(ApplicationStatus.ACCEPTED).build());
        Conversation matchedConv = conversationRepository.save(Conversation.builder()
                .application(matchedApp).requester(d01).caregiver(c02)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(matchedConv).sender(c02.getUser())
                .body("안녕하세요! 한강공원 산책 동행 지원했습니다. 전동휠체어 보조 경험이 있습니다.")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        // COMPLETED 요청 1개 (c03가 완료, 대화 있음, 리뷰 있음)
        HelpRequest completedReq = helpRequestRepository.save(buildRequest(d02, scMap.get("SC002"),
                "어린이대공원 근처 병원 동행", "소아 재활병원 정기 진료 동행입니다.",
                LocalDateTime.of(2026, 6, 10, 9, 0), LocalDateTime.of(2026, 6, 10, 12, 0),
                "서울 광진구 능동로 216", null, "서울특별시", "광진구", "능동", "05045",
                new BigDecimal("37.546000"), new BigDecimal("127.084491"), "광진구 능동 자택", "건국대병원",
                null, HelpRequestStatus.COMPLETED));

        HelpRequestApplication completedApp = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(completedReq).caregiver(c03).status(ApplicationStatus.COMPLETED).build());
        Conversation completedConv = conversationRepository.save(Conversation.builder()
                .application(completedApp).requester(d02).caregiver(c03)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(completedConv).sender(c03.getUser())
                .body("안녕하세요! 소아 재활병원 동행 지원했습니다. 병원 동행 경험이 풍부합니다.")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        reviewRepository.save(Review.builder()
                .helpRequest(completedReq)
                .author(d02)
                .target(c03)
                .rating((short) 5)
                .body("정말 친절하고 세심하게 도와주셨습니다. 진료실 안까지 동행해 주셔서 너무 감사했습니다. 다음에도 꼭 함께하고 싶어요.")
                .visibility(ReviewVisibility.PUBLIC)
                .build());

        // IN_PROGRESS 요청 2개 (양측 시작확인 완료 → 진행 중). 종료확인 핸드셰이크를 양방향으로 테스트.
        // 희망종료를 미래로 두어 autoCompleteOverdueActivities(희망종료+30분)에 의해 자동 완료되지 않게 한다.
        // 두 건 모두 이용자 d01 / 도우미 c01 조합이라, 테스트 계정 한 쌍으로 양쪽 마지막 확인을 모두 시연할 수 있다.

        // (1) 이용자가 먼저 종료확인 완료 → c01(도우미)로 로그인해 마지막 종료확인 시 COMPLETED 전이
        HelpRequest inProgressReq1 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "강남 도서관 외출 동행 (진행 중)", "도서관 왕복 이동 보조 중입니다. 도우미가 종료확인을 누르면 활동이 완료됩니다.",
                LocalDateTime.of(2026, 6, 17, 9, 0), LocalDateTime.of(2026, 6, 18, 18, 0),
                "서울 강남구 테헤란로 7길 22", null, "서울특별시", "강남구", "역삼동", "06232",
                new BigDecimal("37.500636"), new BigDecimal("127.036503"),
                "서울 강남구 역삼동 역삼역 2번 출구", "서울 강남구 테헤란로 7길 22 강남구립역삼도서관",
                "이용자 종료확인 완료, 도우미 종료확인 대기", HelpRequestStatus.IN_PROGRESS));

        HelpRequestApplication inProgressApp1 = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(inProgressReq1).caregiver(c01).status(ApplicationStatus.ACCEPTED)
                .requesterStartConfirmed(true).caregiverStartConfirmed(true)
                .requesterEndConfirmed(true).caregiverEndConfirmed(false)
                .build());
        Conversation inProgressConv1 = conversationRepository.save(Conversation.builder()
                .application(inProgressApp1).requester(d01).caregiver(c01)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(inProgressConv1).sender(c01.getUser())
                .body("도서관 도착했습니다. 활동 잘 마치고 종료확인 부탁드릴게요!")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        // (2) 도우미가 먼저 종료확인 완료 → d01(이용자)로 로그인해 마지막 종료확인 시 COMPLETED 전이
        HelpRequest inProgressReq2 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC002"),
                "정형외과 진료 동행 (진행 중)", "진료 동행 중입니다. 이용자가 종료확인을 누르면 활동이 완료됩니다.",
                LocalDateTime.of(2026, 6, 17, 10, 0), LocalDateTime.of(2026, 6, 18, 17, 0),
                "서울 강남구 봉은사로 114", null, "서울특별시", "강남구", "삼성동", "06120",
                new BigDecimal("37.513200"), new BigDecimal("127.062800"),
                "서울 강남구 삼성동 한티역 2번 출구", "서울 강남구 봉은사로 114 강남정형외과의원",
                "도우미 종료확인 완료, 이용자 종료확인 대기", HelpRequestStatus.IN_PROGRESS));

        HelpRequestApplication inProgressApp2 = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(inProgressReq2).caregiver(c01).status(ApplicationStatus.ACCEPTED)
                .requesterStartConfirmed(true).caregiverStartConfirmed(true)
                .requesterEndConfirmed(false).caregiverEndConfirmed(true)
                .build());
        Conversation inProgressConv2 = conversationRepository.save(Conversation.builder()
                .application(inProgressApp2).requester(d01).caregiver(c01)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(inProgressConv2).sender(c01.getUser())
                .body("진료 잘 마쳤습니다. 저는 종료확인 눌렀어요. 이용자님도 확인 부탁드립니다!")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        log.info("더미 도움 요청 초기화 완료 (OPEN=10, MATCHED=1, IN_PROGRESS=2, COMPLETED=1)");
    }

    // ─── 헬퍼 메서드 ─────────────────────────────────────────────────────────────

    private HelpRequest buildRequest(
            UserProfile requester, ServiceCategory category,
            String title, String body,
            LocalDateTime start, LocalDateTime end,
            String roadAddress, String addressDetail,
            String sido, String sigungu, String bname, String zonecode,
            BigDecimal latitude, BigDecimal longitude,
            String departureAddress, String destinationAddress,
            String specialNotes, HelpRequestStatus status) {
        return HelpRequest.builder()
                .requester(requester)
                .serviceCategory(category)
                .title(title)
                .body(body)
                .desiredStartDatetime(start)
                .desiredEndDatetime(end)
                .roadAddress(roadAddress)
                .addressDetail(addressDetail)
                .sido(sido)
                .sigungu(sigungu)
                .bname(bname)
                .zonecode(zonecode)
                .latitude(latitude)
                .longitude(longitude)
                .departureAddress(departureAddress)
                .destinationAddress(destinationAddress)
                .specialNotes(specialNotes)
                .status(status)
                .build();
    }

    private UserProfile getUserProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("사용자를 찾을 수 없습니다: " + email));
        return userProfileRepository.findById(user.getId()).orElseThrow(
                () -> new IllegalStateException("이용자 프로필을 찾을 수 없습니다: " + email));
    }

    private CaregiverProfile getCaregiverProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("사용자를 찾을 수 없습니다: " + email));
        return caregiverProfileRepository.findById(user.getId()).orElseThrow(
                () -> new IllegalStateException("활동지원사 프로필을 찾을 수 없습니다: " + email));
    }
}
