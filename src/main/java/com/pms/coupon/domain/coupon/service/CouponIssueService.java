package com.pms.coupon.domain.coupon.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.enums.MemberRequestAcquireResult;
import com.pms.coupon.domain.coupon.dto.CouponIssueResponse;
import com.pms.coupon.common.enums.CouponIssueStatus;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.entity.CouponIssue;
import com.pms.coupon.domain.coupon.redis.CouponIssueRedisService;
import com.pms.coupon.domain.coupon.repository.CouponIssueRepository;
import com.pms.coupon.domain.coupon.repository.CouponRepository;
import com.pms.coupon.domain.member.repository.MemberRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueTxService couponIssueTxService;
    private final MemberRepository memberRepository;

    /**
     * 쿠폰 발급
     * @param couponId
     * @param memberId
     * @return
     */
    public CouponIssueResponse issue(Long couponId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 회원 존재 여부 검증
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NOT_FOUND);
        }

        // 2. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new BusinessCustomException(ResponseCode.COUPON_NOT_FOUND));

        // 3. 쿠폰 발급 가능 기간 검증
        if (now.isBefore(coupon.getIssueStartDate()) || now.isAfter(coupon.getIssueEndDate())) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }

        // 4. TTL 계산 (쿠폰 발급 종료 시점까지 유지)
        Duration ttl = calculateCouponTtl(now, coupon.getIssueEndDate());

        // 5. Redis 키 보정 초기화 (멱등)
        ensureCouponKeysInitialized(couponId, coupon.getIssuedQuantity(), ttl);

        // 6. 사용자 요청 상태 선점 (중복 요청 방지)
        MemberRequestAcquireResult acquireResult =
            couponIssueRedisService.acquireMemberRequest(couponId, memberId, ttl);

        if (acquireResult == MemberRequestAcquireResult.ALREADY_DONE) {
            return getExistingIssueResponse(couponId, memberId);
        }

        if (acquireResult == MemberRequestAcquireResult.IN_PROGRESS) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_REQUEST_IN_PROGRESS);
        }

        boolean requestedCountIncreased = false;
        boolean issuePersisted = false;

        try {
            // 7. 전체 요청 수 증가 (선착순 제어)
            long requestedCount = couponIssueRedisService.increaseRequestedCount(couponId);

            // 8. 재고 초과 시 롤백 후 실패 처리
            if (requestedCount > coupon.getTotalQuantity()) {
                couponIssueRedisService.decreaseRequestedCount(couponId);
                throw new BusinessCustomException(ResponseCode.COUPON_SOLD_OUT);
            }

            requestedCountIncreased = true;

            // 9. DB 발급 처리 (비관적 락 기반)
            CouponIssue couponIssue =
                couponIssueTxService.issueWithLock(couponId, memberId, now);

            // rollback 기준점 플래그
            issuePersisted = true;

            // 10. 성공 시 실제 발급 수량 증가 + 사용자 상태 DONE 처리
            couponIssueRedisService.increaseIssuedCount(couponId);
            couponIssueRedisService.markMemberRequestDone(couponId, memberId, ttl);

            // 11. 응답 반환
            return CouponIssueResponse.builder()
                .couponId(couponIssue.getCouponId())
                .memberId(couponIssue.getMemberId())
                .status(CouponIssueStatus.SUCCESS)
                .issuedDate(couponIssue.getIssuedDate())
                .build();

        } catch (RuntimeException e) {
            // 12. 요청 카운트 증가된 경우 롤백
            if (requestedCountIncreased && !issuePersisted) {
                couponIssueRedisService.decreaseRequestedCount(couponId);
            }

            // 13. 예외 종류에 따라 Redis 상태 정리
            if (isAlreadyIssuedException(e)) {
                // 이미 발급된 경우 → DONE 상태 유지 (멱등성 보장)
                couponIssueRedisService.markMemberRequestDone(couponId, memberId, ttl);
                return getExistingIssueResponse(couponId, memberId);
            } else {
                // 그 외 실패 → 요청 상태 제거 (재시도 가능)
                couponIssueRedisService.deleteMemberRequest(couponId, memberId);
            }

            throw e;
        }
    }

    /**
     * 쿠폰 TTL 계산
     * - 발급 종료 시점 + 1초까지 유지
     */
    private Duration calculateCouponTtl(LocalDateTime now, LocalDateTime issueEndDate) {
        Duration ttl = Duration.between(now, issueEndDate.plusSeconds(1));

        if (ttl.isNegative() || ttl.isZero()) {
            throw new BusinessCustomException(ResponseCode.COUPON_ISSUE_PERIOD_INVALID);
        }

        return ttl;
    }

    /**
     * Redis 키 초기화 (최초 1회만 수행)
     */
    private void ensureCouponKeysInitialized(Long couponId, int issuedQuantity, Duration ttl) {
        if (!couponIssueRedisService.hasCouponKeys(couponId)) {
            couponIssueRedisService.initializeCouponKeys(couponId, issuedQuantity, ttl);
        }
    }

    /**
     * 이미 발급된 예외인지 판별
     */
    private boolean isAlreadyIssuedException(RuntimeException e) {
        return e instanceof BusinessCustomException businessException
            && businessException.getResponseCode() == ResponseCode.COUPON_ALREADY_ISSUED;
    }

    private CouponIssueResponse getExistingIssueResponse(Long couponId, Long memberId) {
        CouponIssue existingIssue = couponIssueRepository.findByCouponIdAndMemberId(couponId, memberId)
            .orElseThrow(() -> new BusinessCustomException(ResponseCode.COUPON_ALREADY_ISSUED));

        return CouponIssueResponse.builder()
            .couponId(existingIssue.getCouponId())
            .memberId(existingIssue.getMemberId())
            .status(CouponIssueStatus.SUCCESS)
            .issuedDate(existingIssue.getIssuedDate())
            .build();
    }
}
