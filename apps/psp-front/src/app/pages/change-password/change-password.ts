import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './change-password.html',
  styleUrl: './change-password.css'
})
export class ChangePassword {
  changePasswordForm: FormGroup;
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  username = '';
  isFirstTimeAdmin = false;

  constructor(
    private fb: FormBuilder,
    private authService: Auth,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.changePasswordForm = this.fb.group(
      {
        username: ['', [Validators.required]],
        oldPassword: [''],
        newPassword: ['', [Validators.required, Validators.minLength(12), Validators.pattern(/^(?=.*[a-zA-Z])(?=.*[0-9]).+$/)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator }
    );

    this.route.queryParams.subscribe((params) => {
      this.username = params['username'] || '';
      this.isFirstTimeAdmin = params['firstTime'] === 'true';

      this.changePasswordForm.patchValue({ username: this.username });

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
      if (confirmPassword?.hasError('passwordMismatch')) {
        const errors = { ...confirmPassword.errors };
        delete errors['passwordMismatch'];
        confirmPassword.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }
      return null;
    }
  }

  onSubmit(): void {
    if (this.changePasswordForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const formValue = this.changePasswordForm.value;
      const changePasswordData: { username: string; oldPassword?: string; newPassword: string } = {
        username: formValue.username,
        newPassword: formValue.newPassword
      };

      if (!this.isFirstTimeAdmin && formValue.oldPassword) {
        changePasswordData.oldPassword = formValue.oldPassword;
      }

      this.authService.changePassword(changePasswordData).subscribe({
        next: () => {
          this.successMessage = 'Lozinka uspešno promenjena. Preusmeravanje na prijavu...';
          this.isLoading = false;
          setTimeout(() => {
            this.router.navigate(['/admin/login']);
          }, 2000);
        },
        error: (error) => {
          this.errorMessage =
            (typeof error.error === 'string' ? error.error : error.error?.message) ||
            'Greška pri promeni lozinke. Pokušajte ponovo.';
          this.isLoading = false;
        }
      });
    }
  }
}
