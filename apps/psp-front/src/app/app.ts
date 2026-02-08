import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Auth } from './services/auth';
import { IdleService } from './services/idle.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('psp-front');

  constructor(
    private authService: Auth,
    private idleService: IdleService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        if (this.authService.getToken() && !event.urlAfterRedirects.includes('/admin/login')) {
          this.idleService.startWatching();
        } else {
          this.idleService.stopWatching();
        }
      }
    });
    if (this.authService.getToken()) {
      this.idleService.startWatching();
    }
  }
}
