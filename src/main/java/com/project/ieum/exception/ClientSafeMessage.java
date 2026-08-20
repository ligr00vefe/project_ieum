package com.project.ieum.exception;

/**
 * 예외 메시지를 응답에 실어도 되는지 가른다.
 *
 * <p>{@code IllegalStateException}·{@code IllegalArgumentException}은 이 코드베이스에서 두 가지
 * 전혀 다른 뜻으로 쓰인다. 하나는 도메인 규칙 위반을 사용자에게 알리는 한국어 안내
 * ({@code "이미 지원한 요청입니다."})고, 다른 하나는 프레임워크·JDK가 만든 내부 메시지
 * ({@code "No enum constant com.project.ieum.entity.user.UserRole.XXX"},
 * {@code "Page index must not be less than zero!"})다.
 *
 * <p>전자를 일반 문구로 뭉개면 화면에서 이유를 알 수 없게 되고, 후자를 그대로 내보내면
 * 패키지 구조와 내부 사정이 응답에 실린다. 그래서 하나로 처리하지 않고 <b>출처</b>로 가른다 —
 * 예외를 만든 프레임이 우리 코드면 작성자가 쓴 문구, 아니면 일반 문구.
 *
 * <p>판정에 쓰는 것은 스택 최상단, 즉 예외가 <em>생성된</em> 지점이다. 우리 코드가
 * {@code PageRequest.of(...)}를 부르다 터진 경우 최상단은 스프링이므로 올바르게 걸러진다.
 * 스택이 비어 있으면(생성 시 스택 수집을 끈 예외) 판정할 근거가 없으니 일반 문구로 둔다.
 */
public final class ClientSafeMessage {

    /** 출처를 믿을 수 없을 때 대신 내보내는 문구. */
    public static final String FALLBACK = "요청을 처리할 수 없습니다.";

    private static final String OUR_PACKAGE = "com.project.ieum";

    private ClientSafeMessage() {
    }

    /** 이 예외가 우리 코드에서 만들어졌는가. */
    public static boolean authoredHere(Throwable e) {
        if (e == null) return false;
        StackTraceElement[] stack = e.getStackTrace();
        return stack.length > 0 && stack[0].getClassName().startsWith(OUR_PACKAGE);
    }

    /** 응답에 실어도 되는 메시지. 우리 코드가 쓴 문구가 아니면 {@link #FALLBACK}. */
    public static String of(Throwable e) {
        return of(e, FALLBACK);
    }

    /** 화면에 맞는 대체 문구가 따로 있는 경우. */
    public static String of(Throwable e, String fallback) {
        if (!authoredHere(e)) return fallback;
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? fallback : message;
    }
}
