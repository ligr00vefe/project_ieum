package com.project.ieum.service.common;

import com.project.ieum.entity.User;
import com.project.ieum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 반드시 트랜잭션 안에서 조회한다(@Transactional readOnly).
 *
 * <p>트랜잭션 없이 파생 쿼리(findByEmail)를 실행하면 Hibernate가 커넥션을 "트랜잭션 종료 시점"에
 * 반납하려다 종료할 트랜잭션이 없어 세션이 닫힐 때까지 쥐고 있는다. 이 서비스는 GlobalModelAdvice를
 * 통해 SSE 구독 요청(30분짜리 응답)에도 실행되는데, OSIV가 세션을 응답 완료까지 유지하므로
 * 트랜잭션이 없으면 SSE 연결 하나당 커넥션 하나가 30분씩 물려 풀이 고갈된다(2026-07 운영 장애).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return getByEmail(authentication.getName());
    }

    public Optional<User> getCurrentUserOrEmpty() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
