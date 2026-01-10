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
  ) {}

  ngOnInit() {
    // Proveri autentifikaciju pri inicijalizaciji aplikacije
    // Ako je korisnik na root URL i autentifikovan, redirect na home
    if (this.router.url === '/' && this.authService.isAuthenticated()) {
      this.router.navigate(['/home']);
    }
  }
}
