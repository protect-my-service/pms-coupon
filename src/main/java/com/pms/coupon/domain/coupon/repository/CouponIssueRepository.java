package com.pms.coupon.domain.coupon.repository;

import com.pms.coupon.domain.coupon.entity.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {
}
