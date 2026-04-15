package com.pms.coupon.domain.coupon.listener;

import com.pms.coupon.domain.coupon.event.CouponCreatedEvent;
import com.pms.coupon.domain.coupon.redis.CouponIssueRedisService;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRedisSyncListener {

    private final CouponIssueRedisService couponIssueRedisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCouponCreated(CouponCreatedEvent event) {
        try {
            Duration ttl = Duration.between(LocalDateTime.now(), event.issueEndDate().plusSeconds(1));
            if (ttl.isNegative() || ttl.isZero()) {
                return;
            }

            couponIssueRedisService.initializeCouponKeys(event.couponId(), event.totalQuantity(), ttl);
        } catch (Exception e) {
            log.error(
                "Failed to initialize coupon redis keys after commit. couponId={}, totalQuantity={}, issueEndDate={}",
                event.couponId(),
                event.totalQuantity(),
                event.issueEndDate(),
                e
            );
        }
    }
}
