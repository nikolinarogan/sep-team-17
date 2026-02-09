import { Component, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../app/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'register.html',
  styleUrl: 'register.css'
})
export class RegisterComponent {
  registerForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      surname: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(12), Validators.pattern(/^(?=.*[a-zA-Z])(?=.*[0-9]).+$/)]]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      this.authService.register(this.registerForm.value).subscribe({
        next: (response) => {
          console.log('Registration successful:', response);
          this.isLoading = false;
          this.successMessage = 'A verification email has been sent to your email address. Please check your inbox and click the activation link to activate your account.';
          console.log('Success message set:', this.successMessage);
          this.cdr.detectChanges(); // Force change detection
          // Hide form and show only success message
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 5000);
        },
        error: (error) => {
          console.error('Registration error:', error);
          // Handle different error response formats
          if (error.error) {
            // If error.error is a string, use it directly
            if (typeof error.error === 'string') {
              this.errorMessage = error.error;
            } 
            // If error.error is an object with message property
            else if (error.error.message) {
              this.errorMessage = error.error.message;
            }
            // If error.error is an object, try to get the first value
            else {
              this.errorMessage = JSON.stringify(error.error);
            }
          } else if (error.message) {
            this.errorMessage = error.message;
          } else {
            this.errorMessage = 'Registration failed. Please try again.';
          }
          this.isLoading = false;
        }
      });
    }
  }
}