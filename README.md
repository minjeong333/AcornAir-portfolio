# ✈️ AcornAir — 항공권 예약 웹 서비스

> Acorn Academy 팀 프로젝트(2026.04.28 ~ 05.14)를 기반으로 한 개인 포트폴리오 저장소입니다.
> 팀 제출 원본 저장소: `((https://github.com/minjeong333/AcornAIr.git)`

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)
![JSP](https://img.shields.io/badge/JSP-339933?style=flat)
![Servlet](https://img.shields.io/badge/Servlet-007396?style=flat)
![Oracle](https://img.shields.io/badge/Oracle-F80000?style=flat&logo=oracle&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black)

---

## 프로젝트 개요

대한항공 웹사이트를 레퍼런스로, 항공편 검색 → 좌석 선택 → 결제 → 예약 조회로 이어지는
항공 예약 시스템을 Java/JSP/Servlet + Oracle DB로 구현한 5인 팀 프로젝트입니다.

- **기간**: 2026.04.28 ~ 2026.05.14 (개발 9일 + 발표)
- **인원**: 5인 (장윤성, 고지연, 여도현, 김민정, 김민경)
- **아키텍처**: MVC 패턴 (Controller - Service - DAO)

## 담당 역할 (김민정)

예약 흐름의 후반부 — **승객정보 입력 → 좌석 배정 → 추가 수하물 → 결제** — 전체를 담당했습니다.
그 외에 git 협업 관리(브랜치/PR 워크플로우, 79건 커밋)와 기획명세서 작성도 함께 진행했습니다.

| 영역 | 파일 |
|---|---|
| 승객정보 입력 | `PassengerServlet`, `passenger_info.jsp/js/css` |
| 좌석 배정 | `SeatServlet`, `seatSelect.jsp/css` |
| 추가 수하물 | `BaggageServlet`, `baggage.jsp` |
| 결제 | `PaymentServlet`, `PaymentService`, `payment.jsp` |
| 예약 내역 조회 (일부) | `ReservationDAO` (LISTAGG/GROUP BY 수정) |

## 기술 스택

- **Backend**: Java, Servlet, JSP (MVC 패턴)
- **Database**: Oracle (TB_USER, TB_FLIGHT, TB_AIRPORT, TB_BOOKING, TB_PASSENGER, TB_SEAT, TB_BAGGAGE, CHATBOT_DATA — 8개 테이블)
- **Frontend**: HTML, CSS, JavaScript (모달/iframe 단계 전환 + `postMessage` 통신)
- **협업**: Git (feature/fix 브랜치 전략, PR 리뷰/머지)

## 핵심 기능

- **다중 승객 처리**: 승객 수(`passCnt`)에 따라 입력 폼을 동적으로 생성
- **좌석 배정**: 예약된 좌석 비활성화, 가는편/오는편 좌석 등급 분리 적용
- **세션 기반 예약 흐름**: 승객정보~수하물까지 세션에 누적 후, 결제 시점에 트랜잭션으로 일괄 처리
- **왕복 항공권**: `GO_FLIGHT_ID`/`BACK_FLIGHT_ID`로 가는편·오는편을 분리 관리

> 화면 캡처는 추후 추가 예정입니다.

## ERD 핵심 관계

```
TB_USER 1 ── N TB_BOOKING        (회원별 예약)
TB_FLIGHT 1 ── N TB_BOOKING      (GO_FLIGHT_ID / BACK_FLIGHT_ID — 가는편·오는편 각각 FK, 왕복 처리)
TB_BOOKING 1 ── N TB_PASSENGER   (한 예약에 여러 승객)
TB_BOOKING 1 ── N TB_SEAT        (예약별 좌석)
TB_BOOKING 1 ── N TB_BAGGAGE     (예약별 추가 수하물)
```

## 더 알아보기

- 🔧 [트러블슈팅 — JOIN으로 인한 행 증가와 LISTAGG 중복 문제](docs/TROUBLESHOOTING.md)
- 🚀 [발표 이후 개인 개선 작업](docs/IMPROVEMENTS.md)
- 💭 [회고](docs/RETROSPECTIVE.md)
