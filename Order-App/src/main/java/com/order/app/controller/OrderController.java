package com.order.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.order.app.dto.CheckoutRequest;
import com.order.app.dto.CheckoutResponse;
import com.order.app.dto.PaymentFailedEvent;
import com.order.app.service.OrderService;

@RestController
@RequestMapping("/api")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/products")
	public String products() {
		return orderService.products();
	}

	@PostMapping("/checkout")
	public CheckoutResponse checkout(@RequestBody CheckoutRequest request) {
		return orderService.checkout(request);
	}

	@PostMapping("/events/payment-failed")
	public void paymentFailed(@RequestBody PaymentFailedEvent event) {
		orderService.compensate(event.orderId());
	}

	@GetMapping("/orders/{id}")
	public CheckoutResponse order(@PathVariable Long id) {
		return orderService.get(id);
	}

}
