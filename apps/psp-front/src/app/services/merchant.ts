import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MerchantCreateRequest, MerchantCredentials } from '../models/psp-models';

@Injectable({
  providedIn: 'root',
})
export class Merchant {
  private apiUrl = 'https://localhost:8443/api/merchants';

  constructor(private http: HttpClient) { }

  getAllMerchants(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}`);
  }

  createMerchant(data: MerchantCreateRequest): Observable<MerchantCredentials> {
    return this.http.post<MerchantCredentials>(`${this.apiUrl}/create`, data);
  }
}
