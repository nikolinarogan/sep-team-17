import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../app/services/auth.service';
import { IdleService } from '../../app/services/idle.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'login.html',
  styleUrl: 'login.css'
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string = '';
  infoMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private idleService: IdleService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  ngOnInit() {
    // Ako je korisnik već autentifikovan, preusmeri ga na home
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/home']);
      return;
    }

    // Proveri da li je korisnik preusmeren zbog isteka tokena
   this.route.queryParams.subscribe(params => {
      if (params['idle'] === 'true') {
        this.infoMessage = 'Sesija je istekla zbog neaktivnosti. Molimo prijavite se ponovo.';
      } else if (params['expired'] === 'true') {
        this.infoMessage = 'Sesija je istekla. Molimo prijavite se ponovo.';
      }
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      this.authService.login(this.loginForm.value).subscribe({
        next: (response) => {
          if (response.mustChangePassword) {
            // Admin must change password - redirect to change password page
            this.isLoading = false;
            this.router.navigate(['/change-password'], {
              queryParams: { 
                email: this.loginForm.get('email')?.value,
                firstTime: 'true'
              }
            });
          } else if (response.status === 'MFA_REQUIRED') {
            // Admin MFA verification required
            this.isLoading = false;
            this.router.navigate(['/mfa'], {
              queryParams: { email: response.email }
            });
          } else if (response.token) {
            // Successful login
            this.isLoading = false;
            this.idleService.startWatching();
            this.router.navigate(['/home']);
          } else {
            this.errorMessage = response.message || 'Login failed';
            this.isLoading = false;
          }
        },
        error: (error) => {
          const msg = error.error?.message || error.error || 'Login failed. Please try again.';
          this.errorMessage = typeof msg === 'string' ? msg : String(msg);
          if (error.status === 429) {
            this.infoMessage = '';
          }
          this.isLoading = false;
        }
      });
    }
  }
}

