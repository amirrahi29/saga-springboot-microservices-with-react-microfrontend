package com.order.app.dto;

import java.util.List;

public record CheckoutRequest(String email, List<CheckoutItem> items, Boolean failPayment) {
}
