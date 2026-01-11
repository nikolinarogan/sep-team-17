package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    private String merchantOrderId;   // ID narudžbine iz šopa
    private String pspTransactionId;
    private String paymentMethod;
    private String status;            // "SUCCESS", "FAILED", "ERROR"
    private LocalDateTime timestamp;
}