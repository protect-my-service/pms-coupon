package com.pms.coupon.domain.coupon.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponIssueResponse(
    Long couponId,
    Long memberId,
    CouponIssueStatus status,
    LocalDateTime issuedDate
) {
}
