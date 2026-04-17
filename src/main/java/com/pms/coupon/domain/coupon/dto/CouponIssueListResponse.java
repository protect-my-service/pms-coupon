package com.pms.coupon.domain.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponIssueListResponse(

    Long couponIssueId,

    Long couponId,

    Long memberId,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime issuedDate
) {
}
