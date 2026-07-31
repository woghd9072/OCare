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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // --- 회원 ---

    /**
     * 이미 가입된 이메일로 다시 가입을 시도한 경우.
     * 요청 값 자체는 정상이고 현재 상태와 충돌하는 것이므로 409 를 쓴다.
     */
    MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    /**
     * 이미 사용 중인 닉네임으로 가입을 시도한 경우.
     */
    MEMBER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // --- 인증 ---

    /**
     * 로그인 실패.
     */
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // --- 레코드키 ---

    /**
     * 이미 등록된 레코드키를 다시 등록하려는 경우.
     * 다른 회원이 이미 등록한 경우도 포함되며, 어느 쪽인지는 구분해 알려주지 않는다.
     */
    RECORD_KEY_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 레코드키입니다."),

    /**
     * 등록되지 않은 레코드키로 요청한 경우.
     */
    RECORD_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 레코드키입니다."),

    /**
     * 다른 회원이 등록한 레코드키에 접근하려는 경우.
     *
     * <p>404 로 감추지 않고 403 으로 구분해 응답한다.
     * 등록 API 가 이미 중복 여부(409)로 키의 존재를 드러내므로 감춰 봐야 얻는 것이 없고,
     * 사용자 입장에서는 "없는 키"와 "내 키가 아닌 키"를 구분해야 조치를 취할 수 있기 때문이다.
     */
    RECORD_KEY_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 등록한 레코드키가 아닙니다.");

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
