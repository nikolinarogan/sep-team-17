import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned).pipe(
      catchError((error) => {
        // Ako je token istekao (401 Unauthorized), obriši token i preusmeri na login
        if (error.status === 401) {
          authService.logout();
          router.navigate(['/login'], {
            queryParams: { expired: 'true' }
          });
        }
        return throwError(() => error);
      })
    );
  }

  return next(req);
};

