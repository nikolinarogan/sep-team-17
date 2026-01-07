import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { VehicleService } from '../../app/services/vehicle.service';
import { EquipmentService } from '../../app/services/equipment.service';
import { InsuranceService } from '../../app/services/insurance.service';
import { OrderService } from '../../app/services/order.service';
import { Vehicle } from '../../app/models/vehicle.models';
import { Equipment } from '../../app/models/equipment.models';
import { Insurance } from '../../app/models/insurance.models';
import { OrderRequest } from '../../app/models/order.models';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: 'services.html',
  styleUrl: 'services.css'
})
export class ServicesComponent implements OnInit {
  vehicles: Vehicle[] = [];
  equipment: Equipment[] = [];
  insurances: Insurance[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  successMessage: string = '';

  // Modal state
  showDateModal: boolean = false;
  selectedItem: { type: 'vehicle' | 'equipment' | 'insurance', id: number } | null = null;
  dateForm: FormGroup;
  isSubmitting: boolean = false;

  constructor(
    private vehicleService: VehicleService,
    private equipmentService: EquipmentService,
    private insuranceService: InsuranceService,
    private orderService: OrderService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.dateForm = this.fb.group({
      startDate: ['', Validators.required],
      endDate: ['', Validators.required]
    }, { validators: this.dateRangeValidator });
  }

  dateRangeValidator = (form: FormGroup) => {
    const startDate = form.get('startDate')?.value;
    const endDate = form.get('endDate')?.value;
    
    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      
      if (end <= start) {
        form.get('endDate')?.setErrors({ dateRange: true });
        return { dateRange: true };
      }
    }
    
    return null;
  }

  ngOnInit() {
    this.loadAllServices();
  }

  loadAllServices() {
    this.isLoading = true;
    this.errorMessage = '';

    // Load all services in parallel
    const vehicles$ = this.vehicleService.getAvailableVehicles();
    const equipment$ = this.equipmentService.getAvailableEquipment();
    const insurances$ = this.insuranceService.getAvailableInsurances();

    let completed = 0;
    const total = 3;

    const checkComplete = () => {
      completed++;
      if (completed === total) {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    };

    vehicles$.subscribe({
      next: (vehicles) => {
        this.vehicles = Array.isArray(vehicles) ? vehicles : [];
        checkComplete();
      },
      error: (error) => {
        console.error('Error loading vehicles:', error);
        checkComplete();
      }
    });

    equipment$.subscribe({
      next: (equipment) => {
        this.equipment = Array.isArray(equipment) ? equipment : [];
        checkComplete();
      },
      error: (error) => {
        console.error('Error loading equipment:', error);
        checkComplete();
      }
    });

    insurances$.subscribe({
      next: (insurances) => {
        this.insurances = Array.isArray(insurances) ? insurances : [];
        checkComplete();
      },
      error: (error) => {
        console.error('Error loading insurances:', error);
        checkComplete();
      }
    });
  }

  getAvailability(vehicle: Vehicle): boolean {
    return vehicle.isAvailable ?? vehicle.available ?? false;
  }

  getEquipmentAvailability(eq: Equipment): boolean {
    return eq.isAvailable ?? eq.available ?? false;
  }

  getInsuranceAvailability(insurance: Insurance): boolean {
    return insurance.isAvailable ?? insurance.available ?? false;
  }

  getEquipmentTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'CHILD_SEAT': 'Child Seat',
      'GPS': 'GPS',
      'TOLL_CARD': 'Toll Card',
      'SNOW_CHAINS': 'Snow Chains'
    };
    return labels[type] || type;
  }

  getInsuranceTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'BASIC': 'Basic',
      'FULL': 'Full',
      'PREMIUM': 'Premium'
    };
    return labels[type] || type;
  }

  // Buy methods
  buyVehicle(vehicle: Vehicle) {
    this.selectedItem = { type: 'vehicle', id: vehicle.id! };
    this.showDateModal = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.dateForm.reset();
  }

  buyEquipment(item: Equipment) {
    this.selectedItem = { type: 'equipment', id: item.id! };
    this.showDateModal = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.dateForm.reset();
  }

  buyInsurance(insurance: Insurance) {
    this.selectedItem = { type: 'insurance', id: insurance.id! };
    this.createOrderDirectly();
  }

  createOrderDirectly() {
    if (!this.selectedItem) return;

    const orderRequest: OrderRequest = {
      currency: 'EUR'
    };

    if (this.selectedItem.type === 'vehicle') {
      orderRequest.vehicleId = this.selectedItem.id;
      if (this.dateForm.valid) {
        orderRequest.startDate = this.formatDateForBackend(this.dateForm.value.startDate);
        orderRequest.endDate = this.formatDateForBackend(this.dateForm.value.endDate);
      } else {
        this.errorMessage = 'Please select start and end dates';
        return;
      }
    } else if (this.selectedItem.type === 'equipment') {
      orderRequest.equipmentId = this.selectedItem.id;
      if (this.dateForm.valid) {
        orderRequest.startDate = this.formatDateForBackend(this.dateForm.value.startDate);
        orderRequest.endDate = this.formatDateForBackend(this.dateForm.value.endDate);
      } else {
        this.errorMessage = 'Please select start and end dates';
        return;
      }
    } else if (this.selectedItem.type === 'insurance') {
      orderRequest.insuranceId = this.selectedItem.id;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.orderService.createOrder(orderRequest).subscribe({
      next: (order) => {
        this.successMessage = `Order created successfully! Order ID: ${order.id}`;
        this.isSubmitting = false;
        this.closeModal();
        this.cdr.detectChanges();
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = '';
          this.cdr.detectChanges();
        }, 5000);
      },
      error: (error) => {
        console.error('Error creating order:', error);
        this.errorMessage = error.error?.message || error.error || 'Failed to create order. Please try again.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSubmitDateForm() {
    if (this.dateForm.valid) {
      this.createOrderDirectly();
    } else {
      this.errorMessage = 'Please fill in all required fields';
    }
  }

  closeModal() {
    this.showDateModal = false;
    this.selectedItem = null;
    this.dateForm.reset();
    this.errorMessage = '';
  }

  formatDateForBackend(dateString: string): string {
    // Convert date string to ISO format with time
    const date = new Date(dateString);
    // Set time to 10:00:00 if not provided
    if (dateString.length === 10) {
      date.setHours(10, 0, 0, 0);
    }
    return date.toISOString();
  }

  getMinDate(): string {
    // Minimum date is tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  }
}

