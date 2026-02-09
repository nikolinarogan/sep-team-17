import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { RegisterRequest, LoginRequest, LoginResponse } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://localhost:8080/auth';

  constructor(private http: HttpClient) {}

  register(data: RegisterRequest): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/register`, data, {
      responseType: 'text' as 'json'
    });
  }

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token', response.token);
        }
      })
    );
  }

  verifyMfa(email: string, code: string): Observable<{ message: string; token: string }> {
    return this.http.post<{ message: string; token: string }>(`${this.apiUrl}/verify-mfa`, {
      email,
      code
    }).pipe(
      tap(response => {
        if (response?.token) {
          localStorage.setItem('token', response.token);
        }
      })
    );
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  logout(): void {
    localStorage.removeItem('token');
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      // Proveri da li je token u validnom formatu (ima 3 dela odvojena tačkom)
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.error('Invalid token format');
        this.logout();
        return false;
      }

      // Decode JWT token i proveri expiration
      const payload = JSON.parse(atob(parts[1]));
      
      // Proveri da li payload ima expiration
      if (!payload.exp) {
        console.error('Token missing expiration');
        this.logout();
        return false;
      }

      const expirationTime = payload.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      
      // Proveri da li je token istekao (sa malim buffer-om od 1 sekunde za clock skew)
      if (currentTime >= expirationTime - 1000) {
        // Token je istekao ili će uskoro isteći, obriši ga
        this.logout();
        return false;
      }
      
      return true;
    } catch (error) {
      console.error('Error checking token expiration:', error);
      // Ako ne može da dekodira token (npr. invalid base64), obriši ga
      // Ovo znači da je token oštećen ili neispravan
      this.logout();
      return false;
    }
  }

  getUserRole(): string | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      // Decode JWT token (payload is base64 encoded)
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || null;
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  changePassword(data: { email: string; oldPassword?: string; newPassword: string }): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/change-password`, data, {
      responseType: 'text' as 'json'
    });
  }
}
