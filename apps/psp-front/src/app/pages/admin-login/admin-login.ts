import { Component } from '@angular/core';
import { LoginRequestDTO } from '../../models/psp-models';
import { Auth } from '../../services/auth';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-login.html',
  styleUrl: './admin-login.css',
})
export class AdminLogin {
  credentials: LoginRequestDTO = {
    username: '',
    password: ''
  };

  errorMessage = '';
  isLoading = false;

  constructor(private authService: Auth, private router: Router) {}

  onSubmit() {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.credentials).subscribe({
      next: (response: string) => {
        console.log('Odgovor:', response);
        if (response === 'Uspešna prijava.') {
           this.router.navigate(['/admin/dashboard']);
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Greška:', err);
        this.isLoading = false;
        
        if (err.status === 401) {
          this.errorMessage = err.error || 'Pogrešni podaci.';
        } else {
          this.errorMessage = 'Greška na serveru. Proveri da li backend radi.';
        }
      }
    });
  }
}
