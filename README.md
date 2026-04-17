# PMS Coupon

스터디 부하 테스트 및 성능 개선 실습용 선착순 쿠폰 시스템

## 목차

- [1. 기술 스택](#1-기술-스택)
- [2. 실행 방법](#2-실행-방법)
- [3. 핵심 도메인 및 API](#3-핵심-도메인-및-api)
- [4. 동시성 제어 요약](#4-동시성-제어-요약)
- [5. 테스트](#5-테스트)
- [6. 한계점 및 보강 포인트](#6-한계점-및-보강-포인트)

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
| Migration | Flyway |
| Test Infra | JUnit5 + Testcontainers (PostgreSQL, Redis) |
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

## 5. 테스트

### 5.1 실행

```bash
./gradlew test
```

### 5.2 포함된 검증 시나리오

- 동시 발급 요청 시 `issuedQuantity`가 `totalQuantity`를 초과하지 않는지 검증
- 동일 회원 동시 요청 시 DB 발급 이력이 1건만 저장되는지 검증
- 동일 회원 재요청 시 기존 발급 이력을 기반으로 멱등 성공 응답을 반환하는지 검증
- 쿠폰 생성 커밋 이후 Redis 키 초기화(`AFTER_COMMIT`) 동작 검증
- Redis 키 유실 상황에서 fallback 초기화 후 발급 가능한지 검증

### 5.3 테스트 인프라

- 통합 테스트는 Testcontainers로 PostgreSQL/Redis를 테스트 런타임에 동적으로 기동합니다.
- 테스트 DB 스키마/시드 데이터는 Flyway 마이그레이션으로 관리합니다.
- 테스트 환경 JPA 설정은 `ddl-auto=validate` 기준으로 엔티티-스키마 정합성을 검증합니다.

<br>

## 6. 한계점 및 보강 포인트

| 구분 | 현재 한계점 | 보강 방향 |
|---|---|---|
| DB 확장성 | 단일 쿠폰 row 비관적 락 경합으로 고트래픽 시 처리량 한계 가능 | 쿠폰 단위 샤딩, 도메인 분리, 비동기 발급 파이프라인 도입 검토 |
| Redis 장애 대응 | Redis 키 유실/장애 시 카운트 불일치 가능성 | DB 기준 재동기화 배치/관리 API 제공 |
| 멱등성 범위 | 현재는 `memberId + couponId` 중심의 멱등 처리 | `Idempotency-Key` 기반 재시도 정책으로 확장 |
| 관측 가능성 | 병목 지표를 체계적으로 수집/시각화하지 않음 | Actuator/Micrometer/Prometheus 대시보드 구성(TPS, lock wait, DB pool, Redis latency) |
| 발급 파이프라인 | 동기 처리 중심이라 장애 전파 범위가 큼 | RabbitMQ 기반 비동기 처리 + 재처리/보상 전략 설계 |
