package com.pms.coupon.domain.coupon.repository;

import static com.pms.coupon.domain.coupon.entity.QCoupon.coupon;

import com.pms.coupon.domain.coupon.entity.Coupon;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponCustomImpl implements CouponCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long couponId) {
        Coupon result = queryFactory
            .selectFrom(coupon)
            .where(coupon.id.eq(couponId))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetchOne();

        return Optional.ofNullable(result);
    }
}
