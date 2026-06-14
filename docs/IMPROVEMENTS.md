# 발표 이후 개인 개선 — 결제 금액 클라이언트 재계산 제거

발표(2026.05.14) 이후 코드를 다시 리뷰하면서, 결제 단계에서 발견한 한계점을
개인적으로 개선한 내용입니다. (`PaymentServlet.java`)

## 무엇이 문제였나

기존 `PaymentServlet.doPost()`는 결제 제출 시점에 클라이언트가 다시 보낸
`bags`(수하물 개수) 파라미터로 `baggagePrice`, `totalPrice`를 그 자리에서
재계산했습니다.

```java
// 수정 전
int bags = 0;
String bagsParam = req.getParameter("bags");
if (bagsParam != null && !bagsParam.isEmpty()) {
    bags = Integer.parseInt(bagsParam);
}

int bagPrice = 40000;
int baggagePrice = bags * bagPrice;

int basePrice = bookingDTO.getBasePrice();
int totalPrice = basePrice + baggagePrice;

bookingDTO.setBaggagePrice(baggagePrice);
bookingDTO.setTotalPrice(totalPrice);
```

이미 이전 단계(좌석 선택 → 추가 수하물 선택, `BaggageServlet`)에서 서버가 계산한
`baggagePrice`/`totalPrice`가 `BookingDTO`에 세션 저장되어 있는데도, 결제 단계에서
한 번 더 클라이언트 입력을 신뢰해 덮어쓰는 구조였습니다. 즉 결제 제출 직전에
`bags` 값을 조작해서 보내면 최종 결제 금액이 그 값을 따라 바뀔 수 있었습니다.

## 어떻게 고쳤나

`BaggageServlet`에서 이미 계산해 `BookingDTO`에 저장한 `baggagePrice`/`totalPrice`를
결제 단계에서 그대로 사용하도록 변경했습니다. `bags`(수하물 개수, INSERT용)도
요청 파라미터 대신 세션에 저장된 값을 사용합니다.

```java
// 수정 후
int bags = 0;
Object bagsAttr = session.getAttribute("bags");
if (bagsAttr instanceof Integer) {
    bags = (Integer) bagsAttr;
}

int baggagePrice = bookingDTO.getBaggagePrice();
int totalPrice = bookingDTO.getTotalPrice();

bookingDTO.setPayMethod(payMethod);
// baggagePrice / totalPrice는 이전 단계에서 이미 BookingDTO에 반영되어 있으므로
// 여기서 다시 계산하거나 덮어쓰지 않음
```

## 효과와 남아있는 한계

결제 금액을 결정하는 입력 지점이 "결제 단계"에서 사라지고, "수하물 선택 단계"
한 곳으로 줄었습니다. 다만 `BaggageServlet`도 여전히 클라이언트가 보낸 `bags`를
그대로 신뢰해 가격을 계산하기 때문에, 근본적인 해결은 아닙니다. 수량 범위 검증
(예: 0~3개) 등은 다음 개선 과제로 남겨두었습니다.
