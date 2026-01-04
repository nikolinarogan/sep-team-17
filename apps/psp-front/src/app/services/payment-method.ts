import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PaymentMethod {
  private apiUrl = 'https://localhost:8443/api/payment-methods';

  constructor(private http: HttpClient) { }

  getAllMethods(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  createMethod(method: { name: string, serviceUrl: string }): Observable<any> {
    return this.http.post<any>(this.apiUrl, method);
  }

  deleteMethod(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
