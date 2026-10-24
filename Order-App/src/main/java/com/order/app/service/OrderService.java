package com.order.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.order.app.dto.CheckoutItem;
import com.order.app.dto.CheckoutRequest;
import com.order.app.dto.CheckoutResponse;
import com.order.app.dto.OrderPlacedEvent;
import com.order.app.entity.CustomerOrder;
import com.order.app.entity.OrderItem;
import com.order.app.mapper.OrderMapper;
import com.order.app.repository.OrderRepository;

@Service
public class OrderService {

	private final OrderRepository orders;
	private final OrderMapper mapper;
	private final RestClient products = RestClient.create("https://fakestoreapi.com");
	private final RestClient users = RestClient.create("http://localhost:8081");
	private final RestClient payments = RestClient.create("http://localhost:8083");

	public OrderService(OrderRepository orders, OrderMapper mapper) {
		this.orders = orders;
		this.mapper = mapper;
	}

	public String products() {
		return products.get().uri("/products").retrieve().body(String.class);
	}

	public CheckoutResponse checkout(CheckoutRequest request) {
		if (request.email() == null || request.email().isBlank() || request.items() == null || request.items().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and items are required");
		}
		try {
			users.get().uri("/api/saga/users?email={email}", request.email()).retrieve().toBodilessEntity();
		}
		catch (RestClientResponseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not valid");
		}
		CustomerOrder order = new CustomerOrder();
		order.setEmail(request.email());
		order.setStatus("PENDING");
		double amount = 0;
		for (CheckoutItem item : request.items()) {
			OrderItem line = new OrderItem();
			line.setProductId(item.productId());
			line.setTitle(item.title());
			line.setPrice(item.price());
			line.setQuantity(item.quantity());
			order.addItem(line);
			amount += item.price() * item.quantity();
		}
		order.setAmount(amount);
		CustomerOrder saved = orders.save(order);
		boolean fail = Boolean.TRUE.equals(request.failPayment());
		try {
			payments.post()
					.uri("/api/events/order-placed")
					.body(new OrderPlacedEvent(saved.getId(), amount, fail))
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == HttpStatus.PAYMENT_REQUIRED.value()) {
				throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment failed");
			}
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment failed");
		}
		saved.setStatus("CONFIRMED");
		return mapper.toResponse(orders.save(saved));
	}

	public void compensate(Long orderId) {
		if (orderId != null && orders.existsById(orderId)) {
			orders.deleteById(orderId);
		}
	}

	public CheckoutResponse get(Long id) {
		return orders.findById(id)
				.map(mapper::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
	}

}
