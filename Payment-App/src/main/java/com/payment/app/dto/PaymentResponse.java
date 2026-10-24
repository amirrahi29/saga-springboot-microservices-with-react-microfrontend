package com.payment.app.dto;

public record PaymentResponse(Long paymentId, Long orderId, String status) {
}
