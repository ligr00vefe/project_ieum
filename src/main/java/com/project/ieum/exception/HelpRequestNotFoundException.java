package com.project.ieum.exception;

public class HelpRequestNotFoundException extends NotFoundException {
    public HelpRequestNotFoundException(Long id) {
        super("도움 요청을 찾을 수 없습니다: " + id);
    }
}
