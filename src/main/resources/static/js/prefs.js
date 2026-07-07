// 사용자 환경설정(글자 크기/테마) 토글 — layout/prefs.html의 FOUC 방지 스크립트와 짝을 이룬다.
(function () {
    var STORAGE_KEY = 'ieum:prefs';

    function readPrefs() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (raw) return JSON.parse(raw);
        } catch (e) {}
        return {};
    }

    function writePrefs(patch) {
        var next = Object.assign({}, readPrefs(), patch);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        } catch (e) {}
        return next;
    }

    // ── 글자 크게 보기 (2단계 토글: 100% ↔ 150%) ──
    function currentFontScale() {
        return document.documentElement.getAttribute('data-font-scale') === 'lg' ? 'lg' : 'base';
    }

    function applyFontScale(scale) {
        document.documentElement.setAttribute('data-font-scale', scale);
        writePrefs({ fontScale: scale });
        updateFontScaleButton(scale);
    }

    function updateFontScaleButton(scale) {
        var enlarged = scale === 'lg';
        // 현재 확대 상태면 "원래대로", 아니면 "크게 보기"
        var text = enlarged ? '원래대로' : '크게 보기';
        var aria = enlarged ? '글자 원래 크기로 (현재 150%)' : '글자 크게 보기 (150%)';
        ['fontScaleBtn', 'fontScaleBtnMobile'].forEach(function (id) {
            var btn = document.getElementById(id);
            if (!btn) return;
            btn.setAttribute('aria-label', aria);
            var labelEl = btn.querySelector('[data-font-scale-label]');
            if (labelEl) labelEl.textContent = text;
        });
    }

    // ── 테마 (light / dark / system) ──
    // 선택 원본값(system 포함)은 localStorage에, data-theme에는 확정값(light/dark)만 반영.
    function currentThemeSelection() {
        return readPrefs().theme || 'system';
    }

    function resolveTheme(theme) {
        if (theme !== 'system') return theme;
        try {
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        } catch (e) {
            return 'light';
        }
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', resolveTheme(theme));
        writePrefs({ theme: theme });
        updateThemeControls(theme);
    }

    // 마이페이지의 테마 선택 버튼 그룹(data-theme-option) 활성 상태 표시
    function updateThemeControls(theme) {
        document.querySelectorAll('[data-theme-option]').forEach(function (el) {
            var active = el.getAttribute('data-theme-option') === theme;
            el.setAttribute('aria-pressed', active ? 'true' : 'false');
            el.classList.toggle('theme-option-active', active);
        });
        var select = document.querySelector('[data-theme-select]');
        if (select && select.value !== theme) select.value = theme;

        // 순환 토글 버튼(data-theme-toggle)의 아이콘을 "다음에 전환될 모드"로 표시한다.
        // 순환 순서 system→dark→light이므로: 시스템=달, 다크=해, 라이트=모니터.
        // (버튼을 누르면 그 아이콘이 가리키는 모드로 바뀐다)
        var THEME_LABELS = { light: '라이트 모드', dark: '다크 모드', system: '시스템 설정 따름' };
        var CYCLE = ['system', 'dark', 'light'];
        var next = CYCLE[(CYCLE.indexOf(theme) + 1) % CYCLE.length];
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.querySelectorAll('[data-theme-icon]').forEach(function (icon) {
                icon.classList.toggle('hidden', icon.getAttribute('data-theme-icon') !== next);
            });
            var label = '테마 변경 (현재: ' + (THEME_LABELS[theme] || theme)
                      + ', 클릭 시: ' + (THEME_LABELS[next] || next) + ')';
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
        });
    }

    function init() {
        updateFontScaleButton(currentFontScale());
        updateThemeControls(currentThemeSelection());

        // 글자 크게 보기 토글
        document.querySelectorAll('[data-font-scale-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                applyFontScale(currentFontScale() === 'lg' ? 'base' : 'lg');
            });
        });

        // 테마 선택 버튼 그룹
        document.querySelectorAll('[data-theme-option]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                applyTheme(btn.getAttribute('data-theme-option'));
            });
        });

        // 테마 순환 토글(아이콘 버튼, 예: 관리자 상단바) — system → dark → light
        var THEME_CYCLE = ['system', 'dark', 'light'];
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var cur = currentThemeSelection();
                var next = THEME_CYCLE[(THEME_CYCLE.indexOf(cur) + 1) % THEME_CYCLE.length];
                applyTheme(next);
            });
        });

        // 테마 셀렉트박스(대안 UI)
        var select = document.querySelector('[data-theme-select]');
        if (select) {
            select.addEventListener('change', function () { applyTheme(select.value); });
        }

        // 시스템 설정 추종 중이면 OS 다크모드 변경에 실시간 반영
        try {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
                if (currentThemeSelection() === 'system') {
                    document.documentElement.setAttribute('data-theme', resolveTheme('system'));
                }
            });
        } catch (e) {}
    }

    // 스크립트가 DOMContentLoaded 이후 실행될 수도 있으므로 readyState로 분기한다.
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
