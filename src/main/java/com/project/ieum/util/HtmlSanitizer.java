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
                // CKEditor 5가 이미지를 <figure class="image"> 로 감싸므로 허용
                .addTags("s", "del", "ins", "span", "pre", "figure", "figcaption")
                .addAttributes(":all", "class", "style")
                // img src: "#" = Jsoup 와일드카드(어떤 값이든 허용).
                // 이미지 src로는 JavaScript 실행이 불가능하므로 XSS 위험 없음.
                // "/uploads/..." 같은 상대경로도 통과시키기 위해 사용.
                .addProtocols("img", "src", "#")
                // a href: http/https 만 허용 → javascript: 차단
                .removeProtocols("a", "href", "ftp")
                .addProtocols("a", "href", "http", "https")
                .preserveRelativeLinks(true);
    }

    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        // base URI를 주어야 href="#section" 같은 앵커 링크도 절대 URL로 검증됨
        return Jsoup.clean(html, "http://localhost", SAFELIST);
    }
}
