# 트러블슈팅 — JOIN으로 인한 행 증가와 LISTAGG 중복 문제

## 어디서 발생했나
예약 내역 조회(`ReservationDAO.selectReservationList`)에서, 한 예약(`TB_BOOKING`)에 대해
승객(`TB_PASSENGER`)과 좌석(`TB_SEAT`) 정보를 함께 보여주기 위해 LEFT JOIN을 사용했습니다.

```sql
FROM TB_BOOKING B
JOIN TB_USER U ON B.USER_ID = U.USER_ID
JOIN TB_FLIGHT GF ON B.GO_FLIGHT_ID = GF.FLIGHT_ID
LEFT JOIN TB_FLIGHT BF ON B.BACK_FLIGHT_ID = BF.FLIGHT_ID
LEFT JOIN TB_SEAT S ON B.BOOKING_ID = S.BOOKING_ID
LEFT JOIN TB_BAGGAGE BG ON B.BOOKING_ID = BG.BOOKING_ID
LEFT JOIN TB_PASSENGER P ON B.BOOKING_ID = P.BOOKING_ID
```

`TB_BOOKING : TB_PASSENGER`, `TB_BOOKING : TB_SEAT`는 모두 1:N 관계입니다.
한 예약에 승객이 2명, 좌석이 2개라면 이 JOIN의 결과는 한 예약당 최대 4행(2×2)까지
늘어날 수 있습니다.

## 문제: MAX()로 좌석번호를 가져오던 기존 방식

```sql
-- 수정 전
MAX(CASE WHEN S.FLIGHT_ID = GF.FLIGHT_ID THEN S.SEAT_NO END) AS GO_SEAT_NO,
MAX(CASE WHEN S.FLIGHT_ID = BF.FLIGHT_ID THEN S.SEAT_NO END) AS BACK_SEAT_NO,
```

승객 1명 + 좌석 1개인 예약에서는 문제없이 동작했습니다. 그런데 승객이 2명 이상이라
좌석이 여러 개 배정된 예약에서는, JOIN으로 늘어난 여러 행 중 `MAX()`가 값을 단 하나만
반환하기 때문에 좌석번호 하나만 표시되고 나머지 좌석 정보가 사라지는 문제가 있었습니다.
승객 이름도 같은 구조(`MAX` 또는 단순 컬럼 참조)였기 때문에 다중 승객 예약에서
이름이 정상적으로 표시되지 않았습니다.

## 해결: DISTINCT 서브쿼리 + LISTAGG

조인으로 늘어난 행을 그대로 집계하는 대신, 좌석/승객 정보는 `DISTINCT` 서브쿼리로
별도 조회한 뒤 `LISTAGG`로 합치는 방식으로 변경했습니다.

```sql
-- 수정 후
COUNT(DISTINCT P.PASSENGER_ID) AS PASSENGER_COUNT,

-- TB_SEAT JOIN으로 행이 증가하면 LISTAGG에 중복이 생기므로 DISTINCT 서브쿼리로 처리
(SELECT LISTAGG(NAME, ', ') WITHIN GROUP (ORDER BY NAME)
 FROM (SELECT DISTINCT ENG_LAST_NAME || ' ' || ENG_FIRST_NAME AS NAME
       FROM TB_PASSENGER WHERE BOOKING_ID = B.BOOKING_ID)) AS PASSENGER_NAMES,

(SELECT LISTAGG(SEAT_NO, ', ') WITHIN GROUP (ORDER BY SEAT_NO)
 FROM (SELECT DISTINCT SEAT_NO FROM TB_SEAT
       WHERE BOOKING_ID = B.BOOKING_ID AND FLIGHT_ID = GF.FLIGHT_ID)) AS GO_SEAT_NO,

(SELECT LISTAGG(SEAT_NO, ', ') WITHIN GROUP (ORDER BY SEAT_NO)
 FROM (SELECT DISTINCT SEAT_NO FROM TB_SEAT
       WHERE BOOKING_ID = B.BOOKING_ID AND FLIGHT_ID = BF.FLIGHT_ID)) AS BACK_SEAT_NO,
```

추가로, 메인 쿼리의 `GROUP BY`에 `GF.FLIGHT_ID`, `BF.FLIGHT_ID`를 포함시켜
같은 항공편 정보가 서로 다른 그룹으로 쪼개지지 않도록 정리했습니다.

## 배운 점

1:N 조인이 여러 개 겹치는 쿼리에서는 단순 집계 함수(`MAX`, `SUM` 등)가 행 증가의
영향을 직접 받기 쉽습니다. "조인 결과를 그대로 집계"하는 대신, 필요한 하위 데이터는
서브쿼리로 분리해 먼저 집계한 뒤 메인 쿼리에 합치는 패턴이 더 안전하다는 것을
직접 겪으며 익혔습니다.
