import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../app/services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'change-password.html',
  styleUrl: 'change-password.css'
})
export class ChangePasswordComponent {
  changePasswordForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  email: string = '';
  isFirstTimeAdmin: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Initialize with empty form, will update after getting params
    this.changePasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      oldPassword: [''],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    // Get email from route state or query params
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      this.isFirstTimeAdmin = params['firstTime'] === 'true';
      
      // Update form with email
      this.changePasswordForm.patchValue({ email: this.email });
      
      // For first-time admin, old password is not required
      if (this.isFirstTimeAdmin) {
        this.changePasswordForm.get('oldPassword')?.clearValidators();
        this.changePasswordForm.get('oldPassword')?.updateValueAndValidity();
      } else {
        this.changePasswordForm.get('oldPassword')?.setValidators([Validators.required]);
        this.changePasswordForm.get('oldPassword')?.updateValueAndValidity();
      }
    });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const form = control as FormGroup;
    const newPassword = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
      if (confirmPassword && confirmPassword.hasError('passwordMismatch')) {
        const errors = { ...confirmPassword.errors };
        delete errors['passwordMismatch'];
        confirmPassword.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }
      return null;
    }
  }

  onSubmit() {
    if (this.changePasswordForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const formValue = this.changePasswordForm.value;
      const changePasswordData: any = {
        email: formValue.email,
        newPassword: formValue.newPassword
      };

      // Only include oldPassword if it's not a first-time admin login
      if (!this.isFirstTimeAdmin && formValue.oldPassword) {
        changePasswordData.oldPassword = formValue.oldPassword;
      }

      this.authService.changePassword(changePasswordData).subscribe({
        next: (response) => {
          this.successMessage = 'Password changed successfully! Redirecting to login...';
          this.isLoading = false;
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        },
        error: (error) => {
          console.error('Change password error:', error);
          if (error.error) {
            if (typeof error.error === 'string') {
              this.errorMessage = error.error;
            } else if (error.error.message) {
              this.errorMessage = error.error.message;
            } else {
              this.errorMessage = JSON.stringify(error.error);
            }
          } else if (error.message) {
            this.errorMessage = error.message;
          } else {
            this.errorMessage = 'Failed to change password. Please try again.';
          }
          this.isLoading = false;
        }
      });
    }
  }
}

