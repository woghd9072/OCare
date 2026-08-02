package com.example.ocare.auth.jwt;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;

/**
 * 토큰 검증 실패.
 *
 * <p>{@link BusinessException} 을 상속해 전역 예외 처리기가 401 로 변환하도록 한다.
 * 만료/위조/용도 불일치를 모두 같은 예외로 다루는 이유는, 어떤 이유로 거부됐는지
 * 알려주면 공격자가 유효한 토큰을 만드는 데 단서가 되기 때문이다.
 */
public class InvalidTokenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
