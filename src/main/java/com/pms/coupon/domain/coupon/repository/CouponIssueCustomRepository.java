package com.pms.coupon.domain.coupon.repository;

public interface CouponIssueCustomRepository {

    boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);
}
