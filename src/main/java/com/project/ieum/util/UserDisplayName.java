package com.project.ieum.util;

import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.profile.Profile;

/**
 * 알림·메시지처럼 남에게 보이는 문구에 사용자를 지칭할 때 쓰는 표시 이름.
 *
 * <p>{@link User}에는 이름이 없고 {@link Profile}에 있다. 그래서 이름을 얻으려면 한 단계를
 * 더 들어가야 하는데, 그게 번거로워 이메일을 대신 쓰는 자리가 생겼다. 이메일은 로그인
 * 식별자라 알림 한 줄로 남의 계정 주소가 넘어간다.
 *
 * <p>프로필이 없는 경우는 실질적으로 ADMIN 하나다(User.profile 주석 참고). 이때 이메일로
 * 되돌아가면 고치려던 문제가 그대로 남으므로, 역할에 맞는 호칭을 쓴다.
 *
 * <p><b>호출 위치 주의:</b> {@code User.profile}은 지연 연관이다. 영속성 컨텍스트가 열려 있는
 * 서비스({@code @Transactional}) 안에서 부를 것. {@code /api/**}는 OSIV 대상에서 빠져 있어
 * (WebMvcConfig 참고) 컨트롤러·DTO에서 부르면 세션이 이미 닫혀 있을 수 있다.
 */
public final class UserDisplayName {

    private static final String UNKNOWN = "익명";
    private static final String ADMIN = "관리자";

    private UserDisplayName() {
    }

    public static String of(User user) {
        if (user == null) return UNKNOWN;

        Profile profile = user.getProfile();
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName();
        }
        return user.getRole() == UserRole.ADMIN ? ADMIN : UNKNOWN;
    }
}
