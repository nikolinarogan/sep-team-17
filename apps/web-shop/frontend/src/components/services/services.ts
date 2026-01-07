import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';
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

  // Details view state
  showDetails: boolean = false;
  selectedService: { type: 'vehicle' | 'equipment' | 'insurance', data: Vehicle | Equipment | Insurance } | null = null;

  // Modal state
  showDateModal: boolean = false;
  showOrderSummaryModal: boolean = false;
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
    // Reset view state when component initializes
    this.showDetails = false;
    this.showOrderSummaryModal = false;
    this.selectedService = null;
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

  getInsuranceDescription(type: string): string {
    const descriptions: { [key: string]: string } = {
      'BASIC': 'Basic insurance coverage includes liability protection and basic damage coverage. Suitable for short-term rentals and standard driving conditions.',
      'FULL': 'Full insurance coverage provides comprehensive protection including liability, collision, and comprehensive coverage. Ideal for longer rentals and peace of mind.',
      'PREMIUM': 'Premium insurance offers the highest level of protection with full coverage, roadside assistance, and additional benefits. Perfect for extended rentals and maximum security.'
    };
    return descriptions[type] || 'Comprehensive insurance coverage for your peace of mind during your rental period.';
  }

  getEquipmentDescription(type: string): string {
    const descriptions: { [key: string]: string } = {
      'CHILD_SEAT': 'Safe and comfortable child seat suitable for children of various ages. Meets all safety standards and regulations.',
      'GPS': 'Modern GPS navigation system with real-time traffic updates and turn-by-turn directions. Easy to use and reliable.',
      'TOLL_CARD': 'Electronic toll card for convenient payment on highways and toll roads. Pre-loaded and ready to use.',
      'SNOW_CHAINS': 'High-quality snow chains for winter driving conditions. Essential for safe driving in snowy and icy conditions.'
    };
    return descriptions[type] || 'Essential equipment to enhance your travel experience and ensure safety during your rental period.';
  }

  getVehicleDescription(): string {
    return 'This vehicle is available for rental and is perfect for your travel needs. Whether you need it for a short trip or an extended journey, this reliable vehicle will get you where you need to go comfortably and safely.';
  }

  // Helper methods for type-safe access to selected service
  getSelectedVehicle(): Vehicle | null {
    return this.selectedService?.type === 'vehicle' ? (this.selectedService.data as Vehicle) : null;
  }

  getSelectedEquipment(): Equipment | null {
    return this.selectedService?.type === 'equipment' ? (this.selectedService.data as Equipment) : null;
  }

  getSelectedInsurance(): Insurance | null {
    return this.selectedService?.type === 'insurance' ? (this.selectedService.data as Insurance) : null;
  }

  // View details methods
  viewDetails(vehicle: Vehicle) {
    this.selectedService = { type: 'vehicle', data: vehicle };
    this.showDetails = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  viewEquipmentDetails(item: Equipment) {
    this.selectedService = { type: 'equipment', data: item };
    this.showDetails = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  viewInsuranceDetails(insurance: Insurance) {
    this.selectedService = { type: 'insurance', data: insurance };
    this.showDetails = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  // Back to list
  backToList() {
    this.showDetails = false;
    this.showOrderSummaryModal = false;
    this.selectedService = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  // Checkout methods
  checkout() {
    if (!this.selectedService) return;

    this.selectedItem = { 
      type: this.selectedService.type, 
      id: (this.selectedService.data as any).id! 
    };

    if (this.selectedService.type === 'vehicle' || this.selectedService.type === 'equipment') {
      this.showDateModal = true;
      this.dateForm.reset();
    } else if (this.selectedService.type === 'insurance') {
      // For insurance, go directly to order summary modal (no dates needed)
      this.selectedItem = { 
        type: this.selectedService.type, 
        id: (this.selectedService.data as any).id! 
      };
      this.showOrderSummaryModal = true;
    }
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
      // Close date modal and show order summary modal
      this.showDateModal = false;
      this.showOrderSummaryModal = true;
      this.errorMessage = '';
    } else {
      this.errorMessage = 'Please fill in all required fields';
    }
  }

  // Confirm order from order summary page
  confirmOrder() {
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

    this.orderService.createOrder(orderRequest).pipe(
      tap(order => {
        console.log('Order created successfully:', order);
      }),
      catchError(error => {
        console.error('Error creating order:', error);
        this.errorMessage = error.error?.message || error.error || 'Failed to create order. Please try again.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
        return of(null);
      })
    ).subscribe({
      next: (order) => {
        if (order) {
          console.log('Processing successful order response:', order);
          this.successMessage = `Order created successfully! Order ID: ${order.id}`;
          this.isSubmitting = false;
          
          // Close modal and return to services list
          this.showOrderSummaryModal = false;
          this.showDetails = false; // Return to services list page
          this.selectedService = null;
          this.selectedItem = null;
          this.dateForm.reset();
          this.errorMessage = '';
          
          // Force change detection
          this.cdr.detectChanges();
          
          // Clear success message after 5 seconds
          setTimeout(() => {
            this.successMessage = '';
            this.cdr.detectChanges();
          }, 5000);
        } else {
          console.log('Order response was null');
        }
      },
      error: (error) => {
        console.error('Unexpected error in subscribe:', error);
        this.errorMessage = 'An unexpected error occurred. Please try again.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        console.log('Order creation observable completed');
      }
    });
  }

  closeModal() {
    this.showDateModal = false;
    this.selectedItem = null;
    this.dateForm.reset();
    this.errorMessage = '';
    // Don't reset showDetails here - user should stay on details page after closing modal
  }

  closeOrderSummaryModal() {
    this.showOrderSummaryModal = false;
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

  // Calculate total price for order summary
  calculateTotalPrice(): number {
    if (!this.selectedService) return 0;

    if (this.selectedService.type === 'insurance') {
      const insurance = this.getSelectedInsurance();
      return insurance ? insurance.price : 0;
    }

    // For vehicle and equipment, calculate based on dates
    if (this.dateForm.valid && this.dateForm.value.startDate && this.dateForm.value.endDate) {
      const startDate = new Date(this.dateForm.value.startDate);
      const endDate = new Date(this.dateForm.value.endDate);
      const days = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
      
      if (this.selectedService.type === 'vehicle') {
        const vehicle = this.getSelectedVehicle();
        return vehicle ? vehicle.pricePerDay * days : 0;
      } else if (this.selectedService.type === 'equipment') {
        const equipment = this.getSelectedEquipment();
        return equipment ? equipment.pricePerDay * days : 0;
      }
    }

    return 0;
  }

  getRentalDays(): number {
    if (!this.dateForm.valid || !this.dateForm.value.startDate || !this.dateForm.value.endDate) {
      return 0;
    }
    const startDate = new Date(this.dateForm.value.startDate);
    const endDate = new Date(this.dateForm.value.endDate);
    return Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
  }

  getSelectedServiceName(): string {
    if (!this.selectedService) return '';
    
    if (this.selectedService.type === 'vehicle') {
      return (this.selectedService.data as Vehicle).model;
    } else if (this.selectedService.type === 'equipment') {
      return this.getEquipmentTypeLabel((this.selectedService.data as Equipment).equipmentType);
    } else if (this.selectedService.type === 'insurance') {
      return this.getInsuranceTypeLabel((this.selectedService.data as Insurance).type) + ' Insurance';
    }
    return '';
  }

  getPricePerDay(): number {
    if (!this.selectedService) return 0;
    
    if (this.selectedService.type === 'vehicle') {
      return (this.selectedService.data as Vehicle).pricePerDay;
    } else if (this.selectedService.type === 'equipment') {
      return (this.selectedService.data as Equipment).pricePerDay;
    }
    return 0;
  }
}

