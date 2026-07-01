// 사용자 환경설정(글자 크기/테마) 토글 — layout/prefs.html의 FOUC 방지 스크립트와 짝을 이룬다.
(function () {
    var STORAGE_KEY = 'ieum:prefs';
    var FONT_SCALES = ['base', 'md', 'lg'];
    var FONT_SCALE_LABELS = { base: '100%', md: '112.5%', lg: '125%' };
    var THEMES = ['light', 'dark', 'system'];
    var THEME_LABELS = { light: '라이트 모드', dark: '다크 모드', system: '시스템 설정' };

    function readPrefs() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            if (raw) return JSON.parse(raw);
        } catch (e) {}
        return {};
    }

    function writePrefs(patch) {
        var current = readPrefs();
        var next = Object.assign({}, current, patch);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        } catch (e) {}
        return next;
    }

    function currentFontScale() {
        return document.documentElement.getAttribute('data-font-scale') || 'base';
    }

    function applyFontScale(scale) {
        document.documentElement.setAttribute('data-font-scale', scale);
        writePrefs({ fontScale: scale });
        updateFontScaleButton(scale);
    }

    function updateFontScaleButton(scale) {
        var label = '글자 크게 보기 (현재 ' + FONT_SCALE_LABELS[scale] + ')';
        ['fontScaleBtn', 'fontScaleBtnMobile'].forEach(function (id) {
            var btn = document.getElementById(id);
            if (btn) btn.setAttribute('aria-label', label);
        });
    }

    // theme: 사용자가 선택한 원본 값(light/dark/system). data-theme에는 항상 확정값(light/dark)만 반영한다.
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
        updateThemeButton(theme);
    }

    function updateThemeButton(theme) {
        var label = '테마 변경 (현재 ' + THEME_LABELS[theme] + ')';
        ['themeToggleBtn', 'themeToggleBtnMobile'].forEach(function (id) {
            var btn = document.getElementById(id);
            if (btn) btn.setAttribute('aria-label', label);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        updateFontScaleButton(currentFontScale());
        updateThemeButton(currentThemeSelection());

        document.querySelectorAll('[data-font-scale-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var scale = currentFontScale();
                var next = FONT_SCALES[(FONT_SCALES.indexOf(scale) + 1) % FONT_SCALES.length];
                applyFontScale(next);
            });
        });

        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var theme = currentThemeSelection();
                var next = THEMES[(THEMES.indexOf(theme) + 1) % THEMES.length];
                applyTheme(next);
            });
        });

        try {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
                if (currentThemeSelection() === 'system') {
                    document.documentElement.setAttribute('data-theme', resolveTheme('system'));
                }
            });
        } catch (e) {}
    });
})();
