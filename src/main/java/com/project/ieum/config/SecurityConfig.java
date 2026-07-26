package com.project.ieum.config;

import com.project.ieum.entity.UserRole;
import com.project.ieum.service.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /** favicon, 정적 자원은 Security 필터 체인 자체를 건너뜀 */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/favicon.ico", "/robots.txt", "/sitemap.xml", "/css/**", "/js/**", "/images/**", "/assets/**", "/uploads/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/login",
                        "/register/**",
                        "/password/**",
                        "/api/auth/email/**",
                        "/how-to-use",
                        "/safe-meeting",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/assets/**",
                        "/healthz",
                        "/readyz",
                        "/notices",
                        "/notices/**",
                        "/error").permitAll()
                // 비로그인 열람 허용 — 매칭 게시판(활동지원사 경로)·이음마켓의 목록/상세는 공개.
                // 지원하기·채팅하기 같은 행위(POST)와 작성/수정/내 목록은 아래 규칙에 그대로 걸려 로그인으로 유도된다.
                // 상세는 숫자 id로만 매칭시켜 /market/new · /market/my · /market/chats 등이 새어나가지 않게 한다.
                .requestMatchers(HttpMethod.GET,
                        "/board", "/board/**",
                        "/caregiver/board", "/caregiver/board/",
                        "/caregiver/board/{id:[0-9]+}",
                        "/caregiver/board/poi-search",
                        "/market", "/market/",
                        "/market/{postId:[0-9]+}",
                        "/market/api/posts").permitAll()
                .requestMatchers("/admin/**").hasRole(UserRole.ADMIN.name())
                .requestMatchers("/disabled/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/caregiver/**").hasAnyRole("CAREGIVER", "ADMIN")
                .requestMatchers("/chat/**", "/matching/**", "/schedule/**", "/mypage/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> response.sendRedirect(resolveRedirectUrl(authentication));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String resolveRedirectUrl(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + UserRole.ADMIN.name()));
        if (admin) {
            return "/admin";
        }

        boolean caregiver = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + UserRole.CAREGIVER.name()));
        if (caregiver) {
            return "/caregiver/board";
        }

        return "/disabled/board";
    }
}
