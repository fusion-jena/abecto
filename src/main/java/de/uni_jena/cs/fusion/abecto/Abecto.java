package de.uni_jena.cs.fusion.abecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@SpringBootApplication
@EnableScheduling
@EnableAsync
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
	
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new Jdk8Module());
	}
}