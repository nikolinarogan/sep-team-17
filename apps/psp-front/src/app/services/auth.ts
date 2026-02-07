import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequestDTO } from '../models/psp-models';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private apiUrl = 'https://localhost:8000/api/admin';

  constructor(private http: HttpClient) { }

  login(credentials: LoginRequestDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/login`, credentials, { 
      responseType: 'text' 
    });
  }
  
}
