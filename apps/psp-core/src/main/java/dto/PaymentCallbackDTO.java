package dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentCallbackDTO {
    // uuid koji smo poslali servisu
    private String paymentId;

    // SUCCESS, FAILED, ERROR
    private String status;

    // Generički id transakcije (za banku je GLOBAL_ID, za PayPal je PaymentID)
    private String externalTransactionId;

    // Dodatni id izvršenja (za Banku je STAN, za ostale može biti null)
    private String executionId;

    // Vrijeme kad je banka/servis obradio uplatu
    private LocalDateTime serviceTimestamp;
}
