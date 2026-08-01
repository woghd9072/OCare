package com.example.ocare.health.ingest;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

/**
 * 이미 처리한 키를 기억해 중복 처리를 막는다.
 *
 * <p>DB 의 UNIQUE 제약만으로도 중복 저장은 막을 수 있지만, 그 방식은 1,500건을 모두
 * INSERT 시도한 뒤 예외로 걸러내는 형태가 된다. 재전송이 정상 흐름인 서비스에서는
 * 매번 그 비용을 치르게 되므로, 저장 전에 걸러 내는 단계를 앞에 둔다.
 *
 * <p>이 저장소는 1차 방어일 뿐이다. 캐시가 비었거나 동시 요청이 겹치면 통과할 수 있으므로,
 * 최종 방어선은 여전히 DB 의 UNIQUE 제약이다.
 */
public interface IdempotencyStore {

    /**
     * 아직 표시되지 않은 키만 표시하고, 그 키들을 돌려준다.
     *
     * <p>확인과 표시를 한 번에 하는 이유는, 나눠 하면 두 요청이 동시에 "없음"을 확인하고
     * 둘 다 저장을 진행할 수 있기 때문이다.
     *
     * @return 이번에 처음 표시된 키. 이미 있던 키는 포함되지 않는다
     */
    Set<String> markIfAbsent(Collection<String> keys, Duration ttl);

    /**
     * 표시를 지운다.
     *
     * <p>저장에 실패했을 때 반드시 호출해야 한다. 표시만 남고 저장이 안 된 상태로 두면,
     * 단말이 같은 데이터를 다시 보내도 "이미 처리했다" 며 건너뛰어 데이터가 영영 유실된다.
     */
    void release(Collection<String> keys);
}
