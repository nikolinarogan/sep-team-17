package com.example.pspcrypto;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

@SpringBootApplication
public class PspCryptoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PspCryptoApplication.class, args);
	}

	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	public EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier() {
		return (sslContext, hostnameVerifier) -> {
			SSLContext contextToUse = createInsecureSslContext();

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
