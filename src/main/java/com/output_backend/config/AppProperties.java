package com.output_backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Jwt jwt = new Jwt();
	private final Embedding embedding = new Embedding();
	private final Llm llm = new Llm();
	private final Rag rag = new Rag();
	private final Retention retention = new Retention();
	private final RedisCache redisCache = new RedisCache();
	private final Cors cors = new Cors();

	public Jwt getJwt() {
		return jwt;
	}

	public Embedding getEmbedding() {
		return embedding;
	}

	public Llm getLlm() {
		return llm;
	}

	public Rag getRag() {
		return rag;
	}

	public Retention getRetention() {
		return retention;
	}

	public RedisCache getRedisCache() {
		return redisCache;
	}

	public Cors getCors() {
		return cors;
	}

	public static class Jwt {
		private String issuer = "http://localhost:8080";
		private String audience = "auth-api";
		private String publicKeyPath = "./keys/public.pem";

		public String getIssuer() {
			return issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}

		public String getAudience() {
			return audience;
		}

		public void setAudience(String audience) {
			this.audience = audience;
		}

		public String getPublicKeyPath() {
			return publicKeyPath;
		}

		public void setPublicKeyPath(String publicKeyPath) {
			this.publicKeyPath = publicKeyPath;
		}
	}

	public static class Embedding {
		private String defaultProvider = "ollama";
		private int dimensions = 1024;

		public String getDefaultProvider() {
			return defaultProvider;
		}

		public void setDefaultProvider(String defaultProvider) {
			this.defaultProvider = defaultProvider;
		}

		public int getDimensions() {
			return dimensions;
		}

		public void setDimensions(int dimensions) {
			this.dimensions = dimensions;
		}
	}

	public static class Llm {
		private String defaultProvider = "ollama";

		public String getDefaultProvider() {
			return defaultProvider;
		}

		public void setDefaultProvider(String defaultProvider) {
			this.defaultProvider = defaultProvider;
		}
	}

	public static class Rag {
		private int topK = 5;
		private double similarityThreshold = 0.70;

		public int getTopK() {
			return topK;
		}

		public void setTopK(int topK) {
			this.topK = topK;
		}

		public double getSimilarityThreshold() {
			return similarityThreshold;
		}

		public void setSimilarityThreshold(double similarityThreshold) {
			this.similarityThreshold = similarityThreshold;
		}
	}

	public static class Retention {
		/** Default conversation message retention (days). Enforced in later conversation phases. */
		private int conversationDays = 90;

		public int getConversationDays() {
			return conversationDays;
		}

		public void setConversationDays(int conversationDays) {
			this.conversationDays = conversationDays;
		}
	}

	public static class RedisCache {
		private long queryEmbeddingTtlSeconds = 7200;
		private long retrievalTtlSeconds = 900;
		private long answerTtlSeconds = 3600;
		private boolean answerCacheEnabled = false;

		public long getQueryEmbeddingTtlSeconds() {
			return queryEmbeddingTtlSeconds;
		}

		public void setQueryEmbeddingTtlSeconds(long queryEmbeddingTtlSeconds) {
			this.queryEmbeddingTtlSeconds = queryEmbeddingTtlSeconds;
		}

		public long getRetrievalTtlSeconds() {
			return retrievalTtlSeconds;
		}

		public void setRetrievalTtlSeconds(long retrievalTtlSeconds) {
			this.retrievalTtlSeconds = retrievalTtlSeconds;
		}

		public long getAnswerTtlSeconds() {
			return answerTtlSeconds;
		}

		public void setAnswerTtlSeconds(long answerTtlSeconds) {
			this.answerTtlSeconds = answerTtlSeconds;
		}

		public boolean isAnswerCacheEnabled() {
			return answerCacheEnabled;
		}

		public void setAnswerCacheEnabled(boolean answerCacheEnabled) {
			this.answerCacheEnabled = answerCacheEnabled;
		}
	}

	public static class Cors {
		private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}
	}
}
