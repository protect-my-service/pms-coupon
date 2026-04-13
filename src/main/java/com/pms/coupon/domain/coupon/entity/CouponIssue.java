package com.pms.coupon.domain.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "coupon_issues",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_coupon_issue_coupon_member", columnNames = {"coupon_id", "memberId"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedDate;

    private CouponIssue(Long couponId, Long memberId, LocalDateTime issuedDate) {
        this.couponId = couponId;
        this.memberId = memberId;
        this.issuedDate = issuedDate;
    }

    public static CouponIssue create(Long couponId, Long memberId, LocalDateTime issuedDate) {
        return new CouponIssue(couponId, memberId, issuedDate);
    }

    @PrePersist
    protected void onCreate() {
        if (issuedDate == null) {
            this.issuedDate = LocalDateTime.now();
        }
    }
}
