package com.pms.coupon.domain.coupon.redis;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponIssueRedisService {

    private static final String LIMIT_KEY_PREFIX = "coupon:limit:";
    private static final String REQUEST_COUNT_KEY_PREFIX = "coupon:req-count:";
    private static final String MEMBER_REQUEST_KEY_PREFIX = "coupon:req:";
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";

    private final StringRedisTemplate redisTemplate;

    public CouponIssueRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void initializeCouponKeys(Long couponId, long totalLimit, Duration ttl) {
        redisTemplate.opsForValue().setIfAbsent(limitKey(couponId), String.valueOf(totalLimit), ttl);
        redisTemplate.opsForValue().setIfAbsent(requestCountKey(couponId), "0", ttl);
    }

    public boolean hasCouponKeys(Long couponId) {
        Boolean hasLimitKey = redisTemplate.hasKey(limitKey(couponId));
        Boolean hasRequestCountKey = redisTemplate.hasKey(requestCountKey(couponId));
        return Boolean.TRUE.equals(hasLimitKey) && Boolean.TRUE.equals(hasRequestCountKey);
    }

    public long getLimit(Long couponId) {
        String value = redisTemplate.opsForValue().get(limitKey(couponId));

        if (value == null) {
            throw new BusinessCustomException(ResponseCode.COUPON_REDIS_KEY_NOT_INITIALIZED);
        }

        return Long.parseLong(value);
    }

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

    public long increaseRequestCount(Long couponId) {
        Long result = redisTemplate.opsForValue().increment(requestCountKey(couponId));

        if (result == null) {
            throw new BusinessCustomException(ResponseCode.COUPON_REDIS_OPERATION_FAILED);
        }

        return result;
    }

    public void decreaseRequestCount(Long couponId) {
        redisTemplate.opsForValue().decrement(requestCountKey(couponId));
    }

    public void markMemberRequestDone(Long couponId, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(memberRequestKey(couponId, memberId), DONE, ttl);
    }

    public void deleteMemberRequest(Long couponId, Long memberId) {
        redisTemplate.delete(memberRequestKey(couponId, memberId));
    }

    private String limitKey(Long couponId) {
        return LIMIT_KEY_PREFIX + couponId;
    }

    private String requestCountKey(Long couponId) {
        return REQUEST_COUNT_KEY_PREFIX + couponId;
    }

    private String memberRequestKey(Long couponId, Long memberId) {
        return MEMBER_REQUEST_KEY_PREFIX + couponId + ":" + memberId;
    }

    public enum MemberRequestAcquireResult {
        ACQUIRED,
        IN_PROGRESS,
        ALREADY_DONE
    }
}
