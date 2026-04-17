# PMS Coupon

스터디 부하 테스트 및 성능 개선 실습용 선착순 쿠폰 시스템

## 목차

- [1. 기술 스택](#1-기술-스택)
- [2. 실행 방법](#2-실행-방법)
- [3. 핵심 도메인 및 API](#3-핵심-도메인-및-api)
- [4. 동시성 제어 요약](#4-동시성-제어-요약)
- [5. 향후 확장 방향](#5-향후-확장-방향)
- [6. 한계점 및 고려사항](#6-한계점-및-고려사항)

<br>

## 1. 기술 스택

| 항목 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0 |
| ORM | Spring Data JPA |
| Query Builder | QueryDSL |
| Database | PostgreSQL 15 |
| Cache / In-memory Store | Redis 7 |
| Monitoring | Spring Actuator + Micrometer + Prometheus |
| Build Tool | Gradle |
| Container | Docker Compose |

<br>

## 2. 실행 방법

### 2.1 인프라 실행 (PostgreSQL, Redis)

```bash
docker compose up -d
```

### 2.2 API 서버 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew bootJar
java -jar build/libs/*.jar
```

<br>

## 3. 핵심 도메인 및 API

### 3.1 핵심 엔티티

- `Coupon`
  - 총 발급 수량(`totalQuantity`)
  - 현재 발급 수량(`issuedQuantity`)
  - 발급 기간(`issueStartDate`, `issueEndDate`)
- `CouponIssue`
  - 쿠폰 발급 이력
  - 유니크 제약: `(coupon_id, member_id)`
- `Member`
  - 발급 요청 주체

### 3.2 API 엔드포인트

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/members` | 회원 등록 |
| `POST` | `/coupons` | 쿠폰 생성 |
| `POST` | `/coupons/{id}/issue` | 쿠폰 발급 |

요청 헤더: `X-Member-Id` (쿠폰 생성/발급 API 공통)

응답은 공통 응답 래퍼(`CommonResponse`)를 통해 내려갑니다.

<br>

## 4. 동시성 제어 요약

### 4.1 처리 방식

- Redis로 1차 선검증 후 DB로 최종 확정하는 구조입니다.
- Redis
  - `coupon:requested-count:{couponId}`: 발급 요청(선점) 수량
  - `coupon:issued-count:{couponId}`: 실제 발급 확정 수량
  - `coupon:req:{couponId}:{memberId}`: 유저별 요청 상태
- DB
  - 쿠폰 row `PESSIMISTIC_WRITE` 락
  - `(coupon_id, member_id)` 유니크 제약으로 중복 발급 방지

### 4.2 핵심 포인트

- `requested-count`를 먼저 `INCR`하여 동시 요청 경합을 빠르게 차단
- `requested-count > coupon.totalQuantity`이면 즉시 `DECR` 보상 후 실패 처리
- DB 확정 성공 건에 대해서만 `issued-count`를 증가시켜 실제 발급 수량을 관리
- 최종 정합성은 PostgreSQL 트랜잭션에서 보장
- 실패 시 Redis 카운트 보상 및 요청 키 정리
- Redis 키 초기화는 쿠폰 생성 커밋 이후(`AFTER_COMMIT`) 처리

<br>

## 5. 향후 확장 방향

### TODO

- [x] 동시성/보상 시나리오 통합 테스트 코드 추가
- [x] 테스트 데이터 추가(로컬/검증용 시드 데이터)
- [ ] RabbitMQ 기반 발급 파이프라인 추가
  - 메시지 발행 -> 메시지 수신 -> 발급 수량 유효성 검증 -> 발급 데이터 저장
- [ ] 모니터링 구성
- [ ] 멱등성 키 설계 (후속 보강 예정)

<br>

## 6. 한계점 및 고려사항

- 현재 구조는 단일 DB 기준으로 정합성을 우선하므로, 트래픽이 매우 커지면 락 경합으로 처리량 한계가 발생할 수 있습니다.
- 대규모 트래픽 환경에서는 DB 샤딩(예: couponId/hash 기반) 또는 발급 도메인 분리가 필요할 수 있습니다.
- Redis는 메모리 기반 저장소이므로 장애/유실 상황을 고려한 재동기화 전략(DB 기준 복구)이 필수입니다.
- 발급 경로의 병목 지점을 지속적으로 확인하려면 DB 커넥션 사용률, lock wait, Redis latency 모니터링이 필요합니다.
- 메시지 큐 도입 시 중복 소비/순서 보장/재처리(DLQ) 정책을 함께 설계해야 합니다.
- 멱등성 키와 재시도 정책이 없으면 네트워크 타임아웃 상황에서 클라이언트 재요청 처리 일관성이 깨질 수 있습니다.
