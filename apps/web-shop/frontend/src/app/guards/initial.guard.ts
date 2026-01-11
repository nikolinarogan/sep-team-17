import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const initialGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

   const ignoredRoutes = ['/payment-success', '/payment-failed', '/payment-error'];

   const isIgnored = ignoredRoutes.some(r => state.url.startsWith(r));

   if (authService.isAuthenticated() && !isIgnored) {
    return router.createUrlTree(['/home']);
  }

  // Ako nije autentifikovan, dozvoli pristup login komponenti
  return true;
};

