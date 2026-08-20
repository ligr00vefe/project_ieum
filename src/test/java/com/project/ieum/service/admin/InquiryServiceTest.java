package com.project.ieum.service.admin;

import com.project.ieum.dto.InquiryCreateForm;
import com.project.ieum.dto.admin.InquiryForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.inquiry.InquiryCategory;
import com.project.ieum.entity.inquiry.InquiryReply;
import com.project.ieum.repository.InquiryRepository;
import com.project.ieum.repository.InquiryReplyRepository;
import com.project.ieum.util.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InquiryService}의 본문 살균 회귀가드 (Mockito).
 *
 * <p>문의 본문·답변은 상세 화면이 {@code th:utext}로 그대로 그리므로, 저장 경로 어느 하나라도
 * 살균이 빠지면 저장형 XSS가 된다. 쓰기 경로는 네 개다 — 문의 등록, 문의 수정,
 * 답변 최초 등록, 답변 수정. 네 개 전부를 여기서 가드한다.
 *
 * <p>{@link HtmlSanitizer}는 mock이 아니라 실물을 쓴다. "sanitize가 호출됐다"가 아니라
 * "스크립트가 실제로 제거됐다"를 봐야 살균 규칙이 바뀔 때도 의미가 유지된다.
 */
@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    /** 대표적인 저장형 XSS 페이로드 두 종류 — 태그 삽입과 이벤트 핸들러 속성. */
    private static final String PAYLOAD =
            "<p>정상 본문</p><script>alert(1)</script><img src=\"x\" onerror=\"alert(2)\">";

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private InquiryReplyRepository inquiryReplyRepository;

    private InquiryService inquiryService;
    private User author;

    @BeforeEach
    void setUp() {
        inquiryService = new InquiryService(inquiryRepository, inquiryReplyRepository, new HtmlSanitizer());
        author = User.builder().id(1L).email("user@example.com").role(UserRole.USER).build();
    }

    private void assertSanitized(String stored) {
        assertThat(stored)
                .doesNotContain("<script")
                .doesNotContain("onerror");
        // 정상 서식은 살아남아야 한다 — 살균이 본문을 통째로 날리면 그것도 회귀다.
        assertThat(stored).contains("정상 본문");
    }

    private Inquiry inquiryOf(String body) {
        return Inquiry.builder()
                .id(10L)
                .author(author)
                .category(InquiryCategory.ETC)
                .title("제목")
                .body(body)
                .build();
    }

    @Test
    @DisplayName("문의 등록 — 본문의 스크립트가 저장 전에 제거된다")
    void createSanitizesBody() {
        InquiryCreateForm form = new InquiryCreateForm();
        form.setTitle("제목");
        form.setCategory(InquiryCategory.ETC);
        form.setBody(PAYLOAD);
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(i -> i.getArgument(0));

        inquiryService.create(form, author);

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        assertSanitized(captor.getValue().getBody());
    }

    @Test
    @DisplayName("문의 수정 — 등록을 막아도 수정으로 페이로드가 들어오지 못한다")
    void updateSanitizesBody() {
        Inquiry inquiry = inquiryOf("원래 본문");
        when(inquiryRepository.findById(10L)).thenReturn(Optional.of(inquiry));

        InquiryCreateForm form = new InquiryCreateForm();
        form.setTitle("바뀐 제목");
        form.setCategory(InquiryCategory.ETC);
        form.setBody(PAYLOAD);

        inquiryService.update(10L, form, author);

        assertSanitized(inquiry.getBody());
    }

    @Test
    @DisplayName("문의 수정 — 제목은 살균하지 않는다(th:text라 이중 이스케이프가 된다)")
    void updateKeepsTitleAsIs() {
        Inquiry inquiry = inquiryOf("원래 본문");
        when(inquiryRepository.findById(10L)).thenReturn(Optional.of(inquiry));

        InquiryCreateForm form = new InquiryCreateForm();
        form.setTitle("A & B");
        form.setCategory(InquiryCategory.ETC);
        form.setBody("본문");

        inquiryService.update(10L, form, author);

        assertThat(inquiry.getTitle()).isEqualTo("A & B");
    }

    @Test
    @DisplayName("답변 최초 등록 — 관리자 답변도 살균된다")
    void replyCreateSanitizesBody() {
        Inquiry inquiry = inquiryOf("본문");
        when(inquiryRepository.findById(10L)).thenReturn(Optional.of(inquiry));

        InquiryForm form = new InquiryForm();
        form.setBody(PAYLOAD);
        User admin = User.builder().id(2L).email("admin@example.com").role(UserRole.ADMIN).build();

        inquiryService.reply(10L, form, admin);

        ArgumentCaptor<InquiryReply> captor = ArgumentCaptor.forClass(InquiryReply.class);
        verify(inquiryReplyRepository).save(captor.capture());
        assertSanitized(captor.getValue().getBody());
    }

    @Test
    @DisplayName("답변 수정 — 이미 답변이 있는 분기도 살균된다")
    void replyUpdateSanitizesBody() {
        Inquiry inquiry = inquiryOf("본문");
        InquiryReply existing = InquiryReply.builder().id(20L).inquiry(inquiry).body("원래 답변").build();
        Inquiry withReply = inquiry.toBuilder().reply(existing).build();
        when(inquiryRepository.findById(10L)).thenReturn(Optional.of(withReply));

        InquiryForm form = new InquiryForm();
        form.setBody(PAYLOAD);
        User admin = User.builder().id(2L).email("admin@example.com").role(UserRole.ADMIN).build();

        inquiryService.reply(10L, form, admin);

        assertSanitized(existing.getBody());
    }
}
