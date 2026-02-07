import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutResponse } from '../models/psp-models';
import { timeout } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class Payment {
  private apiUrl = 'https://localhost:8000/api';

  constructor(private http: HttpClient) { }

  getCheckoutData(uuid: string): Observable<CheckoutResponse> {
    return this.http.get<CheckoutResponse>(`${this.apiUrl}/payments/${uuid}`);
  }

  // 2. Metoda koja šalje skenirani string na proveru
  verifyQrScan(uuid: string, scannedData: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/payments/checkout/${uuid}/verify-qr`, {
      scannedString: scannedData
    });
  }
/**
 * Generički poziv za inicijalizaciju plaćanja bilo kojom metodom.
 */
initiatePayment(uuid: string, methodName: string): Observable<{ paymentUrl?: string; qrData?: string }> {
  return this.http.post<{ paymentUrl?: string; qrData?: string }>(
    `${this.apiUrl}/payments/checkout/${uuid}/init/${methodName}`,
    {}
  ).pipe(
    timeout(30000)  // 30 sekundi - ako backend ne odgovori, Observable baca TimeoutError
  );
}
}
