package com.output_backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StreamUtils;

@Configuration
public class JwtConfig {

	@Bean
	RSAPublicKey rsaPublicKey(AppProperties appProperties, ResourceLoader resourceLoader) throws Exception {
		return readPublicKey(resolvePem(appProperties.getJwt().getPublicKeyPath(), resourceLoader));
	}

	@Bean
	JwtDecoder jwtDecoder(RSAPublicKey publicKey, AppProperties appProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(appProperties.getJwt().getIssuer());
		OAuth2TokenValidator<Jwt> withAudience = token -> {
			if (token.getAudience() != null && token.getAudience().contains(appProperties.getJwt().getAudience())) {
				return OAuth2TokenValidatorResult.success();
			}
			OAuth2Error error = new OAuth2Error("invalid_token", "Required audience is missing", null);
			return OAuth2TokenValidatorResult.failure(error);
		};

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
		return decoder;
	}

	private static String resolvePem(String location, ResourceLoader resourceLoader) throws IOException {
		Path path = Path.of(location);
		if (Files.exists(path)) {
			return Files.readString(path);
		}

		Resource resource = resourceLoader.getResource(location);
		if (resource.exists()) {
			try (InputStream inputStream = resource.getInputStream()) {
				return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			}
		}

		Resource fileResource = resourceLoader.getResource("file:" + location);
		if (fileResource.exists()) {
			try (InputStream inputStream = fileResource.getInputStream()) {
				return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			}
		}

		throw new IllegalStateException("JWT public key not found at: " + location);
	}

	private static RSAPublicKey readPublicKey(String pem) throws Exception {
		String sanitized = pem
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
		byte[] decoded = Base64.getDecoder().decode(sanitized);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
		return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
	}
}
