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
    next: (response) => {
      if (response.token) {
        console.log('Login uspešan, token sačuvan.');
        this.router.navigate(['/admin/dashboard']);
      } else {
        this.errorMessage = 'Neočekivan odgovor servera.';
      }
      this.isLoading = false;
    },
    error: (err) => {
      this.isLoading = false;
      if (err.status === 401) {
        this.errorMessage = 'Pogrešno korisničko ime ili lozinka.';
      } else {
        this.errorMessage = 'Sistem nije dostupan.';
      }
    }
  });
}
}
