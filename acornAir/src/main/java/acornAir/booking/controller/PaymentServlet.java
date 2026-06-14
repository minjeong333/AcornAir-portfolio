package acornAir.booking.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import acornAir.booking.dto.BaggageDTO;
import acornAir.booking.dto.BookingDTO;
import acornAir.booking.service.PaymentService;
import acornAir.login.dto.UserDTO;

@WebServlet("/air/booking/payment")
public class PaymentServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private PaymentService paymentService = new PaymentService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		HttpSession session = req.getSession();

		BookingDTO bookingDTO = (BookingDTO) session.getAttribute("bookingDTO");
		String mode = req.getParameter("mode");

		if ("baggage".equals(mode)) {

			req.setAttribute("basePrice", 0);
			req.setAttribute("totalPrice", 0);
			req.setAttribute("bagPrice", 40000);

			req.getRequestDispatcher("/WEB-INF/views/booking/baggage.jsp").forward(req, resp);

			return;
		}
		if (bookingDTO == null) {
			resp.sendRedirect(req.getContextPath() + "/home");
			return;
		}

		req.setAttribute("basePrice", bookingDTO.getBasePrice());
		req.setAttribute("totalPrice", bookingDTO.getTotalPrice());
		req.setAttribute("bagPrice", 40000);

		req.getRequestDispatcher("/WEB-INF/views/booking/payment.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8");

		HttpSession session = req.getSession();

		UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
		BookingDTO bookingDTO = (BookingDTO) session.getAttribute("bookingDTO");

//		if (loginUser == null) {
//			loginUser = new UserDTO();
//			loginUser.setUserId("test01");
//			loginUser.setUserPhone("01012345678");
//			loginUser.setPhoneCountry("+82");
//
//			session.setAttribute("loginUser", loginUser);
//		}
		if (loginUser == null) {
			resp.sendRedirect(req.getContextPath() + "/air/login");
			return;
		}

		if (bookingDTO == null) {
			resp.sendRedirect(req.getContextPath() + "/home");
			return;
		}

		try {
			String payMethod = req.getParameter("payMethod");

			// [개선] 결제 금액은 클라이언트가 이 시점에 다시 보낸 값으로 재계산하지 않는다.
			// 좌석 선택(SeatServlet) -> 수하물 선택(BaggageServlet) 단계를 거치며
			// 서버에서 계산한 결과가 이미 세션의 BookingDTO(basePrice/baggagePrice/totalPrice)에
			// 저장되어 있으므로, 결제 단계에서는 그 값을 그대로 사용한다.
			int bags = 0;
			Object bagsAttr = session.getAttribute("bags");
			if (bagsAttr instanceof Integer) {
				bags = (Integer) bagsAttr;
			}

			int baggagePrice = bookingDTO.getBaggagePrice();
			int totalPrice = bookingDTO.getTotalPrice();

			bookingDTO.setPayMethod(payMethod);
			// baggagePrice / totalPrice는 BaggageServlet 단계에서 이미 BookingDTO에 반영되어
			// 있으므로 여기서는 별도로 설정(덮어쓰기)하지 않는다.

			String contactPhone = (String) session.getAttribute("contactPhone");

			if (contactPhone == null || contactPhone.trim().isEmpty()) {
				contactPhone = loginUser.getUserPhone();
			}

			bookingDTO.setContactPhone(contactPhone);
			bookingDTO.setPhoneCountry(loginUser.getPhoneCountry());

			if (bags > 0) {
				List<BaggageDTO> baggages = new ArrayList<>();

				BaggageDTO goBag = new BaggageDTO();
				goBag.setFlightId(bookingDTO.getGoFlight().getFlightId());
				goBag.setExtraBaggage(bags);
				goBag.setBaggagePrice(baggagePrice);
				baggages.add(goBag);

				if (bookingDTO.getBackFlight() != null) {
					BaggageDTO backBag = new BaggageDTO();
					backBag.setFlightId(bookingDTO.getBackFlight().getFlightId());
					backBag.setExtraBaggage(bags);
					backBag.setBaggagePrice(baggagePrice);
					baggages.add(backBag);
				}

				bookingDTO.setBaggages(baggages);
			} else {
				bookingDTO.setBaggages(null);
			}

			paymentService.pay(bookingDTO);

			session.removeAttribute("bookingDTO");
			session.removeAttribute("passengers");
			session.removeAttribute("goSeats");
			session.removeAttribute("backSeats");
			session.removeAttribute("contactPhone");
			session.removeAttribute("bags");
			session.removeAttribute("total");

			resp.sendRedirect(req.getContextPath() + "/reservation/list");
			System.out.println("=== DB INSERT 완료 ===");

		} catch (Exception e) {
			e.printStackTrace();

			resp.setContentType("text/html; charset=UTF-8");

			resp.getWriter().println("<script>");
			resp.getWriter().println("alert('결제 오류: " + e.toString().replace("'", "") + "');");
			resp.getWriter().println("location.href='" + req.getContextPath() + "/air/booking/passenger';");
			resp.getWriter().println("</script>");
		}
	}
}