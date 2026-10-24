package com.payment.app.dto;

public record OrderPlacedEvent(Long orderId, double amount, boolean failPayment) {
}
