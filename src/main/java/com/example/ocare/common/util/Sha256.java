package com.example.ocare.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 해시 계산.
 *
 * <p>이 서비스에서 해시가 필요한 곳이 여럿이라 한 곳으로 모은다.
 * <ul>
 *   <li>리프레시 토큰 — 저장소가 노출되어도 원문을 알 수 없게 하기 위해</li>
 *   <li>수집 데이터의 중복 판별 키 — 같은 측정 구간의 재전송을 차단하기 위해</li>
 * </ul>
 */
public final class Sha256 {

    private static final String ALGORITHM = "SHA-256";

    private Sha256() {
    }

    /**
     * 입력 문자열의 SHA-256 해시를 소문자 16진수 64자로 반환한다.
     */
    public static String hex(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance(ALGORITHM)
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
