import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Auth } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Auth);
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
        if (error.status === 401) {
          const isLoginRequest = error.url?.includes('/login');
          if (!isLoginRequest) {
            authService.logout();
            const msg = error.error?.message || error.error || '';
            const msgStr = typeof msg === 'string' ? msg : (msg as { message?: string })?.message || '';
            const isIdle = msgStr.toLowerCase().includes('inactivity');
            router.navigate(['/admin/login'], {
              queryParams: { expired: 'true', idle: isIdle ? 'true' : undefined }
            });
          }
        }
        return throwError(() => error);
      })
    );
  }

  return next(req);
};