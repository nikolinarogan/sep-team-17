package dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CryptoPaymentResponseDTO {
    private String btcAmount;
    private String walletAddress;
    private String qrCodeUrl;
}
