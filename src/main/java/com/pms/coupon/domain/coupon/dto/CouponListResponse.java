package com.pms.coupon.domain.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponListResponse(

    Long couponId,

    String name,

    int totalQuantity,

    int issuedQuantity,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime issueStartDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime issueEndDate,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {
}
