package com.project.ieum.config;

import com.project.ieum.entity.CommunicationMethod;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.Region;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        if (adminUserMissing()) {
            initAdminUser();
        }
    }

    private void initDisabilityTypes() {
        disabilityTypeRepository.save(DisabilityType.builder().code("DT001").nameKo("지체장애").sortOrder((short) 1).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT002").nameKo("시각장애").sortOrder((short) 2).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT003").nameKo("청각장애").sortOrder((short) 3).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT004").nameKo("언어장애").sortOrder((short) 4).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT005").nameKo("자폐성장애").sortOrder((short) 5).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT006").nameKo("지적장애").sortOrder((short) 6).build());
        disabilityTypeRepository.save(DisabilityType.builder().code("DT007").nameKo("뇌병변장애").sortOrder((short) 7).build());
        log.info("장애 유형 초기화 완료");
    }

    private void initCommunicationMethods() {
        communicationMethodRepository.save(CommunicationMethod.builder().nameKo("구어").sortOrder((short) 1).build());
        communicationMethodRepository.save(CommunicationMethod.builder().nameKo("수어").sortOrder((short) 2).build());
        communicationMethodRepository.save(CommunicationMethod.builder().nameKo("글자").sortOrder((short) 3).build());
        communicationMethodRepository.save(CommunicationMethod.builder().nameKo("그림").sortOrder((short) 4).build());
        communicationMethodRepository.save(CommunicationMethod.builder().nameKo("전자기기").sortOrder((short) 5).build());
        log.info("의사소통 방식 초기화 완료");
    }

    private void initPersonalityTags() {
        personalityTagRepository.save(PersonalityTag.builder().nameKo("차분함").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("활발함").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("친절함").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("세심함").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("유머러스함").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("책임감").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("인내심").build());
        personalityTagRepository.save(PersonalityTag.builder().nameKo("적극성").build());
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

    private void initServiceCategories() {
        serviceCategoryRepository.save(ServiceCategory.builder().code("OUTING").nameKo("외출 보조").description("이동과 외출 동행이 필요할 때").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("HOSPITAL").nameKo("병원 동행").description("접수, 이동, 귀가까지 동행이 필요할 때").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("DAILY").nameKo("일상 보조").description("가사, 식사, 일상 활동 보조가 필요할 때").build());
        serviceCategoryRepository.save(ServiceCategory.builder().code("COMMUNICATION").nameKo("의사소통 보조").description("설명, 대화, 문서 이해 보조가 필요할 때").build());
        log.info("서비스 카테고리 초기화 완료");
    }

    private void initAdminUser() {
        String adminPassword = System.getenv("IEUM_ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("IEUM_ADMIN_PASSWORD가 없어 기본 관리자 계정 생성을 건너뜁니다.");
            return;
        }
        String adminEmail = System.getenv().getOrDefault("IEUM_ADMIN_EMAIL", "admin@ieum.local");
        userRepository.save(User.builder()
                .email(adminEmail)
                .phone("000-0000-0000")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("기본 관리자 계정 초기화 완료");
    }

    private boolean adminUserMissing() {
        String adminEmail = System.getenv().getOrDefault("IEUM_ADMIN_EMAIL", "admin@ieum.local");
        return userRepository.findByEmail(adminEmail).isEmpty();
    }
}
