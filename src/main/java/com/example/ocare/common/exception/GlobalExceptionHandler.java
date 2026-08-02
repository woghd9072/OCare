package com.example.ocare.common.exception;

import com.example.ocare.common.response.ApiError;
import com.example.ocare.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 전역 예외 처리기.
 *
 * <p>예외 종류에 따라 응답 모양이 달라지지 않도록 모든 실패 응답을
 * {@link ApiResponse#fail(ApiError)} 하나로 통일한다.
 *
 * <p>로그 레벨을 구분하는 이유:
 * 비즈니스 예외는 서비스가 의도적으로 거부한 정상 흐름이므로 {@code warn} 으로 남기고,
 * 예상하지 못한 예외만 스택 트레이스를 포함해 {@code error} 로 남긴다.
 * 그렇지 않으면 중복 수집 요청 같은 일상적인 거부가 에러 로그를 덮어버린다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 서비스 로직이 의도적으로 거부한 요청.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생: code={}, message={}", errorCode.code(), e.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(ApiError.of(errorCode.code(), e.getMessage())));
    }

    /**
     * {@code @RequestBody} 에 대한 Bean Validation 실패.
     *
     * <p>회원가입처럼 입력 항목이 여러 개인 요청에서 어느 필드가 왜 거부됐는지
     * 필드 단위로 내려준다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<ApiError.FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        log.warn("요청 검증 실패: {}", violations);

        return toResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage(), violations);
    }

    /**
     * 쿼리 파라미터·경로 변수에 대한 Bean Validation 실패.
     * (예: 조회 API 의 조회 기간이 허용 범위를 벗어난 경우)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<ApiError.FieldViolation> violations = e.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        log.warn("파라미터 검증 실패: {}", violations);

        return toResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage(), violations);
    }

    /**
     * 요청 본문을 읽을 수 없는 경우. 단말이 보낸 수집 페이로드의 JSON 구조나
     * 타입이 규약과 다를 때 여기로 들어온다.
     *
     * <p>파싱 실패 원인(내부 클래스명 등)이 노출되지 않도록 기본 메시지로 대체한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패: {}", e.getMessage());

        return toResponse(ErrorCode.INVALID_REQUEST, "요청 본문을 해석할 수 없습니다.", null);
    }

    /**
     * 필수 파라미터 누락 또는 타입 불일치.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameter(Exception e) {
        log.warn("파라미터 오류: {}", e.getMessage());

        return toResponse(ErrorCode.INVALID_REQUEST, "요청 파라미터가 올바르지 않습니다.", null);
    }

    /**
     * 위에서 처리되지 않은 모든 예외.
     *
     * <p>Spring MVC 가 상태 코드를 이미 정해 둔 예외({@link ErrorResponse} 구현체)는 그 상태를 존중한다.
     * 존재하지 않는 경로 요청({@code NoResourceFoundException}), 허용되지 않은 메서드,
     * 지원하지 않는 미디어 타입이 여기에 해당한다.
     * 이 분기가 없으면 404 여야 할 요청이 500 으로 응답되어, 클라이언트가 서버 장애로 오해하고
     * 재시도하게 된다.
     *
     * <p>그 외의 예외는 원인 파악을 위해 스택 트레이스를 남기되, 응답에는 내부 정보가 새지 않도록
     * {@link ErrorCode#INTERNAL_ERROR} 의 기본 메시지만 내려준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        if (e instanceof ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
            HttpStatusCode statusCode = errorResponse.getStatusCode();
            ErrorCode errorCode = statusCode.value() == HttpStatus.NOT_FOUND.value()
                    ? ErrorCode.NOT_FOUND
                    : ErrorCode.INVALID_REQUEST;
            log.warn("요청을 처리할 수 없음: status={}, message={}", statusCode.value(), e.getMessage());

            return ResponseEntity.status(statusCode)
                    .body(ApiResponse.fail(ApiError.of(errorCode.code(), errorCode.getMessage())));
        }

        log.error("처리되지 않은 예외 발생", e);

        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(
            ErrorCode errorCode, String message, List<ApiError.FieldViolation> violations) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(ApiError.of(errorCode.code(), message, violations)));
    }
}
