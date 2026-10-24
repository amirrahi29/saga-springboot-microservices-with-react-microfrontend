package com.payment.app.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.app.dto.OrderPlacedEvent;
import com.payment.app.dto.PaymentResponse;
import com.payment.app.service.PaymentService;

@RestController
@RequestMapping("/api/events")
public class PaymentController {

	private final PaymentService payments;

	public PaymentController(PaymentService payments) {
		this.payments = payments;
	}

	@PostMapping("/order-placed")
	public PaymentResponse orderPlaced(@RequestBody OrderPlacedEvent event) {
		return payments.onOrderPlaced(event);
	}

}
