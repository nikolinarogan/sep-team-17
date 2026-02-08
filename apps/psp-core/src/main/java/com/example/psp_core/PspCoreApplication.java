package com.example.psp_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.example.psp_core",
		"controller",
		"service",
		"config",
		"dto",
		"repository",
		"tools"
})
@EnableJpaRepositories(basePackages = "repository")
@EnableScheduling
@EntityScan(basePackages = "model")
public class PspCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(PspCoreApplication.class, args);
	}

	/**
	 * Eureka supplier koji koristi tvoju SSL logiku.
	 */
	@Bean
	public EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier() {
		return (sslContext, hostnameVerifier) -> {
			SSLContext contextToUse = (sslContext != null) ? sslContext : getSystemSslContext();

			var socketFactory = SSLConnectionSocketFactoryBuilder.create()
					.setSslContext(contextToUse)
					.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();

			var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
					.setSSLSocketFactory(socketFactory)
					.build();

			var httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.build();

			return new HttpComponentsClientHttpRequestFactory(httpClient);
		};
	}

	private SSLContext getSystemSslContext() {
		try {
			return SSLContext.getDefault();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * RestClient Builder koji koristi tvoj Insecure SSL kontekst.
	 * NAPOMENA: Ovde VIŠE NEMA @LoadBalanced jer ručno upravljamo instancama.
	 */
	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory(
						HttpClients.custom()
								.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
										.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
												.setSslContext(createInsecureSslContext())
												.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
												.build())
										.build())
								.build()));
	}

	private SSLContext createInsecureSslContext() {
		try {
			return org.apache.hc.core5.ssl.SSLContexts.custom()
					.loadTrustMaterial(null, (chain, authType) -> true)
					.build();
		} catch (Exception e) {
			throw new RuntimeException("Greška pri kreiranju SSL konteksta", e);
		}
	}
}