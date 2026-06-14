package com.project.ieum.exception;

// 상태 전이 사전조건 위반 시 던진다. → 400.
//   · close: status != OPEN (이미 매칭/진행/종료된 요청은 게시자가 마감할 수 없음)
//   · delete: status != CLOSED (미CLOSED 요청의 하드 삭제 금지)
//   · repost: status != CLOSED (마감된 요청만 같은 내용으로 다시 게시 가능)
// 상황마다 메시지가 달라 message 인자를 받는다(고정 메시지가 아님).
public class InvalidRequestStateException extends BadRequestException {
    public InvalidRequestStateException(String message) {
        super(message);
    }

    public static InvalidRequestStateException cannotClose() {
        return new InvalidRequestStateException("매칭된 이후로는 취소할 수 없습니다.");
    }

    public static InvalidRequestStateException cannotDelete() {
        return new InvalidRequestStateException("마감된 요청만 삭제할 수 있습니다.");
    }

    public static InvalidRequestStateException cannotRepost() {
        return new InvalidRequestStateException("마감된 요청만 다시 게시할 수 있습니다.");
    }
}
