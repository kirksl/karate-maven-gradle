package org.company;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication
{
	public static void main(String[] args) throws Exception
	{
		new SpringApplication(OrderApplication.class).run(args);
	}
}
