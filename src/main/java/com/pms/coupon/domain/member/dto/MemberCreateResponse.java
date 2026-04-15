package com.pms.coupon.domain.member.dto;

import java.time.LocalDateTime;

public record MemberCreateResponse(
    Long memberId,
    String name,
    LocalDateTime createdAt
) {
}
