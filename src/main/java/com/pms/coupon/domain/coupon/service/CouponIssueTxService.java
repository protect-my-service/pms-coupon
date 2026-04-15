package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.entity.CouponIssue;
import com.pms.coupon.domain.coupon.repository.CouponCustomRepository;
import com.pms.coupon.domain.coupon.repository.CouponIssueCustomRepository;
import com.pms.coupon.domain.coupon.repository.CouponIssueRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueTxService {

    private final CouponCustomRepository couponCustomRepository;
    private final CouponIssueCustomRepository couponIssueCustomRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public CouponIssue issueWithLock(Long couponId, Long memberId, LocalDateTime now) {
        Coupon lockedCoupon = couponCustomRepository.findByIdWithPessimisticLock(couponId)
            .orElseThrow(() -> new BusinessCustomException(ResponseCode.COUPON_NOT_FOUND));

        if (couponIssueCustomRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            throw new BusinessCustomException(ResponseCode.COUPON_ALREADY_ISSUED);
        }

        if (now.isBefore(lockedCoupon.getIssueStartDate()) || now.isAfter(lockedCoupon.getIssueEndDate())) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }

        if (lockedCoupon.getIssuedQuantity() >= lockedCoupon.getTotalQuantity()) {
            throw new BusinessCustomException(ResponseCode.COUPON_SOLD_OUT);
        }

        lockedCoupon.increaseIssuedQuantity();
        try {
            return couponIssueRepository.save(CouponIssue.create(couponId, memberId, now));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessCustomException(ResponseCode.COUPON_ALREADY_ISSUED);
        }
    }
}
