import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

/**
 * Guest guard – samo za korisnike koji NISU prijavljeni.
 * Blokira pristup login stranici ako je admin već ulogovan.
 */
export const guestGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('psp_admin_token');

  if (token) {
    router.navigate(['/admin/dashboard']);
    return false;
  }
  return true;
};
