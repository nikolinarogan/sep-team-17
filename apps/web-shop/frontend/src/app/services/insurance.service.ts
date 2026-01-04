import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Insurance } from '../models/insurance.models';

@Injectable({
  providedIn: 'root'
})
export class InsuranceService {
  private apiUrl = 'http://localhost:8080/insurances';

  constructor(private http: HttpClient) {}

  getAllInsurances(): Observable<Insurance[]> {
    return this.http.get<Insurance[]>(this.apiUrl);
  }

  getInsuranceById(id: number): Observable<Insurance> {
    return this.http.get<Insurance>(`${this.apiUrl}/${id}`);
  }

  createInsurance(insurance: Insurance): Observable<Insurance> {
    return this.http.post<Insurance>(`${this.apiUrl}/create`, insurance);
  }

  updateInsurance(id: number, insurance: Insurance): Observable<Insurance> {
    return this.http.put<Insurance>(`${this.apiUrl}/${id}`, insurance);
  }

  deleteInsurance(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/${id}`, { 
      responseType: 'text' as 'json'
    });
  }
}

