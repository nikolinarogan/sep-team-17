package dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelRequestDTO {
    @NotBlank(message = "Merchant ID je obavezan")
    private String merchantId;

    @NotBlank(message = "Merchant Password je obavezan")
    private String merchantPassword;

    @NotBlank(message = "Merchant Order ID je obavezan")
    private String merchantOrderId;
}
