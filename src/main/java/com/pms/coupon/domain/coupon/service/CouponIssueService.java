package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.coupon.dto.CouponIssueResponse;
import com.pms.coupon.domain.coupon.dto.CouponIssueStatus;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.entity.CouponIssue;
import com.pms.coupon.domain.coupon.repository.CouponRepository;
import com.pms.coupon.domain.coupon.redis.CouponIssueRedisService.MemberRequestAcquireResult;
import com.pms.coupon.domain.coupon.redis.CouponIssueRedisService;
import com.pms.coupon.domain.member.repository.MemberRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueTxService couponIssueTxService;
    private final MemberRepository memberRepository;

    public CouponIssueResponse issue(Long couponId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        if (!memberRepository.existsById(memberId)) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NOT_FOUND);
        }

        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new BusinessCustomException(ResponseCode.COUPON_NOT_FOUND));

        if (now.isBefore(coupon.getIssueStartDate()) || now.isAfter(coupon.getIssueEndDate())) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }

        Duration couponTtl = calculateCouponTtl(now, coupon.getIssueEndDate());
        ensureCouponKeysInitialized(couponId, coupon.getTotalQuantity(), couponTtl);

        MemberRequestAcquireResult acquireResult = couponIssueRedisService.acquireMemberRequest(couponId, memberId, couponTtl);
        if (acquireResult == MemberRequestAcquireResult.ALREADY_DONE) {
            throw new BusinessCustomException(ResponseCode.COUPON_ALREADY_ISSUED);
        }
        if (acquireResult == MemberRequestAcquireResult.IN_PROGRESS) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_REQUEST_IN_PROGRESS);
        }

        boolean requestCountIncreased = false;

        try {
            long limit = couponIssueRedisService.getLimit(couponId);
            long requestCount = couponIssueRedisService.increaseRequestCount(couponId);

            if (requestCount > limit) {
                couponIssueRedisService.decreaseRequestCount(couponId);
                throw new BusinessCustomException(ResponseCode.COUPON_SOLD_OUT);
            }

            requestCountIncreased = true;

            CouponIssue couponIssue = couponIssueTxService.issueWithLock(couponId, memberId, now);
            couponIssueRedisService.markMemberRequestDone(couponId, memberId, couponTtl);

            return CouponIssueResponse.builder()
                .couponId(couponIssue.getCouponId())
                .memberId(couponIssue.getMemberId())
                .status(CouponIssueStatus.SUCCESS)
                .issuedDate(couponIssue.getIssuedDate())
                .build();
        } catch (RuntimeException e) {
            if (requestCountIncreased) {
                couponIssueRedisService.decreaseRequestCount(couponId);
            }

            couponIssueRedisService.deleteMemberRequest(couponId, memberId);

            throw e;
        }
    }

    private Duration calculateCouponTtl(LocalDateTime now, LocalDateTime issueEndDate) {
        Duration ttl = Duration.between(now, issueEndDate.plusSeconds(1));
        if (ttl.isNegative() || ttl.isZero()) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }
        return ttl;
    }

    private void ensureCouponKeysInitialized(Long couponId, int totalQuantity, Duration ttl) {
        if (!couponIssueRedisService.hasCouponKeys(couponId)) {
            couponIssueRedisService.initializeCouponKeys(couponId, totalQuantity, ttl);
        }
    }
}
