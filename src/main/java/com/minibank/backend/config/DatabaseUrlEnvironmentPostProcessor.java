package com.minibank.backend.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "databaseUrlDerivedProperties";
	private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotEnvFileProperties";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		loadDotEnvFile(environment);

		// EnvironmentPostProcessor runs before application.properties is loaded, so we must look at the
		// raw env/dotenv keys too (SPRING_DATASOURCE_URL, DB_URL, DATABASE_URL, ...).
		String candidateUrl = firstNonBlank(
			environment.getProperty("spring.datasource.url"),
			environment.getProperty("flyway.url"),
			environment.getProperty("SPRING_DATASOURCE_URL"),
			environment.getProperty("DB_URL"),
			environment.getProperty("JDBC_DATABASE_URL"),
			environment.getProperty("DATABASE_URL"),
			environment.getProperty("NEON_DATABASE_URL")
		);
		if (!hasText(candidateUrl) || !isPostgresUrl(candidateUrl)) {
			return;
		}

		JdbcConnectionInfo jdbc = tryParseDatabaseUrl(candidateUrl);
		if (jdbc == null || !hasText(jdbc.url())) {
			return;
		}

		Map<String, Object> props = new LinkedHashMap<>();
		props.put("spring.datasource.url", jdbc.url());
		props.put("flyway.url", jdbc.url());

		String existingUsername = environment.getProperty("spring.datasource.username");
		if (!hasText(existingUsername) && hasText(jdbc.username())) {
			props.put("spring.datasource.username", jdbc.username());
		}
		String existingFlywayUser = environment.getProperty("flyway.user");
		if (!hasText(existingFlywayUser) && hasText(jdbc.username())) {
			props.put("flyway.user", jdbc.username());
		}

		String existingPassword = environment.getProperty("spring.datasource.password");
		if (!hasText(existingPassword) && hasText(jdbc.password())) {
			props.put("spring.datasource.password", jdbc.password());
		}
		String existingFlywayPassword = environment.getProperty("flyway.password");
		if (!hasText(existingFlywayPassword) && hasText(jdbc.password())) {
			props.put("flyway.password", jdbc.password());
		}

		PropertySource<?> propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, props);
		environment.getPropertySources().addFirst(propertySource);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private static void loadDotEnvFile(ConfigurableEnvironment environment) {
		Path envPath = Path.of(".env").toAbsolutePath().normalize();
		if (!Files.exists(envPath) || !Files.isRegularFile(envPath)) {
			return;
		}

		Map<String, Object> props = new LinkedHashMap<>();
		try {
			for (String rawLine : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
				String line = rawLine == null ? "" : rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("export ")) {
					line = line.substring("export ".length()).trim();
				}
				int idx = line.indexOf('=');
				if (idx <= 0) {
					continue;
				}
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();
				if (!hasText(key)) {
					continue;
				}

				value = unquote(value);

				// Do not override real environment variables / existing config.
				if (!environment.containsProperty(key)) {
					props.put(key, value);
				}
			}
		} catch (IOException ignored) {
			return;
		}

		if (props.isEmpty()) {
			return;
		}

		PropertySource<?> propertySource = new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, props);
		var sources = environment.getPropertySources();
		if (sources.contains(DOTENV_PROPERTY_SOURCE_NAME)) {
			sources.replace(DOTENV_PROPERTY_SOURCE_NAME, propertySource);
			return;
		}

		// Keep OS env/system properties higher precedence than .env.
		if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
		} else if (sources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
			sources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, propertySource);
		} else {
			sources.addFirst(propertySource);
		}
	}

	private static String unquote(String value) {
		if (value == null) {
			return null;
		}
		String v = value.trim();
		if (v.length() >= 2) {
			char first = v.charAt(0);
			char last = v.charAt(v.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return v.substring(1, v.length() - 1);
			}
		}
		return v;
	}

	private static JdbcConnectionInfo tryParseDatabaseUrl(String databaseUrl) {
		String trimmed = databaseUrl.trim();
		if (!isPostgresUrl(trimmed)) {
			return null;
		}

		URI uri;
		try {
			uri = URI.create(trimmed);
		} catch (IllegalArgumentException ex) {
			return null;
		}

		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String dbName = stripLeadingSlash(uri.getPath());
		if (!hasText(host) || !hasText(dbName)) {
			return null;
		}

		String username = null;
		String password = null;
		String userInfo = uri.getUserInfo();
		if (hasText(userInfo)) {
			int colonIdx = userInfo.indexOf(':');
			if (colonIdx >= 0) {
				username = urlDecode(userInfo.substring(0, colonIdx));
				password = urlDecode(userInfo.substring(colonIdx + 1));
			} else {
				username = urlDecode(userInfo);
			}
		}

		Map<String, String> queryParams = parseQueryParams(uri.getRawQuery());
		// libpq uses channel_binding; pgjdbc uses channelBinding.
		if (queryParams.containsKey("channel_binding") && !queryParams.containsKey("channelBinding")) {
			queryParams.put("channelBinding", queryParams.remove("channel_binding"));
		}

		String jdbcUrl = buildJdbcUrl(host, port, dbName, queryParams);
		return new JdbcConnectionInfo(jdbcUrl, username, password);
	}

	private static String buildJdbcUrl(String host, int port, String dbName, Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:postgresql://").append(host);
		if (port > 0) {
			sb.append(':').append(port);
		}
		sb.append('/').append(dbName);

		String query = toQueryString(params);
		if (hasText(query)) {
			sb.append('?').append(query);
		}
		return sb.toString();
	}

	private static Map<String, String> parseQueryParams(String rawQuery) {
		Map<String, String> params = new LinkedHashMap<>();
		if (!hasText(rawQuery)) {
			return params;
		}

		for (String pair : rawQuery.split("&")) {
			if (pair.isBlank()) {
				continue;
			}
			int idx = pair.indexOf('=');
			if (idx < 0) {
				params.put(urlDecode(pair), "");
				continue;
			}
			String key = urlDecode(pair.substring(0, idx));
			String value = urlDecode(pair.substring(idx + 1));
			params.put(key, value);
		}
		return params;
	}

	private static String toQueryString(Map<String, String> params) {
		if (params.isEmpty()) {
			return "";
		}
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!hasText(key)) {
				continue;
			}
			if (value == null) {
				parts.add(key);
			} else {
				parts.add(key + "=" + value);
			}
		}
		return String.join("&", parts);
	}

	private static String stripLeadingSlash(String path) {
		if (!hasText(path)) {
			return "";
		}
		return path.startsWith("/") ? path.substring(1) : path;
	}

	private static String urlDecode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static boolean isPostgresUrl(String value) {
		if (!hasText(value)) {
			return false;
		}
		String trimmed = value.trim();
		return trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://");
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (hasText(value)) {
				return value;
			}
		}
		return null;
	}

	record JdbcConnectionInfo(String url, String username, String password) {}
}
