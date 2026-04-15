package com.pms.coupon.domain.coupon.repository;

import static com.pms.coupon.domain.coupon.entity.QCouponIssue.couponIssue;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponIssueCustomImpl implements CouponIssueCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByCouponIdAndMemberId(Long couponId, Long memberId) {
        Integer result = queryFactory
            .selectOne()
            .from(couponIssue)
            .where(
                couponIssue.couponId.eq(couponId),
                couponIssue.memberId.eq(memberId)
            )
            .fetchFirst();

        return result != null;
    }
}
