package com.example.ocare.recordkey.entity;

/**
 * 레코드키의 수집 상태.
 */
public enum RecordKeyStatus {

    /**
     * 수집 대상.
     */
    ACTIVE,

    /**
     * 수집 중단. 단말을 교체했거나 사용자가 연동을 해제한 경우에 해당한다.
     *
     * <p>레코드를 지우지 않고 상태만 바꾸는 이유는, 이미 수집된 과거 데이터가
     * 이 레코드키에 묶여 있어 삭제하면 조회할 수 없게 되기 때문이다.
     */
    INACTIVE
}
