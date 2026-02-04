package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import javax.net.ssl.SSLContext;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

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
}