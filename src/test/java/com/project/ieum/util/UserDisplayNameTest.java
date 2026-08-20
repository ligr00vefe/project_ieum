package com.project.ieum.util;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.user.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserDisplayName} 단위 검증.
 *
 * <p>이 유틸이 있는 이유가 "알림에 이메일을 쓰지 않는다"이므로, 어떤 입력에서도 결과에
 * {@code @}가 섞이지 않는지를 함께 가드한다. 프로필이 없는 ADMIN에서 NPE 없이 값이
 * 나오는지도 여기서 본다 — 관리자 발신은 채팅에서 실제로 지원되는 경로다.
 */
class UserDisplayNameTest {

    private static User user(UserRole role, String fullName) {
        User user = User.builder().id(1L).email("someone@example.com").role(role).build();
        if (fullName == null) return user;
        return user.toBuilder()
                .profile(UserProfile.builder().fullName(fullName).build())
                .build();
    }

    @Test
    @DisplayName("프로필이 있으면 이름을 쓴다")
    void usesFullName() {
        assertThat(UserDisplayName.of(user(UserRole.USER, "홍길동"))).isEqualTo("홍길동");
        assertThat(UserDisplayName.of(user(UserRole.CAREGIVER, "김돌봄"))).isEqualTo("김돌봄");
    }

    @Test
    @DisplayName("프로필이 없는 ADMIN — NPE 없이 '관리자'")
    void adminWithoutProfile() {
        assertThat(UserDisplayName.of(user(UserRole.ADMIN, null))).isEqualTo("관리자");
    }

    @Test
    @DisplayName("이름이 비었거나 사용자가 null이면 '익명'")
    void fallsBackToUnknown() {
        assertThat(UserDisplayName.of(user(UserRole.USER, null))).isEqualTo("익명");
        assertThat(UserDisplayName.of(user(UserRole.USER, "   "))).isEqualTo("익명");
        assertThat(UserDisplayName.of(null)).isEqualTo("익명");
    }

    @Test
    @DisplayName("어떤 경우에도 이메일로 되돌아가지 않는다")
    void neverFallsBackToEmail() {
        assertThat(UserDisplayName.of(user(UserRole.USER, null))).doesNotContain("@");
        assertThat(UserDisplayName.of(user(UserRole.ADMIN, null))).doesNotContain("@");
        assertThat(UserDisplayName.of(user(UserRole.CAREGIVER, "   "))).doesNotContain("@");
    }
}
