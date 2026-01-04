import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { InsuranceService } from '../../app/services/insurance.service';
import { Insurance } from '../../app/models/insurance.models';

@Component({
  selector: 'app-insurance',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'insurance.html',
  styleUrl: 'insurance.css'
})
export class InsuranceComponent implements OnInit {
  insurances: Insurance[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private insuranceService: InsuranceService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadInsurances();
  }

  loadInsurances() {
    this.isLoading = true;
    this.errorMessage = '';
    this.insurances = [];
    
    this.insuranceService.getAllInsurances().subscribe({
      next: (insurances) => {
        if (Array.isArray(insurances)) {
          this.insurances = insurances;
        } else if (insurances) {
          this.insurances = [insurances];
        } else {
          this.insurances = [];
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading insurances:', error);
        if (error.status === 401) {
          this.errorMessage = 'Unauthorized. Please log in again.';
        } else if (error.status === 403) {
          this.errorMessage = 'Access denied. Admin role required.';
        } else {
          this.errorMessage = error.error?.message || error.error || error.message || 'Failed to load insurances. Please try again.';
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  editInsurance(id: number) {
    this.router.navigate(['/insurance/edit', id]);
  }

  deleteInsurance(id: number) {
    if (confirm('Are you sure you want to delete this insurance?')) {
      this.insuranceService.deleteInsurance(id).subscribe({
        next: () => {
          this.loadInsurances();
        },
        error: (error) => {
          console.error('Error deleting insurance:', error);
          alert('Failed to delete insurance. Please try again.');
        }
      });
    }
  }

  addNewInsurance() {
    this.router.navigate(['/insurance/new']);
  }

  getAvailability(insurance: Insurance): boolean {
    return insurance.isAvailable ?? insurance.available ?? false;
  }

  getInsuranceTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'BASIC': 'Basic',
      'FULL': 'Full',
      'PREMIUM': 'Premium'
    };
    return labels[type] || type;
  }
}

