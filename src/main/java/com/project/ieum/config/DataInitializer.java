package com.project.ieum.config;

import com.project.ieum.entity.*;
import com.project.ieum.entity.MbtiType;
import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryCategory;
import com.project.ieum.entity.inquiry.InquiryReply;
import com.project.ieum.entity.market.MarketCategory;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostImage;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.entity.notice.Notice;
import com.project.ieum.entity.popup.Popup;
import com.project.ieum.entity.request.*;
import com.project.ieum.entity.user.*;
import com.project.ieum.repository.*;
import com.project.ieum.repository.market.MarketCategoryRepository;
import com.project.ieum.repository.market.MarketPostImageRepository;
import com.project.ieum.repository.market.MarketPostRepository;
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
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository helpRequestApplicationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final NoticeRepository noticeRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final MarketCategoryRepository marketCategoryRepository;
    private final MarketPostRepository marketPostRepository;
    private final MarketPostImageRepository marketPostImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final PopupRepository popupRepository;

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
        if (marketCategoryRepository.count() == 0) {
            initMarketCategories();
        }
        if (noticeRepository.count() == 0) {
            initDummyNotices();
        }
        if (inquiryRepository.count() == 0) {
            initDummyInquiries();
        }
        if (marketPostRepository.count() == 0) {
            initDummyMarketPosts();
        }

        if (popupRepository.count() == 0) {
            initializePopups();
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
                .fullName("한지수")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.F)
                .guardianName("박세라 요양사")
                .guardianPhone("010-5555-5555")
                .mbtiType(MbtiType.INFP)
                .build());

        profile.updateActivityInfo("집 근처", "큰 소음");
        profile.updatePreferredMbtis(java.util.Set.of(MbtiType.INFJ, MbtiType.ENFJ, MbtiType.ISFJ));
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
                .fullName("송미래")
                .birthDate(LocalDate.of(2001, 2, 2))
                .gender(Gender.F)
                .hasCertification(true)
                .certificationType("사회복지사 1급")
                .experience("1-3년")
                .serviceCategories("이동 보조,병원 동행,야간 보호")
                .introShort("친절하고 성실한 활동지원사입니다.")
                .introLong("장애인 분들의 일상을 함께하며 최선을 다하겠습니다.")
                .mbtiType(MbtiType.ISFJ)
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
        record DummyUser(String email, String fullName, Gender gender, LocalDate birth, String phone, String dt,
                         String guardianName, String guardianPhone, MbtiType mbti, java.util.Set<MbtiType> preferredMbtis) {}
        List<DummyUser> users = List.of(
                new DummyUser("d02@test.com", "박민준", Gender.M, LocalDate.of(1988, 3, 15), "010-1111-1001", "DT001",
                        "김수연 담당 활동지원사", "010-9001-1001", MbtiType.ISTJ,
                        java.util.Set.of(MbtiType.ISTJ, MbtiType.ISFJ, MbtiType.ESTJ)),
                new DummyUser("d03@test.com", "이수진", Gender.F, LocalDate.of(1995, 7, 22), "010-1111-1002", "DT002",
                        "이지현 (언니)", "010-9001-1002", MbtiType.ENFP,
                        java.util.Set.of(MbtiType.ENFP, MbtiType.ENFJ, MbtiType.INFP, MbtiType.INFJ)),
                new DummyUser("d04@test.com", "최재원", Gender.M, LocalDate.of(1979, 11, 8), "010-1111-1003", "DT007",
                        "최동훈 사회복지사", "010-9001-1003", null, null),
                new DummyUser("d05@test.com", "김지은", Gender.F, LocalDate.of(1993, 4, 30), "010-1111-1004", "DT001",
                        "정유나 요양보호사", "010-9001-1004", MbtiType.ESFJ,
                        java.util.Set.of(MbtiType.ISFJ, MbtiType.ESFJ))
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
                    .mbtiType(du.mbti())
                    .build());

            if (du.preferredMbtis() != null) {
                profile.updatePreferredMbtis(du.preferredMbtis());
                userProfileRepository.save(profile);
            }

            disabilityTypeRepository.findAllByOrderBySortOrderAsc().stream()
                    .filter(t -> t.getCode().equals(du.dt()))
                    .forEach(t -> userDisabilityTypeRepository.save(
                            UserDisabilityType.builder().user(profile).disabilityType(t).build()));

            log.info("더미 이용자 계정 생성 완료 - email={}", du.email());
        }
    }

    private void initDummyCaregiverUsers() {
        record DummyCaregiver(String email, String fullName, Gender gender, LocalDate birth, String phone,
                              boolean hasCert, String certType, String exp, String services, String introShort, MbtiType mbti) {}
        List<DummyCaregiver> caregivers = List.of(
                new DummyCaregiver("c02@test.com", "정현우", Gender.M, LocalDate.of(1985, 6, 10), "010-2222-2001",
                        true, "사회복지사 2급", "3-5년", "이동 보조,병원 동행,외출 지원",
                        "3년 이상의 활동 경험을 갖춘 활동지원사입니다.", MbtiType.ISTJ),
                new DummyCaregiver("c03@test.com", "오세영", Gender.F, LocalDate.of(1991, 9, 25), "010-2222-2002",
                        true, "요양보호사 1급", "1-3년", "병원 동행,가사 도움,약복용 보조",
                        "따뜻한 마음으로 함께하겠습니다.", MbtiType.INFJ),
                new DummyCaregiver("c04@test.com", "한동민", Gender.M, LocalDate.of(1987, 12, 3), "010-2222-2003",
                        false, null, "1년 미만", "이동 보조,대기 동행",
                        "성실하게 최선을 다하는 활동지원사입니다.", null),
                new DummyCaregiver("c05@test.com", "임수경", Gender.F, LocalDate.of(1993, 2, 17), "010-2222-2004",
                        true, "사회복지사 1급", "5년 이상", "이동 보조,병원 동행,수어 통역,외출 지원",
                        "5년 이상의 풍부한 경험으로 최고의 서비스를 제공합니다.", MbtiType.ENFJ)
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
                    .mbtiType(dc.mbti())
                    .build());

            personalityTagRepository.findAllByOrderByIdAsc().stream()
                    .filter(t -> tagNames.contains(t.getName()))
                    .forEach(t -> caregiverPersonalityTagRepository.save(
                            CaregiverPersonalityTag.builder().caregiver(profile).tag(t).build()));

            // 더미 가용시간: 월~일 전일(0~6, 08:00~22:00)
            for (short day = 0; day <= 6; day++) {
                caregiverAvailabilityRepository.save(CaregiverAvailability.builder()
                        .caregiver(profile)
                        .dayOfWeek(day)
                        .startTime(java.time.LocalTime.of(8, 0))
                        .endTime(java.time.LocalTime.of(22, 0))
                        .build());
            }

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

        // OPEN 요청 14개 (지도에 표시될 요청) — 희망일: 7/1 ~ 7/30
        HelpRequest openReq1 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC002"),
                "강남세브란스 병원 동행 요청", "오전 외래 진료 동행 부탁드립니다. 휠체어 이동 가능하신 분 선호합니다.",
                LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 12, 0),
                "서울 강남구 언주로 211", "강남구 역삼동 강남세브란스병원", "서울특별시", "강남구", "역삼동", "06273",
                new BigDecimal("37.500636"), new BigDecimal("127.036503"),
                "서울 강남구 역삼로 175", "서울 강남구 언주로 211",
                "휠체어 탑승 보조 필요", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d02, scMap.get("SC001"),
                "혜화동 외출 이동 보조 요청", "전동휠체어 이동 보조가 필요합니다. 지하철 혜화역 이용 예정입니다.",
                LocalDateTime.of(2026, 7, 3, 14, 0), LocalDateTime.of(2026, 7, 3, 17, 0),
                "서울 종로구 창경궁로 35", null, "서울특별시", "종로구", "혜화동", "03086",
                new BigDecimal("37.582551"), new BigDecimal("127.001650"),
                "서울 종로구 창경궁로 112", "서울 성북구 보문로 168",
                "경사로 이동 보조 가능하신 분", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d03, scMap.get("SC006"),
                "홍대 근처 쇼핑 외출 지원", "시각장애 안내 경험 있는 분을 선호합니다. 홍대 상권 쇼핑 동행입니다.",
                LocalDateTime.of(2026, 7, 5, 13, 0), LocalDateTime.of(2026, 7, 5, 16, 0),
                "서울 마포구 양화로 188", null, "서울특별시", "마포구", "합정동", "04033",
                new BigDecimal("37.549200"), new BigDecimal("126.914100"),
                "서울 마포구 양화로 2", "서울 마포구 와우산로 29나길 20",
                "시각 안내 경험자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d04, scMap.get("SC005"),
                "성수동 가사 도움 요청", "청소 및 빨래 보조 부탁드립니다. 아파트 12층입니다.",
                LocalDateTime.of(2026, 7, 7, 10, 0), LocalDateTime.of(2026, 7, 7, 13, 0),
                "서울 성동구 성수일로 89", "12층", "서울특별시", "성동구", "성수동1가", "04780",
                new BigDecimal("37.544571"), new BigDecimal("127.055739"), null, null,
                "엘리베이터 있음", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d05, scMap.get("SC002"),
                "삼성서울병원 외래 동행", "CT 검사 및 진료 동행입니다. 오전 일찍 출발해야 합니다.",
                LocalDateTime.of(2026, 7, 9, 8, 30), LocalDateTime.of(2026, 7, 9, 12, 0),
                "서울 강남구 일원로 81", null, "서울특별시", "강남구", "일원동", "06351",
                new BigDecimal("37.488400"), new BigDecimal("127.085700"),
                "서울 강남구 개포로 227", "서울 강남구 일원로 81",
                "전동휠체어 보조 가능하신 분", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "서초구 외출 이동 보조", "지하철 및 버스 이동 시 보조가 필요합니다.",
                LocalDateTime.of(2026, 7, 11, 15, 0), LocalDateTime.of(2026, 7, 11, 18, 0),
                "서울 서초구 서초대로 77길 33", null, "서울특별시", "서초구", "서초동", "06614",
                new BigDecimal("37.483950"), new BigDecimal("127.032500"),
                "서울 서초구 강남대로 43", "서울 서초구 강남대로 27",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d02, scMap.get("SC004"),
                "잠실 아산병원 대기 동행", "장시간 대기가 예상됩니다. 함께 기다려 주실 분을 찾습니다.",
                LocalDateTime.of(2026, 7, 13, 10, 0), LocalDateTime.of(2026, 7, 13, 14, 0),
                "서울 송파구 올림픽로 43길 88", null, "서울특별시", "송파구", "신천동", "05551",
                new BigDecimal("37.513700"), new BigDecimal("127.100000"),
                "서울 송파구 올림픽로 240", "서울 송파구 올림픽로 43길 88",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d03, scMap.get("SC007"),
                "한남동 주민센터 수어 통역 요청", "행정 업무 처리 시 수어 통역이 필요합니다.",
                LocalDateTime.of(2026, 7, 15, 13, 0), LocalDateTime.of(2026, 7, 15, 15, 0),
                "서울 용산구 한강대로 14가길 35", null, "서울특별시", "용산구", "한남동", "04418",
                new BigDecimal("37.534605"), new BigDecimal("126.994304"),
                "서울 용산구 이태원로 249", "서울 용산구 한남대로27길 7",
                "수어 통역 전문 자격증 소지자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d04, scMap.get("SC006"),
                "노원구 마트 장보기 지원", "대형마트 쇼핑 및 귀가 보조입니다. 짐 운반도 도움이 필요합니다.",
                LocalDateTime.of(2026, 7, 17, 11, 0), LocalDateTime.of(2026, 7, 17, 14, 0),
                "서울특별시 노원구 동일로 1238", null, "서울특별시", "노원구", "월계동", "01796",
                new BigDecimal("37.617559"), new BigDecimal("127.063050"),
                "서울특별시 노원구 화랑로 393", "서울특별시 노원구 동일로 1238",
                null, HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d05, scMap.get("SC003"),
                "신촌 약복용 보조 요청", "정해진 시간에 투약 보조가 필요합니다. 아침 복약입니다.",
                LocalDateTime.of(2026, 7, 19, 8, 0), LocalDateTime.of(2026, 7, 19, 9, 0),
                "서울 서대문구 신촌로 81", null, "서울특별시", "서대문구", "창천동", "03756",
                new BigDecimal("37.555283"), new BigDecimal("126.937046"), null, null,
                "투약 보조 경험자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d01, scMap.get("SC002"),
                "강남구 정형외과 진료 동행", "무릎 정기 진료입니다. 병원 내 이동 보조 부탁드립니다.",
                LocalDateTime.of(2026, 7, 21, 10, 0), LocalDateTime.of(2026, 7, 21, 13, 0),
                "서울 강남구 논현로 86길 11", null, "서울특별시", "강남구", "역삼동", "06232",
                new BigDecimal("37.500636"), new BigDecimal("127.036503"),
                "서울 강남구 테헤란로 2", "서울 강남구 논현로 86길 11",
                "수동휠체어 보조 가능하신 분", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d02, scMap.get("SC005"),
                "합정동 주 2회 청소 가사 도움", "주방·화장실 청소 및 빨래 개기 도움이 필요합니다. 지체장애로 혼자 하기 어렵습니다.",
                LocalDateTime.of(2026, 7, 23, 10, 0), LocalDateTime.of(2026, 7, 23, 13, 0),
                "서울 마포구 양화로 188", "203호", "서울특별시", "마포구", "합정동", "04033",
                new BigDecimal("37.549200"), new BigDecimal("126.914100"), null, null,
                "엘리베이터 있는 빌라, 주차 가능", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d03, scMap.get("SC001"),
                "성동구 복지관 방문 이동 보조", "시각장애 안내견이 있습니다. 안내견과 함께 이동 보조 가능하신 분 부탁드립니다.",
                LocalDateTime.of(2026, 7, 25, 14, 0), LocalDateTime.of(2026, 7, 25, 17, 0),
                "서울 성동구 왕십리로 399", null, "서울특별시", "성동구", "성수동1가", "04780",
                new BigDecimal("37.544571"), new BigDecimal("127.055739"),
                "서울 성동구 사근동 105-1", "서울 성동구 왕십리로 399",
                "안내견 동반 이동 경험자 우대", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d04, scMap.get("SC004"),
                "국민건강보험공단 방문 대기 동행", "장애인 등록 갱신 업무입니다. 긴 대기 시간이 예상됩니다.",
                LocalDateTime.of(2026, 7, 28, 9, 0), LocalDateTime.of(2026, 7, 28, 12, 0),
                "서울 영등포구 국제금융로8길 32", null, "서울특별시", "영등포구", "여의도동", "07340",
                new BigDecimal("37.521000"), new BigDecimal("126.924000"),
                "서울 성동구 성수일로 89", "서울 영등포구 국제금융로8길 32",
                "뇌병변 장애, 의사소통 보조도 함께 부탁드립니다", HelpRequestStatus.OPEN));

        helpRequestRepository.save(buildRequest(d05, scMap.get("SC006"),
                "여의도 한강공원 나들이 외출 지원", "오랜만에 바깥 나들이를 나가고 싶습니다. 전동스쿠터 이동 보조입니다.",
                LocalDateTime.of(2026, 7, 30, 11, 0), LocalDateTime.of(2026, 7, 30, 15, 0),
                "서울 영등포구 여의공원로 68", null, "서울특별시", "영등포구", "여의도동", "07340",
                new BigDecimal("37.521000"), new BigDecimal("126.924000"),
                "서울 영등포구 여의나루로 61", "서울 영등포구 여의공원로 68",
                "전동스쿠터 동행 경험 있으신 분", HelpRequestStatus.OPEN));

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

        // MATCHED 요청 2개 (7월 중)
        HelpRequest matchedReq = helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "여의도 공원 산책 동행", "휠체어 이동 보조가 필요합니다. 한강공원 산책 예정입니다.",
                LocalDateTime.of(2026, 7, 12, 10, 0), LocalDateTime.of(2026, 7, 12, 13, 0),
                "서울 영등포구 여의공원로 68", null, "서울특별시", "영등포구", "여의도동", "07340",
                new BigDecimal("37.521000"), new BigDecimal("126.924000"),
                "서울 영등포구 여의나루로 61", "서울 영등포구 여의공원로 68",
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

        // MATCHED 두 번째 — c03 수락
        HelpRequest matchedReq2 = helpRequestRepository.save(buildRequest(d03, scMap.get("SC002"),
                "종로구 안과 정기 검진 동행", "시각장애 정기 검진입니다. 병원 안내 및 이동 보조 부탁드립니다.",
                LocalDateTime.of(2026, 7, 16, 9, 30), LocalDateTime.of(2026, 7, 16, 12, 0),
                "서울 종로구 대학로 101", null, "서울특별시", "종로구", "혜화동", "03086",
                new BigDecimal("37.582551"), new BigDecimal("127.001650"),
                "서울 종로구 창경궁로 35", "서울 종로구 대학로 101",
                null, HelpRequestStatus.MATCHED));

        HelpRequestApplication matchedApp2 = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(matchedReq2).caregiver(c03).status(ApplicationStatus.ACCEPTED).build());
        Conversation matchedConv2 = conversationRepository.save(Conversation.builder()
                .application(matchedApp2).requester(d03).caregiver(c03)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(matchedConv2).sender(c03.getUser())
                .body("안녕하세요! 안과 정기검진 동행 지원했습니다. 시각장애 안내 경험이 있습니다. 당일 일정 미리 공유해 주시면 감사하겠습니다.")
                .hasRead(false).sentAt(LocalDateTime.now())
                .build());

        // COMPLETED 요청 2개 (리뷰 있음)
        HelpRequest completedReq = helpRequestRepository.save(buildRequest(d02, scMap.get("SC002"),
                "어린이대공원 근처 병원 동행", "소아 재활병원 정기 진료 동행입니다.",
                LocalDateTime.of(2026, 7, 4, 9, 0), LocalDateTime.of(2026, 7, 4, 12, 0),
                "서울 광진구 능동로 216", null, "서울특별시", "광진구", "능동", "05045",
                new BigDecimal("37.546000"), new BigDecimal("127.084491"), "서울 광진구 능동로 209", "서울 광진구 능동로 120길 4",
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

        // COMPLETED 두 번째 — d05 / c02, 리뷰 포함
        HelpRequest completedReq2 = helpRequestRepository.save(buildRequest(d05, scMap.get("SC006"),
                "잠실 롯데월드몰 쇼핑 외출 지원", "지체장애로 혼자 쇼핑하기 어렵습니다. 이동 보조 및 짐 운반 도움이 필요했습니다.",
                LocalDateTime.of(2026, 7, 6, 13, 0), LocalDateTime.of(2026, 7, 6, 17, 0),
                "서울 송파구 올림픽로 240", null, "서울특별시", "송파구", "신천동", "05551",
                new BigDecimal("37.513700"), new BigDecimal("127.100000"),
                "서울 송파구 잠실동 40-1", "서울 송파구 올림픽로 240",
                null, HelpRequestStatus.COMPLETED));

        HelpRequestApplication completedApp2 = helpRequestApplicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(completedReq2).caregiver(c02).status(ApplicationStatus.COMPLETED).build());
        Conversation completedConv2 = conversationRepository.save(Conversation.builder()
                .application(completedApp2).requester(d05).caregiver(c02)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now())
                .build());
        messageRepository.save(Message.builder()
                .conversation(completedConv2).sender(c02.getUser())
                .body("안녕하세요! 쇼핑 외출 잘 마쳤습니다. 즐거운 시간이었으면 좋겠네요!")
                .hasRead(true).sentAt(LocalDateTime.now())
                .build());
        reviewRepository.save(Review.builder()
                .helpRequest(completedReq2)
                .author(d05)
                .target(c02)
                .rating((short) 4)
                .body("친절하게 도와주셨어요. 짐도 많이 들어주시고 이동도 꼼꼼하게 챙겨주셨습니다. 조금 시간이 촉박했는데 잘 맞춰주셨어요.")
                .visibility(ReviewVisibility.PUBLIC)
                .build());

        // IN_PROGRESS 요청 2개 (양측 시작확인 완료 → 진행 중). 종료확인 핸드셰이크를 양방향으로 테스트.
        // 희망종료를 미래로 두어 autoCompleteOverdueActivities(희망종료+30분)에 의해 자동 완료되지 않게 한다.
        // 두 건 모두 이용자 d01 / 활동지원사 c01 조합이라, 테스트 계정 한 쌍으로 양쪽 마지막 확인을 모두 시연할 수 있다.

        // (1) 이용자가 먼저 종료확인 완료 → c01(활동지원사)로 로그인해 마지막 종료확인 시 COMPLETED 전이
        HelpRequest inProgressReq1 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC001"),
                "강남 도서관 외출 동행 (진행 중)", "도서관 왕복 이동 보조 중입니다. 활동지원사가 종료확인을 누르면 활동이 완료됩니다.",
                LocalDateTime.of(2026, 7, 10, 9, 0), LocalDateTime.of(2026, 7, 10, 18, 0),
                "서울 강남구 테헤란로 7길 22", null, "서울특별시", "강남구", "역삼동", "06232",
                new BigDecimal("37.500636"), new BigDecimal("127.036503"),
                "서울 강남구 테헤란로 2", "서울 강남구 테헤란로7길 22",
                "이용자 종료확인 완료, 활동지원사 종료확인 대기", HelpRequestStatus.IN_PROGRESS));

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

        // (2) 활동지원사가 먼저 종료확인 완료 → d01(이용자)로 로그인해 마지막 종료확인 시 COMPLETED 전이
        HelpRequest inProgressReq2 = helpRequestRepository.save(buildRequest(d01, scMap.get("SC002"),
                "정형외과 진료 동행 (진행 중)", "진료 동행 중입니다. 이용자가 종료확인을 누르면 활동이 완료됩니다.",
                LocalDateTime.of(2026, 7, 14, 10, 0), LocalDateTime.of(2026, 7, 14, 17, 0),
                "서울 강남구 봉은사로 114", null, "서울특별시", "강남구", "삼성동", "06120",
                new BigDecimal("37.513200"), new BigDecimal("127.062800"),
                "서울 강남구 봉은사로 417", "서울 강남구 봉은사로 114",
                "활동지원사 종료확인 완료, 이용자 종료확인 대기", HelpRequestStatus.IN_PROGRESS));

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

        log.info("더미 도움 요청 초기화 완료 (OPEN=14, MATCHED=2, IN_PROGRESS=2, COMPLETED=2) — 총 20건, 7/1~7/30");
    }

    // ─── 마켓 카테고리 ─────────────────────────────────────────────────────────────

    private void initMarketCategories() {
        marketCategoryRepository.save(MarketCategory.builder().name("이동 보조기기").description("휠체어(수동·전동), 전동스쿠터, 보행보조차(워커), 목발, 지팡이 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("욕창·체압 관리").description("에어매트리스, 욕창방지 방석·쿠션, 체위변환 보조용품 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("목욕·위생 보조").description("샤워 보조의자, 이동형 변기, 목욕 리프트, 미끄럼방지 용품 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("의사소통·감각 보조").description("AAC 기기, 점자 디스플레이, 청각 보조기기, 시각 보조기기 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("재활·훈련 용품").description("기립 훈련 보조대, 재활 운동기구, 감각통합 교구, 보조 장갑 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("생활 안전 용품").description("안전 손잡이(그랩바), 경사로(램프), 낙상 감지기, 미끄럼방지 매트 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("복지 가구·침구").description("전동침대, 높낮이 조절 책상·식탁, 사이드레일, 이동형 리프트 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("식이·영양 보조").description("연하식 조리기(다지기·믹서), 특수 식기·컵, 급식 보조도구 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("도서·교육 자료").description("장애·복지·재활 관련 서적, 사회복지사 교재, 점자 도서 등").build());
        marketCategoryRepository.save(MarketCategory.builder().name("기타").description("위 카테고리에 해당하지 않는 장애·복지 관련 물품").build());
        log.info("마켓 카테고리 초기화 완료");
    }

    // ─── 공지사항 더미 데이터 ──────────────────────────────────────────────────────

    @Transactional
    public void initDummyNotices() {
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();

        noticeRepository.save(Notice.builder()
                .author(admin)
                .title("[필독] 이음 서비스 이용 가이드 및 주요 규정 안내")
                .body("""
                        안녕하세요, 이음 운영팀입니다.

                        이음 서비스를 처음 이용하시는 분들을 위해 주요 이용 규정과 안내 사항을 정리했습니다.

                        ■ 매칭 서비스 이용 규정
                        1. 도움 요청 등록 후 활동지원사가 지원하면 채팅을 통해 세부 사항을 협의하세요.
                        2. 활동 시작 시 양측 모두 앱에서 [시작 확인]을 눌러주세요.
                        3. 활동 종료 후 양측 [종료 확인]이 완료되어야 매칭이 정상 종료됩니다.
                        4. 허위 지원, 노쇼, 부당 취소는 이용 제한 사유가 됩니다.

                        ■ 안전한 서비스 이용을 위한 당부
                        - 개인 연락처, 금전 거래는 앱 외부에서 진행하지 마세요.
                        - 불편 사항은 문의 게시판을 통해 신고해 주세요.

                        감사합니다.
                        """)
                .isPinned(true)
                .isPublic(true)
                .build());

        noticeRepository.save(Notice.builder()
                .author(admin)
                .title("[공지] 이음마켓 서비스 오픈 안내")
                .body("""
                        안녕하세요, 이음 운영팀입니다.

                        활동지원사 회원 간 중고 물품을 거래할 수 있는 이음마켓이 오픈되었습니다.

                        ■ 이음마켓 이용 안내
                        - 복지용구(휠체어, 보조기기 등) 포함 다양한 카테고리의 물품을 거래할 수 있습니다.
                        - 나눔 기능을 통해 무료로 물품을 나눌 수도 있습니다.
                        - 직거래 장소는 안전한 공공장소를 이용해 주세요.
                        - 사기 거래, 허위 매물 등록 시 계정이 정지될 수 있습니다.

                        많은 이용 부탁드립니다.
                        """)
                .isPinned(true)
                .isPublic(true)
                .build());

        noticeRepository.save(Notice.builder()
                .author(admin)
                .title("[안내] 활동지원사 자격증 인증 절차 변경 안내")
                .body("""
                        안녕하세요, 이음 운영팀입니다.

                        2026년 7월 1일부터 활동지원사 자격증 인증 절차가 아래와 같이 변경됩니다.

                        ■ 변경 전: 자격증 사본 이미지 업로드
                        ■ 변경 후: 사회보장정보원 자격 조회 시스템 연동 (자동 인증)

                        기존에 인증을 완료하신 분들은 별도 재인증 없이 유지됩니다.
                        신규 가입자는 변경된 절차에 따라 인증을 진행해 주세요.

                        문의 사항은 고객센터 또는 문의 게시판을 이용해 주세요.
                        """)
                .isPinned(false)
                .isPublic(true)
                .build());

        noticeRepository.save(Notice.builder()
                .author(admin)
                .title("[점검] 2026년 7월 정기 서버 점검 안내 (7/9 새벽 2~4시)")
                .body("""
                        안녕하세요, 이음 운영팀입니다.

                        서비스 안정성 향상을 위해 아래와 같이 정기 점검을 실시합니다.

                        ■ 점검 일시: 2026년 7월 9일 (목) 새벽 2:00 ~ 4:00
                        ■ 점검 내용: DB 최적화, 서버 보안 패치 적용
                        ■ 점검 중 영향: 앱 접속 불가, 매칭 서비스 중단

                        점검 시간 중 서비스 이용이 불가하오니 참고해 주시기 바랍니다.
                        """)
                .isPinned(false)
                .isPublic(true)
                .build());

        noticeRepository.save(Notice.builder()
                .author(admin)
                .title("[업데이트] 앱 v2.1 주요 기능 개선 안내")
                .body("""
                        안녕하세요, 이음 운영팀입니다.

                        v2.1 업데이트로 아래 기능이 개선되었습니다.

                        ■ 주요 변경 사항
                        1. 지도 화면 성능 최적화 (마커 클러스터링 적용)
                        2. 채팅 미읽음 알림 실시간 반영
                        3. 추천 알고리즘 개선 (거리·가용시간 가중치 조정)
                        4. 이음마켓 검색 필터 추가 (카테고리, 가격대, 나눔 여부)

                        이용에 불편 사항이 있으시면 문의 게시판으로 남겨주세요.
                        """)
                .isPinned(false)
                .isPublic(true)
                .build());

        log.info("공지사항 더미 데이터 초기화 완료");
    }

    // ─── 문의 게시판 더미 데이터 ──────────────────────────────────────────────────

    @Transactional
    public void initDummyInquiries() {
        User admin   = userRepository.findByEmail("admin@test.com").orElseThrow();
        User d01User = userRepository.findByEmail("d01@test.com").orElseThrow();
        User d02User = userRepository.findByEmail("d02@test.com").orElseThrow();
        User d03User = userRepository.findByEmail("d03@test.com").orElseThrow();
        User c01User = userRepository.findByEmail("c01@test.com").orElseThrow();
        User c02User = userRepository.findByEmail("c02@test.com").orElseThrow();

        // 답변 완료된 문의 3건
        Inquiry i1 = inquiryRepository.save(Inquiry.builder()
                .author(d01User)
                .category(InquiryCategory.MATCHING)
                .title("매칭 후 활동지원사가 연락이 없어요")
                .body("지원을 수락했는데 활동지원사분이 채팅으로 연락이 없습니다. 어떻게 해야 하나요? 활동 일정이 3일 후라서 걱정됩니다.")
                .isSecret(false)
                .build());
        i1.markAnswered();
        inquiryRepository.save(i1);
        inquiryReplyRepository.save(InquiryReply.builder()
                .inquiry(i1)
                .answeredBy(admin)
                .body("안녕하세요, 이음 운영팀입니다.\n\n채팅 화면에서 직접 메시지를 먼저 보내보시는 것을 권장드립니다. 24시간 내 응답이 없으면 운영팀으로 다시 문의해 주시면 조치해 드리겠습니다. 불편을 드려 죄송합니다.")
                .build());

        Inquiry i2 = inquiryRepository.save(Inquiry.builder()
                .author(c01User)
                .category(InquiryCategory.ACCOUNT)
                .title("자격증 인증이 반려되었는데 사유를 알 수 없어요")
                .body("사회복지사 1급 자격증을 업로드했는데 반려 처리가 되었습니다. 반려 사유가 메시지로 오지 않아서 무엇이 문제인지 모르겠습니다.")
                .isSecret(true)
                .build());
        i2.markAnswered();
        inquiryRepository.save(i2);
        inquiryReplyRepository.save(InquiryReply.builder()
                .inquiry(i2)
                .answeredBy(admin)
                .body("안녕하세요, 이음 운영팀입니다.\n\n업로드하신 이미지가 일부 잘려 자격증 번호가 확인되지 않았습니다. 자격증 전체가 보이도록 다시 업로드해 주시면 신속히 처리해 드리겠습니다.")
                .build());

        Inquiry i3 = inquiryRepository.save(Inquiry.builder()
                .author(d02User)
                .category(InquiryCategory.SERVICE)
                .title("리뷰 작성 후 수정이 안 되나요?")
                .body("활동 완료 후 리뷰를 작성했는데 오타가 있어서 수정하려고 했더니 수정 버튼이 없습니다. 수정 기능이 없는 건가요?")
                .isSecret(false)
                .build());
        i3.markAnswered();
        inquiryRepository.save(i3);
        inquiryReplyRepository.save(InquiryReply.builder()
                .inquiry(i3)
                .answeredBy(admin)
                .body("안녕하세요, 이음 운영팀입니다.\n\n현재 리뷰 수정 기능은 지원되지 않습니다. 수정이 필요하시면 본 문의에 수정하실 내용을 남겨주시면 운영팀에서 직접 수정해 드리겠습니다.")
                .build());

        // 대기 중 문의 3건
        inquiryRepository.save(Inquiry.builder()
                .author(c02User)
                .category(InquiryCategory.MARKET)
                .title("이음마켓 게시글 수정 후 사진이 사라졌어요")
                .body("마켓 게시글 내용을 수정했더니 기존에 등록했던 사진 3장이 모두 사라졌습니다. 다시 복구가 가능한가요?")
                .isSecret(false)
                .build());

        inquiryRepository.save(Inquiry.builder()
                .author(d03User)
                .category(InquiryCategory.MATCHING)
                .title("도움 요청 삭제가 안 됩니다")
                .body("지원자가 있는 요청을 삭제하려고 했는데 삭제가 안 됩니다. 지원자가 있어도 취소할 수 있는 방법이 있나요? 일정이 바뀌어서 꼭 취소해야 합니다.")
                .isSecret(false)
                .build());

        inquiryRepository.save(Inquiry.builder()
                .author(d01User)
                .category(InquiryCategory.ETC)
                .title("회원 탈퇴 후 데이터는 어떻게 되나요?")
                .body("탈퇴를 고려 중인데, 탈퇴하면 기존 매칭 이력과 리뷰 데이터가 어떻게 처리되는지 궁금합니다. 개인정보는 즉시 삭제되나요?")
                .isSecret(true)
                .build());

        log.info("문의 게시판 더미 데이터 초기화 완료");
    }

    // ─── 이음마켓 더미 게시글 ─────────────────────────────────────────────────────

    @Transactional
    public void initDummyMarketPosts() {
        Map<String, MarketCategory> catMap = marketCategoryRepository.findAll()
                .stream().collect(Collectors.toMap(MarketCategory::getName, c -> c));

        User c01 = userRepository.findByEmail("c01@test.com").orElseThrow();
        User c02 = userRepository.findByEmail("c02@test.com").orElseThrow();
        User c03 = userRepository.findByEmail("c03@test.com").orElseThrow();
        User c04 = userRepository.findByEmail("c04@test.com").orElseThrow();
        User c05 = userRepository.findByEmail("c05@test.com").orElseThrow();
        User d01 = userRepository.findByEmail("d01@test.com").orElseThrow();
        User d02 = userRepository.findByEmail("d02@test.com").orElseThrow();
        User d03 = userRepository.findByEmail("d03@test.com").orElseThrow();
        User d04 = userRepository.findByEmail("d04@test.com").orElseThrow();
        User d05 = userRepository.findByEmail("d05@test.com").orElseThrow();

        MarketCategory mobility    = catMap.get("이동 보조기기");
        MarketCategory pressureCare = catMap.get("욕창·체압 관리");
        MarketCategory bathCare    = catMap.get("목욕·위생 보조");
        MarketCategory commSensory = catMap.get("의사소통·감각 보조");
        MarketCategory rehab       = catMap.get("재활·훈련 용품");
        MarketCategory safety      = catMap.get("생활 안전 용품");
        MarketCategory furniture   = catMap.get("복지 가구·침구");
        MarketCategory dietary     = catMap.get("식이·영양 보조");
        MarketCategory books       = catMap.get("도서·교육 자료");
        MarketCategory etc         = catMap.get("기타");

        // 1. 전동휠체어 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d02)
                .category(mobility)
                .title("오토복 C2000 전동휠체어 (2년 사용, 정기점검 완료)")
                .description("아버지께서 2년간 사용하신 오토복 C2000 전동휠체어입니다. 건강 호전으로 더 이상 사용하지 않아 판매합니다.\n\n배터리 잔존 용량 약 80%, 충전기 포함. 좌석 쿠션 교체 완료, 외관 기스 약간 있으나 주행에 전혀 문제 없습니다. 직접 시운전 해보실 수 있습니다.")
                .price(new BigDecimal("1800000"))
                .roadAddress("서울 종로구 창경궁로 35")
                .sido("서울특별시").sigungu("종로구").bname("혜화동").zonecode("03086")
                .latitude(new BigDecimal("37.582551")).longitude(new BigDecimal("127.001650"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 2. 경량 수동휠체어 — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(c05)
                .category(mobility)
                .title("미키 MPT-43JL 경량 수동휠체어 나눔합니다")
                .description("활동지원 현장에서 예비용으로 구비해 두었던 경량 수동휠체어입니다. 총 사용 시간이 매우 적고 상태 양호합니다.\n\n무게 11kg으로 가볍고, 접으면 승용차 트렁크에 들어갑니다. 발판, 팔걸이 모두 탈부착 가능합니다. 필요하신 분 직접 방문해서 가져가세요.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 서초구 강남대로 43")
                .sido("서울특별시").sigungu("서초구").bname("서초동").zonecode("06614")
                .latitude(new BigDecimal("37.483950")).longitude(new BigDecimal("127.032500"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 3. 전동스쿠터 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d04)
                .category(mobility)
                .title("카렉스 전동스쿠터 VX2 4륜 (실내외 겸용, 1년 사용)")
                .description("1년 사용한 4륜 전동스쿠터입니다. 실내 이동과 근거리 외출에 적합하며 주행 안정성이 뛰어납니다.\n\n최대 속도 6km/h, 1회 충전 주행 거리 약 15km. 바구니, 충전기 포함. 타이어 교체한 지 3개월 됩니다. 직접 오셔서 확인 후 구매 가능하며 서울 내 배송 협의 가능합니다.")
                .price(new BigDecimal("650000"))
                .roadAddress("서울 성동구 성수일로 89")
                .sido("서울특별시").sigungu("성동구").bname("성수동1가").zonecode("04780")
                .latitude(new BigDecimal("37.544571")).longitude(new BigDecimal("127.055739"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 4. 욕창 방지 에어매트리스 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c03)
                .category(pressureCare)
                .title("욕창 예방 교번 압력 에어매트리스 (싱글, 컴프레서 포함)")
                .description("와상 어르신 케어 중 사용하던 교번 압력 에어매트리스입니다. 일정 시간마다 압력이 교번되어 욕창을 예방합니다.\n\n싱글 사이즈(90×190cm), 소음이 작은 컴프레서 포함. 패드 세탁 완료, 위생적으로 관리된 제품입니다. 의료기기 등록 제품으로 안전합니다.")
                .price(new BigDecimal("120000"))
                .roadAddress("서울 마포구 양화로 2")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 5. 보행 보조차(워커) — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(d01)
                .category(mobility)
                .title("실내용 4륜 보행 보조차(워커) 나눔 — 뇌졸중 재활 후 사용 안 해서요")
                .description("뇌졸중 재활 기간에 사용했던 4륜 보행 보조차입니다. 회복 후 사용하지 않아 필요하신 분께 나눔합니다.\n\n높이 조절 가능(손잡이 높이 75~96cm), 좌석 및 등받이 있어 중간 중간 앉아서 쉴 수 있습니다. 브레이크 정상 작동, 전체적으로 상태 양호합니다.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 강남구 역삼로 175")
                .sido("서울특별시").sigungu("강남구").bname("역삼동").zonecode("06273")
                .latitude(new BigDecimal("37.500636")).longitude(new BigDecimal("127.036503"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 6. 욕창 방지 방석 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c01)
                .category(pressureCare)
                .title("로호 에어셀 욕창 방지 방석 (휠체어용, 40×40cm)")
                .description("휠체어 사용자를 위한 로호 에어셀 욕창 방지 방석입니다. 개별 에어셀이 체압을 고르게 분산시켜 욕창을 예방합니다.\n\n사용 횟수 10회 미만, 세척 완료 상태입니다. 커버 포함, 압력 조절 밸브 정상 작동합니다.")
                .price(new BigDecimal("80000"))
                .roadAddress("서울 강남구 테헤란로 2길 22")
                .sido("서울특별시").sigungu("강남구").bname("역삼동").zonecode("06232")
                .latitude(new BigDecimal("37.500636")).longitude(new BigDecimal("127.036503"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 7. 목욕 보조의자 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c02)
                .category(bathCare)
                .title("샤워 목욕 보조 의자 (높이 조절형, 등받이·팔걸이 있음)")
                .description("활동지원 업무 중 구매해 사용했던 샤워 보조 의자입니다. 기관 업무 전환으로 더 이상 필요하지 않아 판매합니다.\n\n높이 조절 가능(43~57cm), 등받이와 팔걸이 있어 이동이 불편한 분도 안전하게 사용할 수 있습니다. 미끄럼 방지 다리 적용. 분해하여 세척 완료 상태입니다.")
                .price(new BigDecimal("35000"))
                .roadAddress("서울 강남구 봉은사로 417")
                .sido("서울특별시").sigungu("강남구").bname("삼성동").zonecode("06120")
                .latitude(new BigDecimal("37.513200")).longitude(new BigDecimal("127.062800"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 8. 이동형 변기 — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(c04)
                .category(bathCare)
                .title("이동형 간이 변기(좌변기) 나눔합니다 — 화장실 이동 어려운 분께")
                .description("화장실 이동이 어려운 분을 위한 이동형 간이 변기입니다. 침대 옆에서 사용할 수 있어 야간 이동 위험을 줄여줍니다.\n\n내통 분리 세척 가능, 팔걸이 높이 조절 가능합니다. 구매 후 3회 사용, 꼼꼼히 세척 후 판매합니다. 필요하신 분 무료로 드립니다.")
                .price(new BigDecimal("0"))
                .roadAddress("서울특별시 노원구 화랑로 393")
                .sido("서울특별시").sigungu("노원구").bname("월계동").zonecode("01796")
                .latitude(new BigDecimal("37.617559")).longitude(new BigDecimal("127.063050"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 9. 경사로(램프) — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d03)
                .category(safety)
                .title("접이식 휠체어 경사로 알루미늄 150cm (1짝)")
                .description("현관 턱 극복용으로 사용하던 알루미늄 접이식 경사로입니다. 이사 후 계단 구조가 달라져 필요 없게 되었습니다.\n\n길이 150cm, 최대 하중 272kg. 접으면 보관과 이동이 쉽습니다. 표면 긁힘 약간 있으나 구조적 문제 없습니다.")
                .price(new BigDecimal("90000"))
                .roadAddress("서울 마포구 양화로 188")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 10. 의사소통 보완 기기(AAC) — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c05)
                .category(commSensory)
                .title("AAC 보완대체의사소통 기기 — 터치형 음성출력장치 (한국어)")
                .description("언어장애 아동을 위해 구매했던 AAC 보완대체의사소통 기기입니다. 성장하면서 일반 음성 의사소통이 가능해져 더 이상 사용하지 않아 판매합니다.\n\n화면 터치로 그림 상징을 선택하면 한국어 음성으로 출력됩니다. 충전기, 전용 케이스 포함. 소프트웨어 초기화 가능합니다.")
                .price(new BigDecimal("350000"))
                .roadAddress("서울 서초구 강남대로 43")
                .sido("서울특별시").sigungu("서초구").bname("서초동").zonecode("06614")
                .latitude(new BigDecimal("37.483950")).longitude(new BigDecimal("127.032500"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 11. 점자 디스플레이 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d03)
                .category(commSensory)
                .title("한소네 U2 MINI 점자 디스플레이 (시각장애인용, 32셀)")
                .description("시각장애가 있는 가족이 사용하던 한소네 U2 MINI 점자 디스플레이입니다. 스마트폰·PC와 블루투스 연결로 사용합니다.\n\n32셀 점자 출력, 배터리 교체 후 배터리 상태 양호. 정품 파우치, 연결 케이블 포함. 구매 희망자와 사용법 설명 가능합니다.")
                .price(new BigDecimal("480000"))
                .roadAddress("서울 마포구 양화로 188")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 12. 전동침대 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d02)
                .category(furniture)
                .title("에이스 의료용 전동침대 3모터 (싱글, 사이드레일 포함)")
                .description("와상 환자였던 가족이 사용하던 3모터 전동침대입니다. 건강 회복 후 일반 침대로 교체하여 판매합니다.\n\n등받이·무릎·높이 각각 독립 조절(3모터), 사이드 낙상 방지 레일 2개 포함. 리모컨 정상, 모터 소음 없음. 직접 분해 후 방문 수령 부탁드립니다 (서울 혜화동).")
                .price(new BigDecimal("550000"))
                .roadAddress("서울 종로구 창경궁로 35")
                .sido("서울특별시").sigungu("종로구").bname("혜화동").zonecode("03086")
                .latitude(new BigDecimal("37.582551")).longitude(new BigDecimal("127.001650"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 13. 높낮이 조절 식탁 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c03)
                .category(furniture)
                .title("휠체어 사용자용 높낮이 조절 식탁 (수동 크랭크, 60×90cm)")
                .description("휠체어를 탄 채로 식사할 수 있도록 설계된 높낮이 조절 식탁입니다. 하단 공간이 넓어 휠체어가 쉽게 들어갑니다.\n\n수동 크랭크로 높이 조절(68~90cm), 상판 화이트 상태 양호. 분해 가능하여 이동이 편리합니다.")
                .price(new BigDecimal("180000"))
                .roadAddress("서울 마포구 양화로 2")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 14. 욕실 손잡이(그랩바) 세트 — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(c01)
                .category(safety)
                .title("욕실 안전 손잡이(그랩바) 세트 나눔 — 이사로 재설치 어려워서")
                .description("이사 전 욕실에 부착했던 안전 손잡이 세트입니다. 벽 철거 시 분리한 것으로 재설치 가능합니다.\n\nL형 손잡이 1개(45cm), 일자형 손잡이 2개(60cm) 포함. 스테인레스 재질, 표면 상태 양호. 미끄럼 방지 코팅 있습니다. 설치 앵커·나사 함께 드립니다.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 강남구 테헤란로 2길 22")
                .sido("서울특별시").sigungu("강남구").bname("역삼동").zonecode("06232")
                .latitude(new BigDecimal("37.500636")).longitude(new BigDecimal("127.036503"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 15. 청각장애인용 진동 시계 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d03)
                .category(commSensory)
                .title("청각장애인용 진동 알람 손목시계 (Shake-Awake SA-310)")
                .description("청각장애인을 위한 진동 알람 손목시계입니다. 진동으로 기상 알람, 일정 알림을 전달합니다.\n\n진동 강도 조절 가능, 배터리 최근 교체. 방수 기능, 야광 디스플레이. 박스·보증서 없으나 제품 상태 매우 양호합니다.")
                .price(new BigDecimal("42000"))
                .roadAddress("서울 마포구 양화로 188")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 16. 목발 — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(d01)
                .category(rehab)
                .title("알루미늄 겨드랑이 목발 1쌍 나눔 (성인용, 높이 조절)")
                .description("골절 회복 후 사용하지 않는 목발 나눔합니다. 알루미늄 재질로 가볍고, 높이 조절이 쉽습니다.\n\n성인용 (사용자 신장 약 160~190cm 범위), 겨드랑이 패드·손잡이 그립 양호. 각 파트 세척 완료. 필요하신 분 가져가세요.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 강남구 역삼로 175")
                .sido("서울특별시").sigungu("강남구").bname("역삼동").zonecode("06273")
                .latitude(new BigDecimal("37.500636")).longitude(new BigDecimal("127.036503"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 17. 음식 다지기(연하장애용) — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c02)
                .category(dietary)
                .title("연하장애 식이 전동 다지기 (필립스 HR2500, 연속 사용 가능)")
                .description("연하(삼킴) 장애가 있는 분의 식이 준비에 쓰이던 전동 다지기입니다. 음식을 곱게 갈아 연하식을 손쉽게 만들 수 있습니다.\n\n연속 사용 30분 가능, 탈부착 세척 OK. 1년 사용, 모터 이상 없음. 다지기 날 예비 1개 포함합니다.")
                .price(new BigDecimal("38000"))
                .roadAddress("서울 강남구 봉은사로 417")
                .sido("서울특별시").sigungu("강남구").bname("삼성동").zonecode("06120")
                .latitude(new BigDecimal("37.513200")).longitude(new BigDecimal("127.062800"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 18. 낙상 감지 센서 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c04)
                .category(safety)
                .title("침대 낙상 감지 센서 알람 (압력 매트 타입, 보호자 알림 기능)")
                .description("침대 이탈 시 보호자 스마트폰으로 알람을 보내는 낙상 감지 압력 매트입니다. 치매·와상 환자 가정에서 많이 사용합니다.\n\nWi-Fi 연동 스마트폰 앱 알림, 감도 조절 가능. 배터리 또는 USB 전원. 가족 요양 종료로 판매합니다. 설정 방법 알려드립니다.")
                .price(new BigDecimal("55000"))
                .roadAddress("서울특별시 노원구 화랑로 393")
                .sido("서울특별시").sigungu("노원구").bname("월계동").zonecode("01796")
                .latitude(new BigDecimal("37.617559")).longitude(new BigDecimal("127.063050"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 19. 수동 기립 훈련 기구 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d04)
                .category(rehab)
                .title("재활용 기립 훈련 보조 기구 — 하지 마비 재활 기립 보조대")
                .description("하지 마비 재활 기간에 사용한 기립 훈련 보조대입니다. 상체 지지대와 발판으로 안전하게 기립 훈련을 도와줍니다.\n\n높이·각도 조절 가능, 최대 하중 120kg. 사용 기간 6개월, 프레임 상태 양호. 병원 재활치료 대기 기간 자택 훈련용으로 적합합니다.")
                .price(new BigDecimal("220000"))
                .roadAddress("서울 성동구 성수일로 89")
                .sido("서울특별시").sigungu("성동구").bname("성수동1가").zonecode("04780")
                .latitude(new BigDecimal("37.544571")).longitude(new BigDecimal("127.055739"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 20. 장애 아동 감각통합 교구 세트 — 나눔 (SOLD로 거래 완료 예시)
        MarketPost soldPost = MarketPost.builder()
                .seller(c03)
                .category(etc)
                .title("발달장애 아동 감각통합 교구 세트 나눔 (촉각·전정감각용)")
                .description("자폐성 장애 아동의 감각통합 치료에 사용했던 교구 세트입니다. 치료사 권유로 구매했으나 이제 필요 없어 나눔합니다.\n\n촉각 자극 패드 5종, 밸런스 쿠션, 미니 트램폴린 포함. 사용감 있으나 기능 이상 없습니다. 감각통합이 필요한 아이를 키우시는 분께 드립니다.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 마포구 양화로 2")
                .sido("서울특별시").sigungu("마포구").bname("합정동").zonecode("04033")
                .latitude(new BigDecimal("37.549200")).longitude(new BigDecimal("126.914100"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build();
        soldPost.complete();
        marketPostRepository.save(soldPost);

        // 21. 전동 스쿠터 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c04)
                .category(mobility)
                .title("미출시 전동스쿠터 D100 판매 (주행거리 200km 미만)")
                .description("거의 새것 수준의 전동스쿠터입니다. 구매 후 건강이 호전되어 사용 빈도가 줄었습니다. 배터리 완충 시 약 30km 주행 가능. 충전기·바구니 포함. 직거래만 가능합니다.")
                .price(new BigDecimal("950000"))
                .roadAddress("서울 강서구 강서로 200")
                .sido("서울특별시").sigungu("강서구").bname("화곡동").zonecode("07655")
                .latitude(new BigDecimal("37.547100")).longitude(new BigDecimal("126.850300"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 22. 욕창 예방 에어 쿠션 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d01)
                .category(pressureCare)
                .title("로호 에어쿠션 (저압 교대 방식, 넥사스 커버 포함)")
                .description("척수장애로 오래 앉아있는 분들께 추천드리는 로호 에어쿠션입니다. 저압 교대 방식으로 욕창 예방 효과가 뛰어납니다. 6개월 사용, 세탁 가능한 커버 포함. 공기압 정상.")
                .price(new BigDecimal("280000"))
                .roadAddress("서울 노원구 노원로 12")
                .sido("서울특별시").sigungu("노원구").bname("상계동").zonecode("01811")
                .latitude(new BigDecimal("37.655400")).longitude(new BigDecimal("127.063100"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 23. 목욕 리프트 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c02)
                .category(bathCare)
                .title("아쿠아텍 욕조 리프트 (전동식, 최대 130kg)")
                .description("욕조 목욕을 혼자 하기 힘드신 분들을 위한 전동식 욕조 리프트입니다. 최대 130kg까지 지지 가능. 충전식 배터리 내장, 원터치 조작. 1년 사용 후 요양원 입소로 불필요해졌습니다.")
                .price(new BigDecimal("420000"))
                .roadAddress("서울 동대문구 천호대로 50")
                .sido("서울특별시").sigungu("동대문구").bname("장안동").zonecode("02538")
                .latitude(new BigDecimal("37.574200")).longitude(new BigDecimal("127.072800"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 24. 의사소통 보조 앱 전용 태블릿 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d04)
                .category(commSensory)
                .title("AAC 전용 태블릿 (터치 감도 조절, 스탠드 포함)")
                .description("언어장애인용 AAC 앱 전용으로 설정된 태블릿입니다. 화면 터치 감도 및 속도 조절 가능. 보호 케이스·스탠드·화면보호필름 포함. AAC 앱 라이선스는 포함되지 않습니다.")
                .price(new BigDecimal("320000"))
                .roadAddress("서울 양천구 목동서로 100")
                .sido("서울특별시").sigungu("양천구").bname("목동").zonecode("07994")
                .latitude(new BigDecimal("37.526500")).longitude(new BigDecimal("126.875200"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 25. 재활 자전거 (핸드바이크) — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(c05)
                .category(rehab)
                .title("상지 재활용 핸드바이크 나눔 (병원 퇴원 후 불필요)")
                .description("뇌졸중 재활 목적으로 구매했다가 병원 치료를 마치고 퇴원 후 집에서 사용하지 않아 나눔합니다. 손잡이 회전 방향 양방향, 저항 조절 8단계. 직접 가져가실 분만 연락 주세요.")
                .price(new BigDecimal("0"))
                .roadAddress("경기 부천시 소사로 150")
                .sido("경기도").sigungu("부천시").bname("소사본동").zonecode("14595")
                .latitude(new BigDecimal("37.479900")).longitude(new BigDecimal("126.802700"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 26. 안전 손잡이 세트 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d03)
                .category(safety)
                .title("욕실·화장실 안전바 세트 (스테인리스, 설치 자재 포함)")
                .description("이사로 인해 이전 집에 설치했던 안전바를 교체하면서 여분을 판매합니다. 스테인리스 재질로 녹이 슬지 않습니다. L형 2개, 일자형 2개 총 4개 세트. 벽 앵커·볼트 포함.")
                .price(new BigDecimal("85000"))
                .roadAddress("서울 구로구 가마산로 120")
                .sido("서울특별시").sigungu("구로구").bname("구로동").zonecode("08288")
                .latitude(new BigDecimal("37.494800")).longitude(new BigDecimal("126.887300"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 27. 전동 침대 리모컨 호환 부품 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(c01)
                .category(furniture)
                .title("지누스 전동침대 리모컨·리니어 모터 부품 판매")
                .description("지누스 전동침대 사용 중 리모컨 오작동으로 교체 후 남은 정품 부품입니다. 리모컨 1개, 리니어 모터(허리 부분) 1개. 전동침대 수리에 필요하신 분께 판매합니다.")
                .price(new BigDecimal("75000"))
                .roadAddress("서울 관악구 관악로 100")
                .sido("서울특별시").sigungu("관악구").bname("신림동").zonecode("08772")
                .latitude(new BigDecimal("37.481400")).longitude(new BigDecimal("126.952600"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 28. 경관급식 펌프 — 판매
        marketPostRepository.save(MarketPost.builder()
                .seller(d05)
                .category(dietary)
                .title("프리카 경관급식 펌프 세트 (비위관 급식용)")
                .description("가정 간호 중 사용하던 경관급식 펌프입니다. 정밀 유량 조절 가능, 배터리·AC 겸용. 튜브 연결 규격 표준형. 위생 세척 완료 후 판매합니다. 의료기기 등록 제품입니다.")
                .price(new BigDecimal("180000"))
                .roadAddress("서울 강북구 솔샘로 60")
                .sido("서울특별시").sigungu("강북구").bname("번동").zonecode("01101")
                .latitude(new BigDecimal("37.637700")).longitude(new BigDecimal("127.026300"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build());

        // 29. 점자 관련 도서 세트 — 나눔
        marketPostRepository.save(MarketPost.builder()
                .seller(c03)
                .category(books)
                .title("시각장애 자립 생활 관련 도서·교재 나눔 (10권)")
                .description("시각장애인 자립생활 교육 과정에서 사용한 교재 및 참고 도서 10권을 나눔합니다. 점자 학습서 3권, 보조기기 활용 안내서 4권, 생활 복지 안내 3권. 필요하신 분 가져가세요.")
                .price(new BigDecimal("0"))
                .roadAddress("서울 동작구 노량진로 110")
                .sido("서울특별시").sigungu("동작구").bname("노량진동").zonecode("06954")
                .latitude(new BigDecimal("37.513200")).longitude(new BigDecimal("126.942300"))
                .status(MarketPostStatus.ACTIVE).sharing(true)
                .build());

        // 30. 전동휠체어 배터리 — 판매 (RESERVED)
        MarketPost reservedPost = MarketPost.builder()
                .seller(c02)
                .category(mobility)
                .title("전동휠체어 교체용 리튬 배터리 24V 20Ah (미사용)")
                .description("전동휠체어 배터리 교체를 위해 구매했으나 기종이 달라 사용하지 못한 미개봉 배터리입니다. 24V 20Ah 리튬이온, 순정 BMS 탑재. 구매 영수증 있습니다. 전동휠체어 수리 또는 교체가 필요하신 분 연락 주세요.")
                .price(new BigDecimal("220000"))
                .roadAddress("서울 중랑구 망우로 88")
                .sido("서울특별시").sigungu("중랑구").bname("망우동").zonecode("02084")
                .latitude(new BigDecimal("37.597800")).longitude(new BigDecimal("127.095600"))
                .status(MarketPostStatus.ACTIVE).sharing(false)
                .build();
        reservedPost.reserve();
        marketPostRepository.save(reservedPost);

        log.info("이음마켓 더미 게시글 초기화 완료 (30건)");
        initMarketPostImages();
    }

    private void initMarketPostImages() {
        // post_id → [display_order, imageUrl] 목록 (실서버 DB 기준 하드코딩)
        Object[][] data = {
            {6L,  0, "/uploads/market/1782753608683_6_0.jpg"},
            {27L, 0, "/uploads/market/1782753671907_27_0.jpg"},
            {17L, 0, "/uploads/market/1782753698549_17_0.jpg"},
            {7L,  0, "/uploads/market/1782753755388_7_0.jpg"},
            {7L,  1, "/uploads/market/1782753755390_7_1.jpg"},
            {7L,  2, "/uploads/market/1782753755392_7_2.jpg"},
            {30L, 0, "/uploads/market/1782753852300_30_0.jpg"},
            {23L, 0, "/uploads/market/1782753949975_23_0.jpg"},
            {13L, 0, "/uploads/market/1782753981777_13_0.jpg"},
            {4L,  0, "/uploads/market/1782754026038_4_0.jpg"},
            {18L, 0, "/uploads/market/1782754112440_18_0.jpg"},
            {18L, 1, "/uploads/market/1782754112442_18_1.jpg"},
            {18L, 2, "/uploads/market/1782754112444_18_2.jpg"},
            {18L, 3, "/uploads/market/1782754112446_18_3.jpg"},
            {21L, 0, "/uploads/market/1782754308908_21_0.jpg"},
            {21L, 1, "/uploads/market/1782754308910_21_1.jpg"},
            {10L, 0, "/uploads/market/1782754619413_10_0.jpg"},
            {22L, 0, "/uploads/market/1782754699326_22_0.jpg"},
            {12L, 0, "/uploads/market/1782754828257_12_0.jpg"},
            {12L, 1, "/uploads/market/1782754828260_12_1.jpg"},
            {1L,  0, "/uploads/market/1782754864669_1_0.jpg"},
            {15L, 0, "/uploads/market/1782754893592_15_0.jpg"},
            {11L, 0, "/uploads/market/1782754904714_11_0.jpg"},
            {9L,  0, "/uploads/market/1782754986588_9_0.jpg"},
            {9L,  1, "/uploads/market/1782754986591_9_1.jpg"},
            {9L,  2, "/uploads/market/1782754986592_9_2.jpg"},
            {26L, 0, "/uploads/market/1782755015915_26_0.jpg"},
            {24L, 0, "/uploads/market/1782755070770_24_0.jpg"},
            {19L, 0, "/uploads/market/1782755123902_19_0.jpg"},
            {3L,  0, "/uploads/market/1782755166147_3_0.jpg"},
            {3L,  1, "/uploads/market/1782755166149_3_1.jpg"},
            {3L,  2, "/uploads/market/1782755166151_3_2.jpg"},
            {28L, 0, "/uploads/market/1782755297153_28_0.jpg"},
            // 2026-07-01 추가분
            {14L, 0, "/uploads/market/1782835015392_14_0.jpg"},
            {14L, 1, "/uploads/market/1782835015403_14_1.jpg"},
            {14L, 2, "/uploads/market/1782835015407_14_2.jpg"},
            {14L, 3, "/uploads/market/1782835015409_14_3.jpg"},
            {29L, 0, "/uploads/market/1782835160561_29_0.jpg"},
            {29L, 1, "/uploads/market/1782835160563_29_1.jpg"},
            {8L,  0, "/uploads/market/1782835454711_8_0.jpg"},
            {8L,  1, "/uploads/market/1782835454713_8_1.jpg"},
            {8L,  2, "/uploads/market/1782835454715_8_2.jpg"},
            {2L,  0, "/uploads/market/1782835828607_2_0.jpg"},
            {2L,  1, "/uploads/market/1782835828609_2_1.jpg"},
            {2L,  2, "/uploads/market/1782835828611_2_2.jpg"},
            {25L, 0, "/uploads/market/1782835841229_25_0.jpg"},
            {25L, 1, "/uploads/market/1782835841230_25_1.jpg"},
            {16L, 0, "/uploads/market/1782835861311_16_0.jpg"},
            {5L,  0, "/uploads/market/1782835898189_5_0.jpg"},
            {5L,  1, "/uploads/market/1782835898190_5_1.jpg"},
            {5L,  2, "/uploads/market/1782835898194_5_2.jpg"},
        };
        int count = 0;
        for (Object[] row : data) {
            Long postId = (Long) row[0];
            int displayOrder = (int) row[1];
            String imageUrl = (String) row[2];
            marketPostRepository.findById(postId).ifPresent(post -> {
                boolean exists = marketPostImageRepository
                        .findByPost_IdOrderByDisplayOrderAsc(post.getId())
                        .stream().anyMatch(img -> img.getImageUrl().equals(imageUrl));
                if (!exists) {
                    marketPostImageRepository.save(MarketPostImage.builder()
                            .post(post)
                            .imageUrl(imageUrl)
                            .displayOrder(displayOrder)
                            .build());
                }
            });
            count++;
        }
        log.info("이음마켓 이미지 등록 완료 — {}건 처리", count);
    }

    // ─── popup 더미 데이터 ───────────────────────────────────────────────────────
    private void initializePopups() {
        // 리포지토리를 통해 데이터베이스에 저장
        popupRepository.save(Popup.builder()
                .name("오픈 이벤트")
                .content("<figure class=\"image\">\n" +
                        "  <img style=\"aspect-ratio:1024/1536;\" src=\"/uploads/popups/editor/d99c40da-c6a0-41dc-b0b4-0ddde0673b17.jpg\" width=\"1024\" height=\"1536\">\n" +
                        " </figure>")
                .linkUrl("https://ieumcare.shop/register/type-select")
                .layout("PORTRAIT")
                .duration("MONTH_1")
                .enabled(true)
                .expiresAt(LocalDateTime.of(2026, 7, 31, 2, 17, 11, 177081))
                .build());

        log.info("더미 팝업 초기화 완료 총 1건");
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
