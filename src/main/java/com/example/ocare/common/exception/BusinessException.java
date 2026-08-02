package com.example.ocare.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 예외.
 *
 * <p>"이메일이 중복이다", "등록되지 않은 레코드키다" 처럼 서비스 로직이 의도적으로 거부하는
 * 상황에 사용한다. 예상하지 못한 시스템 오류({@link RuntimeException} 일반)와 구분해야
 * 예외 처리기가 전자는 정해진 코드와 상태로, 후자는 500 으로 내려보낼 수 있다.
 *
 * <p>{@link ErrorCode} 가 HTTP 상태와 기본 메시지를 함께 들고 있으므로
 * 던지는 쪽은 코드만 지정하면 된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 예외는 서블릿 컨테이너 등에서 직렬화될 수 있어 명시해 둔다.
     * 값을 두지 않으면 클래스가 조금만 바뀌어도 자동 생성된 값이 달라진다.
     */
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 기본 메시지 대신 상황을 더 구체적으로 설명해야 할 때 사용한다.
     * (예: 어떤 레코드키가 미등록인지 함께 알려주는 경우)
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
