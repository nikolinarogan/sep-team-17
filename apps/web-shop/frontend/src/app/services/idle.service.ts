import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class IdleService {
  private idleTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minuta
  private hasListeners = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  startWatching(): void {
    if (this.hasListeners) {
      this.resetTimer();
      return;
    }
    this.hasListeners = true;
    this.resetTimer();
    ['click', 'keydown', 'mousemove', 'scroll', 'touchstart'].forEach(ev => {
      document.addEventListener(ev, () => this.resetTimer());
    });
  }

  stopWatching(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
      this.idleTimer = null;
    }
    this.hasListeners = false;
  }

  private resetTimer(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => {
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { idle: 'true' } });
    }, this.IDLE_TIMEOUT_MS);
  }
}