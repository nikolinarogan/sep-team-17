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

@Service
public class GenericPaymentService {

    private final DiscoveryClient discoveryClient;
    private final RestClient.Builder restClientBuilder;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository transactionRepository;

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
        // 1. Učitaj konfiguraciju iz baze
        PaymentMethod method = paymentMethodRepository.findByName(methodName)
                .orElseThrow(() -> new RuntimeException("Nepoznat metod: " + methodName));

        // 2. Generiši UNIVERZALNI callback URL
        // Npr: https://localhost:8000/api/payments/external/capture?method=CRYPTO&uuid=...
        String returnUrl = String.format("%s/api/payments/external/capture?method=%s&uuid=%s",
                externalUrl, methodName, tx.getUuid());

        // 3. Pripremi zahtjev
        MicroservicePaymentRequest req = MicroservicePaymentRequest.builder()
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactionUuid(tx.getUuid())
                .returnUrl(returnUrl)
                .cancelUrl(tx.getFailedUrl())
                .build();

        // 4. Nađi servis na Eureki (psp-paypal, psp-crypto...)
        String serviceBaseUrl = getServiceUrl(method.getServiceName());

        // 5. Pozovi mikroservis
        try {
            MicroservicePaymentResponse response = restClientBuilder.build()
                    .post()
                    .uri(serviceBaseUrl + "/api/connector/init")
                    .body(req)
                    .retrieve()
                    .body(MicroservicePaymentResponse.class);

            if (response != null && response.isSuccess()) {
                tx.setExecutionId(response.getExternalId());
                transactionRepository.save(tx);

                return PaymentInitResult.builder()
                        .redirectUrl(response.getRedirectUrl())
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Greška u komunikaciji sa servisom " + method.getServiceName(), e);
        }
        throw new RuntimeException("Inicijalizacija plaćanja nije uspela.");
    }

    // Capture metoda koja radi za BILO KOJI servis
    // Prima 'token' koji može biti PayPal OrderID ili Crypto Hash
    public boolean capture(String methodName, String executionId) {
        PaymentMethod method = paymentMethodRepository.findByName(methodName).orElseThrow();
        String serviceBaseUrl = getServiceUrl(method.getServiceName());

        try {
            Boolean result = restClientBuilder.build()
                    .post()
                    .uri(serviceBaseUrl + "/api/connector/capture/" + executionId)
                    .retrieve()
                    .body(Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }

    private String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) throw new RuntimeException("Servis " + serviceName + " nije dostupan!");
        return instances.get(0).getUri().toString();
    }
}