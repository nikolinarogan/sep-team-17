import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequestDTO } from '../models/psp-models';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private apiUrl = 'https://localhost:8000/api/admin';

  constructor(private http: HttpClient) { }

  login(credentials: LoginRequestDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        // Ako bek vrati token, sačuvaj ga
        if (response && response.token) {
          localStorage.setItem('psp_admin_token', response.token);
        }
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('psp_admin_token');
  }

  logout(): void {
    localStorage.removeItem('psp_admin_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
  
}
