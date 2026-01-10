import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const initialGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Ako je korisnik autentifikovan, redirect na home
  if (authService.isAuthenticated()) {
    return router.createUrlTree(['/home']);
  }

  // Ako nije autentifikovan, dozvoli pristup login komponenti
  return true;
};

