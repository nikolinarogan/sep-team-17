import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../app/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'login.html',
  styleUrl: 'login.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
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
          } else if (response.token) {
            // Successful login
            this.isLoading = false;
            this.router.navigate(['/home']);
          } else {
            this.errorMessage = response.message || 'Login failed';
            this.isLoading = false;
          }
        },
        error: (error) => {
          this.errorMessage = error.error?.message || error.error || 'Login failed. Please try again.';
          this.isLoading = false;
        }
      });
    }
  }
}

