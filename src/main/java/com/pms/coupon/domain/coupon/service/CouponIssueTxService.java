package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.entity.CouponIssue;
import com.pms.coupon.domain.coupon.repository.CouponCustomRepository;
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
    private final CouponIssueRepository couponIssueRepository;

    /**
     * 쿠폰 발급 (DB 정합성 보장)
     * - 비관적 락을 통해 동시성 제어
     */
    @Transactional
    public CouponIssue issueWithLock(Long couponId, Long memberId, LocalDateTime now) {
        // 1. 쿠폰 row-level 비관적 락 획득
        Coupon lockedCoupon = couponCustomRepository.findByIdWithPessimisticLock(couponId)
            .orElseThrow(() -> new BusinessCustomException(ResponseCode.COUPON_NOT_FOUND));

        // 2. 발급 가능 기간 검증 (Redis에서 1차 검증했지만 DB에서 재검증)
        if (now.isBefore(lockedCoupon.getIssueStartDate()) || now.isAfter(lockedCoupon.getIssueEndDate())) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }

        // 3. 재고 확인
        if (lockedCoupon.getIssuedQuantity() >= lockedCoupon.getTotalQuantity()) {
            throw new BusinessCustomException(ResponseCode.COUPON_SOLD_OUT);
        }

        // 4. 발급 수량 증가
        lockedCoupon.increaseIssuedQuantity();


        try {
            // 5. 쿠폰 발급 이력 저장
            return couponIssueRepository.save(CouponIssue.create(couponId, memberId, now));
        } catch (DataIntegrityViolationException e) {
            // 6. DB Unique 제약 위반 시 (동시성 중복 요청)
            throw new BusinessCustomException(ResponseCode.COUPON_ALREADY_ISSUED);
        }
    }
}
