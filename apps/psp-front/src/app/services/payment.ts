import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutResponse } from '../models/psp-models';

@Injectable({
  providedIn: 'root'
})
export class Payment {
  private apiUrl = 'https://localhost:8443/api';

  constructor(private http: HttpClient) { }

  getCheckoutData(uuid: string): Observable<CheckoutResponse> {
    return this.http.get<CheckoutResponse>(`${this.apiUrl}/payments/${uuid}`);
  }

  //  initiateCardPayment(uuid: string) {
  //   return this.http.post<{ paymentUrl: string }>(`${this.apiUrl}/payments/checkout/${uuid}/card`, {});
  // }

  // 1. Metoda koja traži od bekenda QR podatke (NBS string)
  // initiateQrPayment(uuid: string): Observable<{ qrData: string }> {
  //   return this.http.post<{ qrData: string }>(`${this.apiUrl}/payments/checkout/${uuid}/qr`, {});
  // }

  // 2. Metoda koja šalje skenirani string na proveru
  verifyQrScan(uuid: string, scannedData: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/payments/checkout/${uuid}/verify-qr`, {
      scannedString: scannedData
    });
  }
//   initiatePaypalPayment(uuid: string) {
//     // Putanja mora da odgovara onoj u Spring Boot PaymentController-u
//     return this.http.post(`${this.apiUrl}/payments/paypal/checkout/${uuid}`, {});
// }
/**
 * Generički poziv za inicijalizaciju plaćanja bilo kojom metodom.
 */
initiatePayment(uuid: string, methodName: string): Observable<{ paymentUrl?: string; qrData?: string }> {
  return this.http.post<{ paymentUrl?: string; qrData?: string }>(
    `${this.apiUrl}/payments/checkout/${uuid}/init/${methodName}`,
    {}
  );
}
}
