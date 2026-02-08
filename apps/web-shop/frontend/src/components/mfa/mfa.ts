import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../app/services/auth.service';
import { IdleService } from '../../app/services/idle.service';

@Component({
  selector: 'app-mfa',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: 'mfa.html',
  styleUrl: 'mfa.css'
})
export class MfaComponent implements OnInit {
  email = '';
  code = '';
  errorMessage = '';
  isLoading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private idleService: IdleService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.email = params['email'] || '';
      if (!this.email) {
        this.router.navigate(['/login']);
      }
    });
  }

  onSubmit() {
    if (!this.email || !this.code || this.code.length !== 6) {
      this.errorMessage = 'Unesite 6-cifreni kod koji ste primili na email.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.verifyMfa(this.email, this.code.trim()).subscribe({
      next: () => {
        this.idleService.startWatching();
        this.router.navigate(['/home']);
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
    this.router.navigate(['/login']);
  }
}
