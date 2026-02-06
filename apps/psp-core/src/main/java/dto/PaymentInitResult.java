package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper za rezultat inicijalizacije plaćanja.
 * CARD, PAYPAL: redirectUrl
 * QR: qrData
 * CRYPTO: redirectUrl ili qrData (zavisno od provajdera)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResult {
    private String redirectUrl;
    private String qrData;
}