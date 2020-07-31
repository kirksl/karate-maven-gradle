package org.company;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController
{
	private static final String template = "Sending order from '%s' and to '%s'...";
	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/order")
	@ResponseStatus(HttpStatus.OK)
	public Order order(@RequestParam(value = "fromorder", defaultValue = "UserName123@order.com") String fromOrder,
	@RequestParam(value = "toorder", defaultValue = "UserName456@order.com") String toOrder)
	{
		return new Order(counter.incrementAndGet(), String.format(template, fromOrder, toOrder));
	}
}