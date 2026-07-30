package com.example.ocare.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 오류 코드.
 *
 * <p>enum 상수 이름을 그대로 응답의 {@code code} 값으로 사용한다.
 * 코드 문자열을 별도 필드로 두면 상수명과 문자열이 서로 어긋날 여지가 생기기 때문이다.
 *
 * <p>HTTP 상태를 코드에 함께 묶어 두는 이유는, 같은 상태 코드를 쓰는 오류가 여럿일 때
 * (예: 미등록 레코드키와 페이로드 형식 오류가 모두 400) 어느 상태로 응답할지
 * 예외 처리기가 판단하지 않아도 되게 하기 위함이다.
 *
 * <p>도메인별 상세 코드는 해당 기능을 구현하는 시점에 추가한다.
 */
@Getter
public enum ErrorCode {

    /**
     * 요청 값 자체가 잘못된 경우. Bean Validation 실패가 여기에 해당한다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    /**
     * 인증 정보가 없거나 유효하지 않은 경우.
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    /**
     * 인증은 되었으나 해당 리소스에 접근할 권한이 없는 경우.
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /**
     * 요청한 리소스가 존재하지 않는 경우.
     */
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    /**
     * 서버 내부 오류. 예상하지 못한 예외를 클라이언트에 노출할 때 사용한다.
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 응답에 실릴 오류 코드 문자열.
     */
    public String code() {
        return name();
    }
}
