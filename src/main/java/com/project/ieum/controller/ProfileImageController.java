package com.project.ieum.controller;

import com.project.ieum.entity.User;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.UserService;
import com.project.ieum.util.ImageThumbUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileImageController {

    private static final Path PRESET_DIR =
            Paths.get("src/main/resources/static/assets/profile_img");

    private final UserRepository userRepository;
    private final UserService userService;

    /** 프리셋 이미지 폴더 스캔 — user_ 파일 제외, 알파벳 순 정렬 */
    private List<String> scanPresetImages() {
        if (!Files.exists(PRESET_DIR)) return List.of();
        try (Stream<Path> stream = Files.list(PRESET_DIR)) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !name.startsWith("user_"))
                    .sorted()
                    .map(name -> "/assets/profile_img/" + name)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    /** 현재 프로필 + 프리셋 목록(폴더 스캔) + 커스텀 업로드 목록 */
    @GetMapping("/images")
    public ResponseEntity<?> getProfileImages(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String profileImageUrl = userService.getProfileImageUrl(user);
        String thumbUrl = userService.getProfileThumbUrl(user);
        List<Map<String, String>> customImages = userService.getCustomProfileImages(user.getId());
        List<String> presetImages = scanPresetImages();

        return ResponseEntity.ok(Map.of(
                "profileImageUrl", profileImageUrl != null ? profileImageUrl : "",
                "thumbUrl",        thumbUrl != null ? thumbUrl : "",
                "presetImages",    presetImages,
                "customImages",    customImages
        ));
    }

    /** 파일 업로드 → 커스텀 이미지 슬롯 저장 + 현재 프로필로 즉시 적용 */
    @PostMapping("/image")
    public ResponseEntity<?> uploadProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Map<String, String> saved = userService.saveCustomProfileImage(user.getId(), file);
        userService.updateProfileImageDirect(user, saved.get("imageUrl"));
        return ResponseEntity.ok(Map.of(
                "imageUrl", saved.get("imageUrl"),
                "thumbUrl", saved.get("thumbUrl")
        ));
    }

    /** 프리셋 또는 커스텀 이미지를 현재 프로필로 즉시 적용 */
    @PostMapping("/set")
    public ResponseEntity<?> setProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "url 필드가 필요합니다."));
        }
        userService.updateProfileImageDirect(user, url);
        String thumbUrl = ImageThumbUtil.deriveThumbUrl(url);
        return ResponseEntity.ok(Map.of(
                "profileImageUrl", url,
                "thumbUrl", thumbUrl != null ? thumbUrl : ""
        ));
    }

    /** 커스텀 업로드 이미지 삭제 */
    @DeleteMapping("/image")
    public ResponseEntity<?> deleteCustomImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String url) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.deleteCustomProfileImage(user.getId(), url);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
