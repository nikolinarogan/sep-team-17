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
import tools.AuditLogger; // Import tvog novog alata

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GenericPaymentService {

    private final DiscoveryClient discoveryClient;
    private final RestClient.Builder restClientBuilder;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final AuditLogger auditLogger; // Dodato

    private final AtomicInteger counter = new AtomicInteger(0);

    @Value("${psp.external-url}")
    private String externalUrl;

    public GenericPaymentService(DiscoveryClient discoveryClient,
                                 RestClient.Builder restClientBuilder,
                                 PaymentMethodRepository paymentMethodRepository,
                                 PaymentTransactionRepository transactionRepository,
                                 AuditLogger auditLogger) { // Dodato u konstruktor
        this.discoveryClient = discoveryClient;
        this.restClientBuilder = restClientBuilder;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
    }

    public PaymentInitResult initiate(PaymentTransaction tx, String methodName) {
        auditLogger.logEvent("INITIATE_GENERIC_START", "PENDING",
                "Method: " + methodName + " | UUID: " + tx.getUuid() + " | Amount: " + tx.getAmount());

        PaymentMethod method = paymentMethodRepository.findByName(methodName)
                .orElseThrow(() -> new RuntimeException("Nepoznat metod: " + methodName));

        MicroservicePaymentRequest req = createRequest(tx, methodName);

        int maxAttempts = 3;
        Exception lastException = null;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(method.getServiceName());
                if (instances.isEmpty()) {
                    auditLogger.logSecurityAlert("INSTANCE_NOT_FOUND", "No instances for service: " + method.getServiceName());
                    throw new RuntimeException("Nema dostupnih instanci za servis: " + method.getServiceName());
                }

                int index = Math.abs(counter.getAndIncrement()) % instances.size();
                ServiceInstance instance = instances.get(index);
                String baseUrl = instance.getUri().toString();

                // PCI DSS 10.2.4: Beleženje svakog pokušaja komunikacije
                auditLogger.logEvent("MICROSERVICE_COMM_ATTEMPT", "RETRY",
                        String.format("Attempt: %d | Service: %s | Instance: %s", (i + 1), method.getServiceName(), baseUrl));

                MicroservicePaymentResponse response = restClientBuilder.build()
                        .post()
                        .uri(baseUrl + "/api/connector/init")
                        .body(req)
                        .retrieve()
                        .body(MicroservicePaymentResponse.class);

                if (response != null && response.isSuccess()) {
                    tx.setExecutionId(response.getExternalId());
                    transactionRepository.save(tx);

                    auditLogger.logEvent("INITIATE_GENERIC_SUCCESS", "SUCCESS",
                            "External ID received: " + response.getExternalId());

                    return PaymentInitResult.builder().redirectUrl(response.getRedirectUrl()).build();
                }
            } catch (Exception e) {
                auditLogger.logEvent("MICROSERVICE_COMM_FAILED", "ERROR",
                        "Attempt " + (i + 1) + " failed: " + e.getMessage());

                lastException = e;
                if (i < maxAttempts - 1) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        auditLogger.logSecurityAlert("ALL_RETRIES_FAILED", "Failed to communicate with " + methodName + " after " + maxAttempts + " attempts.");
        throw new RuntimeException("Svi pokušaji komunikacije su propali.");
    }

    public boolean capture(String methodName, String executionId) {
        auditLogger.logEvent("CAPTURE_EXTERNAL_START", "PENDING",
                "Method: " + methodName + " | ExecutionID: " + executionId);

        PaymentMethod method = paymentMethodRepository.findByName(methodName).orElseThrow();

        int maxAttempts = 3;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(method.getServiceName());
                if (instances.isEmpty()) throw new RuntimeException("Nema instanci");

                int index = Math.abs(counter.getAndIncrement()) % instances.size();
                String baseUrl = instances.get(index).getUri().toString();

                auditLogger.logEvent("CAPTURE_ATTEMPT", "RETRY", "Attempt: " + (i+1) + " Instance: " + baseUrl);

                Boolean result = restClientBuilder.build()
                        .post()
                        .uri(baseUrl + "/api/connector/capture/" + executionId)
                        .retrieve()
                        .body(Boolean.class);

                boolean isSuccess = result != null && result;
                auditLogger.logEvent("CAPTURE_FINISHED", isSuccess ? "SUCCESS" : "FAILED", "Method: " + methodName);

                return isSuccess;
            } catch (Exception e) {
                auditLogger.logEvent("CAPTURE_ERROR", "ERROR", "Attempt " + (i + 1) + " error: " + e.getMessage());
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