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
        @UniqueConstraint(name = "uk_coupon_issue_coupon_member", columnNames = {"coupon_id", "member_id"})
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CouponIssue(Long couponId, Long memberId, LocalDateTime createdAt) {
        this.couponId = couponId;
        this.memberId = memberId;
        this.createdAt = createdAt;
    }

    public static CouponIssue create(Long couponId, Long memberId, LocalDateTime createdAt) {
        return new CouponIssue(couponId, memberId, createdAt);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getIssuedDate() {
        return this.createdAt;
    }
}
