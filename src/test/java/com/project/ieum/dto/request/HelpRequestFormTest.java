package com.project.ieum.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HelpRequestForm#isEndAfterStart()} 회귀 가드 (Bean Validation 슬라이스).
 *
 * <p>서비스 테스트(Mockito)와 달리, 여기선 폼의 {@code @AssertTrue} 제약을 실제 {@link Validator}로
 * 구동해 검증한다. 시간 순서 역전은 구현 중 2회 발생한 회귀 지점이라, true/false 방향을 못 박아 둔다.
 *
 * <p>전략: 시간 외 모든 필드가 채워진 valid 베이스 폼({@link #validForm})에서 시간 두 필드만 바꾼다.
 * 그러면 위반이 잡혀도 그 출처가 {@code isEndAfterStart}임을 분리해 단언할 수 있다.
 *
 * <p>주의: 시작 일시는 {@code @FutureOrPresent}라 항상 미래 시각을 쓴다(시간 순서와 무관한 위반 배제).
 */
class HelpRequestFormTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("종료가 시작 이후면 시간 제약 위반이 없다")
    void endAfterStart_valid() {
        // given — 시간 외 필드는 모두 유효, 시간만 정상(+1h ~ +3h)
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        HelpRequestForm form = validForm(start, start.plusHours(2));

        // when
        Set<ConstraintViolation<HelpRequestForm>> violations = validator.validate(form);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("종료가 시작 이전이면 endAfterStart 위반이 발생한다")
    void endBeforeStart_invalid() {
        // given — 시간만 역전(start 보다 end 가 1시간 이르게)
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        HelpRequestForm form = validForm(start, start.minusHours(1));

        // when
        Set<ConstraintViolation<HelpRequestForm>> violations = validator.validate(form);

        // then — 위반은 endAfterStart 한 건뿐 (@AssertTrue 메서드명 isEndAfterStart → 경로는 "endAfterStart")
        assertThat(violations).hasSize(1);
        assertThat(violations).first()
            .extracting(v -> v.getPropertyPath().toString())
            .isEqualTo("endAfterStart");
    }

    @Test
    @DisplayName("종료와 시작이 같은 시각이면 위반이 없다 (경계: !isBefore)")
    void endEqualsStart_valid() {
        // given — start == end (동일 LocalDateTime)
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        HelpRequestForm form = validForm(start, start);

        // when
        Set<ConstraintViolation<HelpRequestForm>> violations = validator.validate(form);

        // then — !end.isBefore(start) 이므로 같은 시각은 유효
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("종료 시간이 null이면 endAfterStart 위반은 발생하지 않는다")
    void nullEnd_noTimeRangeViolation() {
        // given — end=null (종료는 선택값, isEndAfterStart 는 null 가드로 true 반환)
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        HelpRequestForm form = validForm(start, null);

        // when
        Set<ConstraintViolation<HelpRequestForm>> violations = validator.validate(form);

        // then — null 종료는 허용. 시간 순서 제약(endAfterStart)은 발동하지 않는다
        assertThat(violations).isEmpty();
    }

    /**
     * 시간 두 필드를 제외한 모든 제약을 만족하는 베이스 폼.
     * 시간만 인자로 바꿔 끼워, 위반의 출처를 isEndAfterStart로 분리한다.
     */
    private HelpRequestForm validForm(LocalDateTime start, LocalDateTime end) {
        HelpRequestForm form = new HelpRequestForm();
        form.setTitle("거동 보조 요청");
        form.setServiceCategoryId(1L);
        form.setBody("병원 동행이 필요합니다.");
        form.setDesiredStartDatetime(start);
        form.setDesiredEndDatetime(end);
        form.setRoadAddress("부산광역시 금정구 중앙대로 1");
        form.setSido("부산광역시");
        form.setSigungu("금정구");
        return form;
    }
}
