import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { VehicleService } from '../../app/services/vehicle.service';
import { Vehicle } from '../../app/models/vehicle.models';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'vehicles.html',
  styleUrl: 'vehicles.css'
})
export class VehiclesComponent implements OnInit {
  vehicles: Vehicle[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private vehicleService: VehicleService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadVehicles();
  }

  loadVehicles() {
    this.isLoading = true;
    this.errorMessage = '';
    this.vehicles = [];
    
    this.vehicleService.getAllVehicles().subscribe({
      next: (vehicles) => {
        console.log('Vehicles received:', vehicles);
        console.log('Vehicles count:', vehicles?.length);
        console.log('Vehicles type:', typeof vehicles);
        console.log('Is array?', Array.isArray(vehicles));
        
        if (Array.isArray(vehicles)) {
          this.vehicles = vehicles;
        } else if (vehicles) {
          this.vehicles = [vehicles];
        } else {
          this.vehicles = [];
        }
        
        console.log('After processing - vehicles array:', this.vehicles);
        console.log('After processing - vehicles.length:', this.vehicles.length);
        console.log('Setting isLoading to false');
        this.isLoading = false;
        console.log('isLoading is now:', this.isLoading);
        this.cdr.detectChanges(); // Force change detection
      },
      error: (error) => {
        console.error('Error loading vehicles:', error);
        console.error('Error status:', error.status);
        console.error('Error statusText:', error.statusText);
        console.error('Error message:', error.message);
        console.error('Error error:', error.error);
        
        if (error.status === 401) {
          this.errorMessage = 'Unauthorized. Please log in again.';
        } else if (error.status === 403) {
          this.errorMessage = 'Access denied. Admin role required.';
        } else {
          this.errorMessage = error.error?.message || error.error || error.message || 'Failed to load vehicles. Please try again.';
        }
        
        this.isLoading = false;
      }
    });
  }

  editVehicle(id: number) {
    this.router.navigate(['/vehicles/edit', id]);
  }

  deleteVehicle(id: number) {
    if (confirm('Are you sure you want to delete this vehicle?')) {
      this.vehicleService.deleteVehicle(id).subscribe({
        next: () => {
          this.loadVehicles(); // Reload the list
        },
        error: (error) => {
          console.error('Error deleting vehicle:', error);
          alert('Failed to delete vehicle. Please try again.');
        }
      });
    }
  }

  addNewVehicle() {
    this.router.navigate(['/vehicles/new']);
  }

  getAvailability(vehicle: Vehicle): boolean {
    // Handle both isAvailable and available fields, default to false if null/undefined
    return vehicle.isAvailable ?? vehicle.available ?? false;
  }
}

