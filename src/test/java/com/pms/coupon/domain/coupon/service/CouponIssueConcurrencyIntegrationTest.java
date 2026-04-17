package com.pms.coupon.domain.coupon.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.coupon.dto.CouponCreateRequest;
import com.pms.coupon.domain.coupon.entity.Coupon;
import com.pms.coupon.domain.coupon.redis.CouponIssueRedisService;
import com.pms.coupon.domain.coupon.repository.CouponIssueRepository;
import com.pms.coupon.domain.coupon.repository.CouponRepository;
import com.pms.coupon.domain.member.entity.Member;
import com.pms.coupon.domain.member.repository.MemberRepository;
import com.pms.coupon.support.IntegrationTestContainers;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueConcurrencyIntegrationTest extends IntegrationTestContainers {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponIssueService couponIssueService;

    @Autowired
    private CouponIssueRedisService couponIssueRedisService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void tearDown() {
        couponIssueRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();

        var keys = stringRedisTemplate.keys("coupon:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("재고보다 많은 동시 발급 요청 시 성공 건수는 재고 수량을 초과하지 않는다")
    void concurrentIssue_neverExceedsTotalQuantity() throws Exception {
        int totalQuantity = 10;
        int requestCount = 50;

        Coupon coupon = couponRepository.save(Coupon.create(
            "concurrency-coupon",
            totalQuantity,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusMinutes(10)
        ));

        List<Long> memberIds = IntStream.range(0, requestCount)
            .mapToObj(i -> memberRepository.save(Member.create("member-" + i)).getId())
            .toList();

        ConcurrencyResult result = runConcurrentIssues(coupon.getId(), memberIds);

        Coupon reloaded = couponRepository.findById(coupon.getId()).orElseThrow();
        long issueRows = couponIssueRepository.findAll().stream()
            .filter(issue -> Objects.equals(issue.getCouponId(), coupon.getId()))
            .count();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.successCount()).isEqualTo(totalQuantity);
        softly.assertThat(result.soldOutCount()).isEqualTo(requestCount - totalQuantity);
        softly.assertThat(result.unexpectedErrors()).isEmpty();
        softly.assertThat(reloaded.getIssuedQuantity()).isEqualTo(totalQuantity);
        softly.assertThat(issueRows).isEqualTo(totalQuantity);
        softly.assertAll();
    }

    @Test
    @DisplayName("동일 회원 동시 요청 시 DB 발급 이력은 1건만 남고 나머지는 진행중 또는 멱등 성공 응답을 받는다")
    void sameMemberConcurrentRequest_onlyOneSuccess() throws Exception {
        Coupon coupon = couponRepository.save(Coupon.create(
            "same-member-coupon",
            5,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusMinutes(10)
        ));
        Long memberId = memberRepository.save(Member.create("same-member")).getId();

        List<Long> sameMembers = List.of(memberId, memberId, memberId, memberId, memberId);
        ConcurrencyResult result = runConcurrentIssues(coupon.getId(), sameMembers);

        long issueRows = couponIssueRepository.findAll().stream()
            .filter(issue -> Objects.equals(issue.getCouponId(), coupon.getId()))
            .count();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.successCount()).isGreaterThanOrEqualTo(1);
        softly.assertThat(result.successCount() + result.alreadyIssuedCount() + result.inProgressCount())
            .isEqualTo(sameMembers.size());
        softly.assertThat(result.unexpectedErrors()).isEmpty();
        softly.assertThat(issueRows).isEqualTo(1);
        softly.assertAll();
    }

    @Test
    @DisplayName("쿠폰 생성 후 AFTER_COMMIT에서 Redis 키가 초기화된다")
    void couponCreate_initializesRedisKeysAfterCommit() {
        Long creatorId = memberRepository.save(Member.create("creator")).getId();
        var request = new CouponCreateRequest(
            "created-coupon",
            20,
            LocalDate.now(),
            LocalDate.now().plusDays(1)
        );

        var response = couponService.create(creatorId, request);
        Long couponId = response.couponId();

        assertTrue(waitUntil(() -> couponIssueRedisService.hasCouponKeys(couponId), 20, 100));
    }

    @Test
    @DisplayName("발급 시 Redis 키가 유실되어도 fallback 초기화 후 발급을 진행한다")
    void issue_fallbackInitializesRedisKeysWhenMissing() {
        Coupon coupon = couponRepository.save(Coupon.create(
            "fallback-coupon",
            3,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusMinutes(10)
        ));
        Long memberId = memberRepository.save(Member.create("fallback-member")).getId();

        couponIssueService.issue(coupon.getId(), memberId);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(couponIssueRedisService.hasCouponKeys(coupon.getId())).isTrue();
        softly.assertThat(couponIssueRepository.findAll().stream()
            .filter(issue -> Objects.equals(issue.getCouponId(), coupon.getId()))
            .count()).isEqualTo(1);
        softly.assertAll();
    }

    private ConcurrencyResult runConcurrentIssues(Long couponId, List<Long> memberIds) throws Exception {
        int threadCount = memberIds.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger soldOutCount = new AtomicInteger();
        AtomicInteger alreadyIssuedCount = new AtomicInteger();
        AtomicInteger inProgressCount = new AtomicInteger();
        Queue<String> unexpectedErrors = new ConcurrentLinkedQueue<>();

        try {
            for (Long memberId : memberIds) {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        couponIssueService.issue(couponId, memberId);
                        successCount.incrementAndGet();
                    } catch (BusinessCustomException e) {
                        if (e.getResponseCode() == ResponseCode.COUPON_SOLD_OUT) {
                            soldOutCount.incrementAndGet();
                        } else if (e.getResponseCode() == ResponseCode.COUPON_ALREADY_ISSUED) {
                            alreadyIssuedCount.incrementAndGet();
                        } else if (e.getResponseCode() == ResponseCode.COUPON_ISSUE_REQUEST_IN_PROGRESS) {
                            inProgressCount.incrementAndGet();
                        } else {
                            unexpectedErrors.add("Business:" + e.getResponseCode().name());
                        }
                    } catch (Throwable t) {
                        unexpectedErrors.add(t.getClass().getSimpleName() + ":" + t.getMessage());
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "thread ready timeout");
            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "thread execution timeout");
        } finally {
            executorService.shutdownNow();
        }

        return new ConcurrencyResult(
            successCount.get(),
            soldOutCount.get(),
            alreadyIssuedCount.get(),
            inProgressCount.get(),
            new ArrayList<>(unexpectedErrors)
        );
    }

    private boolean waitUntil(Check check, int maxRetry, long sleepMillis) {
        for (int i = 0; i < maxRetry; i++) {
            if (check.test()) {
                return true;
            }
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface Check {
        boolean test();
    }

    private record ConcurrencyResult(
        int successCount,
        int soldOutCount,
        int alreadyIssuedCount,
        int inProgressCount,
        List<String> unexpectedErrors
    ) {
    }
}
