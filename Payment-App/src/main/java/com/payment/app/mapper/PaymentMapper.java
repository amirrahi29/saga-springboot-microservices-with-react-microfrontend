package com.payment.app.mapper;

import org.springframework.stereotype.Component;

import com.payment.app.dto.PaymentResponse;
import com.payment.app.entity.Payment;

@Component
public class PaymentMapper {

	public PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getStatus());
	}

}
