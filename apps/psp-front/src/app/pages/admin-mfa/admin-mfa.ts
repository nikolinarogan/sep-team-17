import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-admin-mfa',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-mfa.html',
  styleUrl: './admin-mfa.css',
})
export class AdminMfa implements OnInit {
  username = '';
  code = '';
  errorMessage = '';
  isLoading = false;

  constructor(
    private authService: Auth,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.username = params['username'] || '';
      if (!this.username) {
        this.router.navigate(['/admin/login']);
      }
    });
  }

  onSubmit() {
    if (!this.username || !this.code || this.code.length !== 6) {
      this.errorMessage = 'Unesite 6-cifreni kod koji ste primili na email.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.verifyMfa(this.username, this.code.trim()).subscribe({
      next: () => {
        this.router.navigate(['/admin/dashboard']);
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.status === 401
          ? (typeof err.error === 'string' ? err.error : 'Neispravan ili istekao kod. Pokušajte ponovo sa prijavom.')
          : 'Došlo je do greške. Pokušajte ponovo.';
      }
    });
  }

  backToLogin() {
    this.router.navigate(['/admin/login']);
  }
}
