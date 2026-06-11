package com.project.ieum.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Quill 에디터 HTML 출력을 저장 전에 정제합니다.
 * <p>
 * 허용 태그/속성만 통과시켜 XSS(스크립트 삽입, 이벤트 핸들러, javascript: href 등)를 차단합니다.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = buildSafelist();

    private static Safelist buildSafelist() {
        return Safelist.relaxed()
                // Quill이 생성하는 추가 태그
                .addTags("s", "del", "ins", "span", "pre")
                // 인라인 스타일(색상·크기) — 위험 속성은 Jsoup이 자동 제거
                .addAttributes(":all", "class", "style")
                // 이미지: 로컬 상대경로(/uploads/...) 허용
                .addProtocols("img", "src", "http", "https", "/")
                // 링크: http·https 만 허용 (javascript: 차단)
                .removeProtocols("a", "href", "ftp")
                .addProtocols("a", "href", "http", "https", "#");
    }

    /** 신뢰할 수 없는 HTML을 허용 목록 기준으로 정제하여 반환합니다. */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, SAFELIST);
    }
}
