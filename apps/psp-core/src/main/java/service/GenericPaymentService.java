package service;

import dto.*;
import model.PaymentMethod;
import model.PaymentTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import repository.PaymentMethodRepository;
import repository.PaymentTransactionRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GenericPaymentService {

    private final DiscoveryClient discoveryClient;
    private final RestClient.Builder restClientBuilder;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository transactionRepository;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Value("${psp.external-url}")
    private String externalUrl;

    public GenericPaymentService(DiscoveryClient discoveryClient,
                                 RestClient.Builder restClientBuilder,
                                 PaymentMethodRepository paymentMethodRepository,
                                 PaymentTransactionRepository transactionRepository) {
        this.discoveryClient = discoveryClient;
        this.restClientBuilder = restClientBuilder;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
    }

    public PaymentInitResult initiate(PaymentTransaction tx, String methodName) {
        PaymentMethod method = paymentMethodRepository.findByName(methodName)
                .orElseThrow(() -> new RuntimeException("Nepoznat metod: " + methodName));

        MicroservicePaymentRequest req = createRequest(tx, methodName);

        // RUČNI RETRY LOGIKA
        int maxAttempts = 3;
        Exception lastException = null;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                // 1. Dobijamo instance sa Eureke za konkretan servis (npr. psp-paypal)
                List<ServiceInstance> instances = discoveryClient.getInstances(method.getServiceName());
                if (instances.isEmpty()) {
                    throw new RuntimeException("Nema dostupnih instanci za servis: " + method.getServiceName());
                }

                // 2. Round Robin biranje instance
                int index = Math.abs(counter.getAndIncrement()) % instances.size();
                ServiceInstance instance = instances.get(index);
                String baseUrl = instance.getUri().toString(); // Dobijamo npr. https://localhost:8082

                System.out.println("POKUŠAJ " + (i + 1) + " -> CORE šalje zahtev na: " + baseUrl);

                // 3. Izvršavanje poziva
                MicroservicePaymentResponse response = restClientBuilder.build()
                        .post()
                        .uri(baseUrl + "/api/connector/init")
                        .body(req)
                        .retrieve()
                        .body(MicroservicePaymentResponse.class);

                if (response != null && response.isSuccess()) {
                    tx.setExecutionId(response.getExternalId());
                    transactionRepository.save(tx);
                    return PaymentInitResult.builder().redirectUrl(response.getRedirectUrl()).build();
                }
            } catch (Exception e) {
                System.err.println("Pokušaj " + (i + 1) + " propao za " + method.getServiceName() + ": " + e.getMessage());
                lastException = e;
                if (i < maxAttempts - 1) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new RuntimeException("Svi pokušaji komunikacije sa " + methodName + " su propali. Poslednja greška: " + lastException.getMessage());
    }

    public boolean capture(String methodName, String executionId) {
        PaymentMethod method = paymentMethodRepository.findByName(methodName).orElseThrow();

        int maxAttempts = 3;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(method.getServiceName());
                if (instances.isEmpty()) throw new RuntimeException("Nema instanci");

                int index = Math.abs(counter.getAndIncrement()) % instances.size();
                String baseUrl = instances.get(index).getUri().toString();

                Boolean result = restClientBuilder.build()
                        .post()
                        .uri(baseUrl + "/api/connector/capture/" + executionId)
                        .retrieve()
                        .body(Boolean.class);

                return result != null && result;
            } catch (Exception e) {
                System.err.println("Capture pokušaj " + (i + 1) + " neuspešan.");
                if (i == maxAttempts - 1) return false;
            }
        }
        return false;
    }

    private MicroservicePaymentRequest createRequest(PaymentTransaction tx, String methodName) {
        String returnUrl = String.format("%s/api/payments/external/capture?method=%s&uuid=%s",
                externalUrl, methodName, tx.getUuid());

        return MicroservicePaymentRequest.builder()
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactionUuid(tx.getUuid())
                .returnUrl(returnUrl)
                .cancelUrl(tx.getFailedUrl())
                .build();
    }
}