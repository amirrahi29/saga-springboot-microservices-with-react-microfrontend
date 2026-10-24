package com.order.app.dto;

public record CheckoutItem(Long productId, String title, double price, int quantity) {
}
