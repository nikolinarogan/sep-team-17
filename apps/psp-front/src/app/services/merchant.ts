import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MerchantCreateRequest, MerchantCredentials } from '../models/psp-models';

@Injectable({
  providedIn: 'root',
})
export class Merchant {
  private apiUrl = 'https://localhost:8000/api/merchants';
  private adminApiUrl = 'https://localhost:8000/api/admin';

  constructor(private http: HttpClient) { }

  getAllMerchants(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}`);
  }

  createMerchant(data: MerchantCreateRequest): Observable<MerchantCredentials> {
    return this.http.post<MerchantCredentials>(`${this.apiUrl}/create`, data);
  }
  getMerchantById(id: string): Observable<any> {
    return this.http.get<any>(`${this.adminApiUrl}/merchants/${id}`);
  }

  saveMerchantServices(id: string, configs: any[]): Observable<string> {
    return this.http.post(`${this.adminApiUrl}/merchants/${id}/services`, configs, { 
      responseType: 'text' 
    });
  }
  
  getMerchantSubscriptions(id: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${id}/subscriptions`);
  }
}
