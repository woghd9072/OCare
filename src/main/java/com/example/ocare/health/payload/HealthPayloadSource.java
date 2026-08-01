package com.example.ocare.health.payload;

/**
 * 데이터 출처와 단말 정보.
 *
 * @param name    출처 표기. 이 값으로 삼성/애플을 판별한다
 * @param mode    출처 코드.
 * @param type    현재 데이터에서는 항상 빈 문자열이라 용도가 확인되지 않았다
 * @param product 단말 제품 정보
 */
public record HealthPayloadSource(
        String name,
        Integer mode,
        String type,
        HealthPayloadProduct product
) {

    public String productName() {
        return product == null ? null : product.name();
    }

    public String productVendor() {
        return product == null ? null : product.vender();
    }

    /**
     * 단말 제품 정보.
     *
     * @param name   제품명 (Android / iPhone)
     * @param vender 제조사.
     */
    public record HealthPayloadProduct(String name, String vender) {
    }
}
