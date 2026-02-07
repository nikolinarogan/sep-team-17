package com.example.psp_paypal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication
@EnableDiscoveryClient
public class PspPaypalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PspPaypalApplication.class, args);
	}
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
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
