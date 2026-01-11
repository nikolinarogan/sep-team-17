import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { NavbarComponent } from '../components/navbar/navbar';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('frontend');

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.router.events.subscribe(event => {
    console.log("RUTER DOGAĐAJ:", event);
  });
  }

  ngOnInit() {
    /*const ignoredRoutes = ['/payment-success', '/payment-failed', '/payment-error'];
  if (this.authService.isAuthenticated() && !ignoredRoutes.some(r => this.router.url.startsWith(r))) {
    this.router.navigate(['/home']);
  }*/
  }
}
