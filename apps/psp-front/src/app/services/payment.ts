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
  
}
