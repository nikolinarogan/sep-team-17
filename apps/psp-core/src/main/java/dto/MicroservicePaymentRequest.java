package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroservicePaymentRequest {
    private BigDecimal amount;
    private String currency;
    private String transactionUuid;
    // URL na koji eksterni sistem (PayPal) treba da vrati korisnika
    // Ovo će biti adresa Gateway-a, ne konkretne instance Core-a
    private String returnUrl;
    private String cancelUrl;
}