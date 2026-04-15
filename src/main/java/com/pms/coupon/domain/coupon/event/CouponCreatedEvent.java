package com.pms.coupon.domain.coupon.event;

import java.time.LocalDateTime;

public record CouponCreatedEvent(
    Long couponId,
    int totalQuantity,
    LocalDateTime issueEndDate
) {
}
