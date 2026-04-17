package com.pms.coupon.domain.coupon.redis;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.enums.MemberRequestAcquireResult;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponIssueRedisService {

    private static final String REQUESTED_COUNT_KEY_PREFIX = "coupon:requested-count:";
    private static final String ISSUED_COUNT_KEY_PREFIX = "coupon:issued-count:";
    private static final String MEMBER_REQUEST_KEY_PREFIX = "coupon:req:";

    private static final String PROCESSING = "PROCESSING"; // 요청 처리 중
    private static final String DONE = "DONE";             // 발급 완료

    private final StringRedisTemplate redisTemplate;

    public CouponIssueRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 쿠폰 Redis 키 초기화 (최초 1회)
     * - requestedCount: 요청 수
     * - issuedCount: 실제 발급 수
     */
    public void initializeCouponKeys(Long couponId, long initialIssuedCount, Duration ttl) {
        redisTemplate.opsForValue()
            .setIfAbsent(requestedCountKey(couponId), String.valueOf(initialIssuedCount), ttl);

        redisTemplate.opsForValue()
            .setIfAbsent(issuedCountKey(couponId), String.valueOf(initialIssuedCount), ttl);
    }

    /**
     * 쿠폰 Redis 키 존재 여부 확인
     */
    public boolean hasCouponKeys(Long couponId) {
        Boolean hasRequestedCountKey = redisTemplate.hasKey(requestedCountKey(couponId));
        Boolean hasIssuedCountKey = redisTemplate.hasKey(issuedCountKey(couponId));

        return Boolean.TRUE.equals(hasRequestedCountKey)
            && Boolean.TRUE.equals(hasIssuedCountKey);
    }

    /**
     * 사용자 요청 선점 (중복 요청 방지)
     * - 최초 요청: ACQUIRED
     * - 이미 완료: ALREADY_DONE
     * - 진행 중: IN_PROGRESS
     */
    public MemberRequestAcquireResult acquireMemberRequest(Long couponId, Long memberId, Duration ttl) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(memberRequestKey(couponId, memberId), PROCESSING, ttl);

        if (Boolean.TRUE.equals(result)) {
            return MemberRequestAcquireResult.ACQUIRED;
        }

        String requestState = redisTemplate.opsForValue().get(memberRequestKey(couponId, memberId));

        if (DONE.equals(requestState)) {
            return MemberRequestAcquireResult.ALREADY_DONE;
        }

        return MemberRequestAcquireResult.IN_PROGRESS;
    }

    /**
     * 요청 수 증가 (선착순 제어용)
     */
    public long increaseRequestedCount(Long couponId) {
        Long result = redisTemplate.opsForValue().increment(requestedCountKey(couponId));

        if (result == null) {
            throw new BusinessCustomException(ResponseCode.COUPON_REDIS_OPERATION_FAILED);
        }

        return result;
    }

    /**
     * 요청 수 감소 (롤백)
     */
    public void decreaseRequestedCount(Long couponId) {
        redisTemplate.opsForValue().decrement(requestedCountKey(couponId));
    }

    /**
     * 실제 발급 수 증가 (정합성용)
     */
    public void increaseIssuedCount(Long couponId) {
        Long result = redisTemplate.opsForValue().increment(issuedCountKey(couponId));

        if (result == null) {
            throw new BusinessCustomException(ResponseCode.COUPON_REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 사용자 요청 완료 처리 (멱등성 보장)
     */
    public void markMemberRequestDone(Long couponId, Long memberId, Duration ttl) {
        redisTemplate.opsForValue()
            .set(memberRequestKey(couponId, memberId), DONE, ttl);
    }

    /**
     * 사용자 요청 상태 제거 (실패 시 재시도 가능)
     */
    public void deleteMemberRequest(Long couponId, Long memberId) {
        redisTemplate.delete(memberRequestKey(couponId, memberId));
    }

    private String requestedCountKey(Long couponId) {
        return REQUESTED_COUNT_KEY_PREFIX + couponId;
    }

    private String issuedCountKey(Long couponId) {
        return ISSUED_COUNT_KEY_PREFIX + couponId;
    }

    private String memberRequestKey(Long couponId, Long memberId) {
        return MEMBER_REQUEST_KEY_PREFIX + couponId + ":" + memberId;
    }
}
