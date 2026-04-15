package com.pms.coupon.domain.coupon.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponCreateResponse(
    Long couponId,
    String name,
    int totalQuantity,
    LocalDateTime issueStartDate,
    LocalDateTime issueEndDate,
    LocalDateTime createdAt
) {
}
