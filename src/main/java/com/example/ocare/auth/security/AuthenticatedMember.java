package com.example.ocare.auth.security;

/**
 * 인증된 요청 주체.
 *
 * <p>토큰에서 꺼낸 회원 식별자만 담는다. 이름이나 이메일은 토큰에 넣지 않으므로
 * 필요하면 서비스 계층이 DB 에서 조회한다.
 *
 * <p>컨트롤러는 {@code @AuthenticationPrincipal AuthenticatedMember member} 로 받는다.
 * 회원 식별자를 요청 파라미터로 받지 않는 이유는, 클라이언트가 보낸 값을 신뢰하면
 * 다른 회원의 식별자를 넣어 남의 데이터에 접근할 수 있기 때문이다.
 */
public record AuthenticatedMember(Long id) {
}
