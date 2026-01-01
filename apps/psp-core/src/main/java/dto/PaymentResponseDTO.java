package dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponseDTO {
    private String paymentUrl; // URL na koji Shop preusmjerava kupca (frontend)
    private String paymentId;  // UUID transakcije koji smo generisali
}
