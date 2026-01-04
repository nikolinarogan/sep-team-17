import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VehicleService } from '../../app/services/vehicle.service';
import { EquipmentService } from '../../app/services/equipment.service';
import { InsuranceService } from '../../app/services/insurance.service';
import { Vehicle } from '../../app/models/vehicle.models';
import { Equipment } from '../../app/models/equipment.models';
import { Insurance } from '../../app/models/insurance.models';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule],
  templateUrl: 'services.html',
  styleUrl: 'services.css'
})
export class ServicesComponent implements OnInit {
  vehicles: Vehicle[] = [];
  equipment: Equipment[] = [];
  insurances: Insurance[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private vehicleService: VehicleService,
    private equipmentService: EquipmentService,
    private insuranceService: InsuranceService,
    private cdr: ChangeDetectorRef
  ) {}

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
}

