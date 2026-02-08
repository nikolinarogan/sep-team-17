import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from './auth';

/**
 * PCI DSS 8.2.8 – Idle timeout 15 minuta.
 * Prati aktivnost korisnika na frontendu (klik, tastatura, miš, scroll).
 * Ako nema aktivnosti 15 min, odjavi korisnika i preusmeri na login.
 */
@Injectable({ providedIn: 'root' })
export class IdleService {
  private idleTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minuta
  private hasListeners = false;
  private readonly boundReset = () => this.resetTimer();
  private readonly events = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart'] as const;

  constructor(
    private authService: Auth,
    private router: Router
  ) {}

  startWatching(): void {
    if (this.hasListeners) {
      this.resetTimer();
      return;
    }
    this.hasListeners = true;
    this.resetTimer();
    this.events.forEach((ev) => document.addEventListener(ev, this.boundReset));
  }

  stopWatching(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
      this.idleTimer = null;
    }
    if (this.hasListeners) {
      this.events.forEach((ev) => document.removeEventListener(ev, this.boundReset));
    }
    this.hasListeners = false;
  }

  private resetTimer(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => {
      this.authService.logout();
      this.router.navigate(['/admin/login'], { queryParams: { idle: 'true' } });
    }, this.IDLE_TIMEOUT_MS);
  }
}
