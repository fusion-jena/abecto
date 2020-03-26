/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.uni_jena.cs.fusion.abecto.model.ModelRepository;

@SpringBootApplication
@EnableScheduling
@EnableAsync
// not using default applications.properties to allow overriding by test configuration
@PropertySource(value = "classpath:abecto.properties")
@PropertySource(value = "classpath:test.properties", ignoreResourceNotFound = true)// optional additional test configurations
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

	@Bean()
	ModelRepository modelRepository(Environment env) {
		String storagePath = env.getRequiredProperty("abecto.storage");
		// OS independent home
		storagePath = storagePath.replaceFirst("~", System.getProperty("user.home"));
		return new ModelRepository(new File(storagePath + "/models"));
	}

	@GetMapping("")
	public String create() {
		return "ABECTO is running.";
	}
}