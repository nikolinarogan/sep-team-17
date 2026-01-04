import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { InsuranceService } from '../../app/services/insurance.service';
import { Insurance, InsuranceType } from '../../app/models/insurance.models';

@Component({
  selector: 'app-insurance-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'insurance-form.html',
  styleUrl: 'insurance-form.css'
})
export class InsuranceFormComponent implements OnInit {
  insuranceForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  isEditMode: boolean = false;
  insuranceId: number | null = null;
  insuranceTypes = Object.values(InsuranceType);

  constructor(
    private fb: FormBuilder,
    private insuranceService: InsuranceService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.insuranceForm = this.fb.group({
      price: ['', [Validators.required, Validators.min(0)]],
      type: ['', [Validators.required]],
      isAvailable: [true, [Validators.required]]
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.insuranceId = +id;
      this.loadInsurance(+id);
    }
  }

  loadInsurance(id: number) {
    this.isLoading = true;
    this.insuranceService.getInsuranceById(id).subscribe({
      next: (insurance) => {
        this.insuranceForm.patchValue({
          price: insurance.price,
          type: insurance.type,
          isAvailable: insurance.isAvailable ?? insurance.available ?? true
        });
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading insurance:', error);
        this.errorMessage = 'Failed to load insurance. Please try again.';
        this.isLoading = false;
      }
    });
  }

  getInsuranceTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'BASIC': 'Basic',
      'FULL': 'Full',
      'PREMIUM': 'Premium'
    };
    return labels[type] || type;
  }

  onSubmit() {
    if (this.insuranceForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const isAvailableValue = this.insuranceForm.get('isAvailable')?.value;
      const available = isAvailableValue === true || isAvailableValue === 'true';

      if (this.isEditMode) {
        // For update, Insurance model uses isAvailable
        const insuranceData: Insurance = {
          price: this.insuranceForm.get('price')?.value,
          type: this.insuranceForm.get('type')?.value as InsuranceType,
          isAvailable: available
        };

        this.insuranceService.updateInsurance(this.insuranceId!, insuranceData).subscribe({
          next: () => {
            this.successMessage = 'Insurance updated successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/insurance']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error updating insurance:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to update insurance. Please try again.';
            this.isLoading = false;
          }
        });
      } else {
        // For create, InsuranceRequestDTO uses available
        const insuranceData: Insurance = {
          price: this.insuranceForm.get('price')?.value,
          type: this.insuranceForm.get('type')?.value as InsuranceType,
          available: available
        };

        this.insuranceService.createInsurance(insuranceData).subscribe({
          next: () => {
            this.successMessage = 'Insurance created successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/insurance']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error creating insurance:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to create insurance. Please try again.';
            this.isLoading = false;
          }
        });
      }
    }
  }

  cancel() {
    this.router.navigate(['/insurance']);
  }
}

