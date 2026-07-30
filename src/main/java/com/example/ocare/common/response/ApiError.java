package com.example.ocare.common.response;

import java.util.List;

/**
 * 실패 응답의 오류 상세.
 *
 * <p>{@code code} 는 클라이언트가 분기 처리에 사용하는 식별자이고
 * {@code message} 는 사람이 읽는 설명이다. 메시지 문구가 바뀌어도
 * 클라이언트 로직이 깨지지 않도록 둘을 분리한다.
 *
 * @param code        오류 식별 코드 (예: MEMBER_EMAIL_DUPLICATED)
 * @param message     오류 설명
 * @param fieldErrors 요청 검증 실패 시 거부된 필드 목록. 그 외의 경우 null
 */
public record ApiError(String code, String message, List<FieldViolation> fieldErrors) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<FieldViolation> fieldErrors) {
        return new ApiError(code, message, fieldErrors);
    }

    /**
     * 어떤 필드가 왜 거부됐는지 전달한다.
     *
     * <p>회원가입처럼 입력 항목이 여러 개인 요청에서, 클라이언트가 어느 입력란에
     * 오류를 표시해야 하는지 알 수 있도록 필드 단위로 내려준다.
     */
    public record FieldViolation(String field, String message) {
    }
}
