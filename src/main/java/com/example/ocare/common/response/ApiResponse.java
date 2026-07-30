package com.example.ocare.common.response;

/**
 * 모든 REST API 응답을 감싸는 공통 포맷.
 *
 * <p>수집 API 는 단말(App to App)이 호출하고 조회 API 는 서비스가 호출하는데,
 * 호출 주체가 달라도 성공/실패 판별 방식은 하나로 유지되어야 한다.
 * HTTP 상태 코드만으로는 "무엇이 왜 실패했는지"를 전달하기 어려워
 * 본문에 {@code success} 플래그와 오류 상세를 함께 담는다.
 *
 * <p>{@code spring.jackson.default-property-inclusion=non_null} 설정에 의해
 * 성공 응답에서는 {@code error} 가, 실패 응답에서는 {@code data} 가 직렬화되지 않는다.
 *
 * @param <T> 응답 본문 타입
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /**
     * 반환할 데이터가 있는 성공 응답.
     *
     * <p>팩토리 이름을 {@code success} 로 두면 record 가 자동 생성하는 접근자
     * {@code success()} 와 충돌하므로 {@code ok} / {@code fail} 로 명명한다.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 반환할 데이터가 없는 성공 응답. (등록/삭제처럼 결과 본문이 불필요한 경우)
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 실패 응답. 오류 코드와 메시지는 {@link ApiError} 가 담는다.
     */
    public static ApiResponse<Void> fail(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
