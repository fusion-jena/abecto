package de.uni_jena.cs.fusion.abecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@PropertySources({ @PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "classpath:application_test.properties", ignoreResourceNotFound = true) })
@RestController
public class Abecto {

	public static void main(String[] args) {
		SpringApplication.run(Abecto.class, args);
	}

	@Bean
	public TaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setThreadNamePrefix("abecto-async-");
		executor.initialize();
		return executor;
	}
	
	@GetMapping("")
	public String create() {
		return "ABECTO is running.";
	}
}