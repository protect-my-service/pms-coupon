package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.utils.DateUtils;
import com.pms.coupon.domain.coupon.dto.CouponCreateRequest;
import com.pms.coupon.domain.coupon.dto.CouponCreateResponse;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.event.CouponCreatedEvent;
import com.pms.coupon.domain.coupon.repository.CouponRepository;
import com.pms.coupon.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CouponCreateResponse create(Long memberId, CouponCreateRequest request) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NOT_FOUND);
        }

        String couponName = request.name().trim();
        int totalQuantity = request.totalQuantity();

        LocalDateTime issueStartDate = DateUtils.toStartOfDay(request.issueStartDate());
        LocalDateTime issueEndDate = DateUtils.toEndOfDay(request.issueEndDate());
        if (!DateUtils.isValidDateRange(issueStartDate, issueEndDate)) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID_ON_CREATE);
        }

        Coupon newCoupon = Coupon.create(couponName, totalQuantity, issueStartDate, issueEndDate);
        Coupon coupon = couponRepository.save(newCoupon);

        eventPublisher.publishEvent(new CouponCreatedEvent(
            coupon.getId(),
            coupon.getTotalQuantity(),
            coupon.getIssueEndDate()
        ));

        return CouponCreateResponse.builder()
            .couponId(coupon.getId())
            .name(coupon.getName())
            .totalQuantity(coupon.getTotalQuantity())
            .issueStartDate(coupon.getIssueStartDate())
            .issueEndDate(coupon.getIssueEndDate())
            .createdAt(coupon.getCreatedAt())
            .build();
    }
}
