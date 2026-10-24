package com.payment.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.payment.app.dto.OrderPlacedEvent;
import com.payment.app.dto.PaymentFailedEvent;
import com.payment.app.dto.PaymentResponse;
import com.payment.app.entity.Payment;
import com.payment.app.mapper.PaymentMapper;
import com.payment.app.repository.PaymentRepository;

@Service
public class PaymentService {

	private final PaymentRepository payments;
	private final PaymentMapper mapper;
	private final RestClient orders = RestClient.create("http://localhost:8082");

	public PaymentService(PaymentRepository payments, PaymentMapper mapper) {
		this.payments = payments;
		this.mapper = mapper;
	}

	public PaymentResponse onOrderPlaced(OrderPlacedEvent event) {
		Payment payment = new Payment();
		payment.setOrderId(event.orderId());
		payment.setAmount(event.amount());
		if (event.failPayment()) {
			payment.setStatus("FAILED");
			payments.save(payment);
			orders.post().uri("/api/events/payment-failed").body(new PaymentFailedEvent(event.orderId())).retrieve()
					.toBodilessEntity();
			throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment failed");
		}
		payment.setStatus("PAID");
		return mapper.toResponse(payments.save(payment));
	}

}
