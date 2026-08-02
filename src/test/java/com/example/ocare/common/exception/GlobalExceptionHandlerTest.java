package com.example.ocare.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전역 예외 처리기가 예외 종류별로 약속된 응답을 내리는지 검증한다.
 *
 * <p>Spring 컨텍스트 전체를 띄우지 않고 {@code standaloneSetup} 으로 테스트용 컨트롤러와
 * 예외 처리기만 조립한다. DB·Redis 없이 실행되고, 검증 대상이 예외 처리기 하나로 좁혀진다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("비즈니스 예외는 ErrorCode 에 정의된 상태와 코드로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("비즈니스 예외에 메시지를 직접 지정하면 그 메시지가 응답에 실린다")
    void businessExceptionWithCustomMessage() throws Exception {
        mockMvc.perform(get("/test/business-custom"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("본인의 레코드키가 아닙니다."));
    }

    @Test
    @DisplayName("요청 본문 검증 실패 시 거부된 필드가 모두 응답에 담긴다")
    void requestBodyValidationFailure() throws Exception {
        String body = """
                {"name": "", "email": "not-an-email"}
                """;

        mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.error.fieldErrors[*].field",
                        containsInAnyOrder("name", "email")));
    }

    @Test
    @DisplayName("본문 JSON 이 깨진 경우 400 으로 응답하며 내부 파싱 오류를 노출하지 않는다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                // 고정 문구로 대체되므로 Jackson 내부 클래스명이나 파싱 위치가 응답에 새지 않는다
                .andExpect(jsonPath("$.error.message").value("요청 본문을 해석할 수 없습니다."));
    }

    @Test
    @DisplayName("필수 파라미터가 없으면 400 으로 응답한다")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("요청 파라미터가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("파라미터 타입이 맞지 않으면 400 으로 응답한다")
    void parameterTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/param").param("size", "정수가아님"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 으로 응답하며 내부 메시지를 노출하지 않는다")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    /**
     * 예외 처리기 검증만을 위한 테스트용 컨트롤러.
     */
    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        @GetMapping("/test/business-custom")
        void businessWithMessage() {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 레코드키가 아닙니다.");
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/param")
        void param(@RequestParam int size) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("내부 구현 상세가 담긴 메시지");
        }

        record TestRequest(
                @NotBlank String name,
                @NotBlank @Email String email
        ) {
        }
    }
}
