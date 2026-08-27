package com.minibank.backend.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String uploadsDir = Path.of("uploads").toAbsolutePath().toUri().toString();
		String dataUploadsDir = Path.of("data", "uploads").toAbsolutePath().toUri().toString();

		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadsDir, dataUploadsDir);
	}
}
