package com.pms.coupon.domain.coupon.repository;

import com.pms.coupon.domain.coupon.entity.CouponIssue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    Optional<CouponIssue> findByCouponIdAndMemberId(Long couponId, Long memberId);
}
