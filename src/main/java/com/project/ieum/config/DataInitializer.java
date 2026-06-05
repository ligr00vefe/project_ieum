package com.project.ieum.config;

import com.project.ieum.entity.CommunicationMethod;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.Region;
import com.project.ieum.entity.user.DisabilityType;
import com.project.ieum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
