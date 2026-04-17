package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.utils.DateUtils;
import com.pms.coupon.domain.coupon.dto.CouponCreateRequest;
import com.pms.coupon.domain.coupon.dto.CouponCreateResponse;
import com.pms.coupon.domain.coupon.dto.CouponListResponse;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.event.CouponCreatedEvent;
import com.pms.coupon.domain.coupon.repository.CouponRepository;
import com.pms.coupon.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 쿠폰 생성
     * - 생성 후 Redis 초기화를 위한 이벤트 발행
     */
    @Transactional
    public CouponCreateResponse create(Long memberId, CouponCreateRequest request) {

        // 1. 회원 존재 여부 검증
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NOT_FOUND);
        }

        // 2. 쿠폰 이름 및 수량 추출
        String couponName = request.name().trim();
        int totalQuantity = request.totalQuantity();

        // 3. 발급 기간 시작/종료 시간 보정 (00:00:00 ~ 23:59:59)
        LocalDateTime issueStartDate = DateUtils.toStartOfDay(request.issueStartDate());
        LocalDateTime issueEndDate = DateUtils.toEndOfDay(request.issueEndDate());

        // 4. 발급 기간 유효성 검증
        if (!DateUtils.isValidDateRange(issueStartDate, issueEndDate)) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID_ON_CREATE);
        }

        // 5. 쿠폰 생성 및 저장
        Coupon newCoupon = Coupon.create(couponName, totalQuantity, issueStartDate, issueEndDate);
        Coupon coupon = couponRepository.save(newCoupon);

        // 6. 쿠폰 생성 이벤트 발행
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

    @Transactional(readOnly = true)
    public Page<CouponListResponse> getCoupons(int page, int size) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            )
        );
        Page<Coupon> resultPage = couponRepository.findAll(pageable);

        return resultPage.map(coupon -> CouponListResponse.builder()
            .couponId(coupon.getId())
            .name(coupon.getName())
            .totalQuantity(coupon.getTotalQuantity())
            .issuedQuantity(coupon.getIssuedQuantity())
            .issueStartDate(coupon.getIssueStartDate())
            .issueEndDate(coupon.getIssueEndDate())
            .createdAt(coupon.getCreatedAt())
            .build());
    }
}
