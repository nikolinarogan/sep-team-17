import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { EquipmentService } from '../../app/services/equipment.service';
import { Equipment } from '../../app/models/equipment.models';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'equipment.html',
  styleUrl: 'equipment.css'
})
export class EquipmentComponent implements OnInit {
  equipment: Equipment[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private equipmentService: EquipmentService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadEquipment();
  }

  loadEquipment() {
    this.isLoading = true;
    this.errorMessage = '';
    this.equipment = [];
    
    this.equipmentService.getAllEquipment().subscribe({
      next: (equipment) => {
        if (Array.isArray(equipment)) {
          this.equipment = equipment;
        } else if (equipment) {
          this.equipment = [equipment];
        } else {
          this.equipment = [];
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading equipment:', error);
        if (error.status === 401) {
          this.errorMessage = 'Unauthorized. Please log in again.';
        } else if (error.status === 403) {
          this.errorMessage = 'Access denied. Admin role required.';
        } else {
          this.errorMessage = error.error?.message || error.error || error.message || 'Failed to load equipment. Please try again.';
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  editEquipment(id: number) {
    this.router.navigate(['/equipment/edit', id]);
  }

  deleteEquipment(id: number) {
    if (confirm('Are you sure you want to delete this equipment?')) {
      this.equipmentService.deleteEquipment(id).subscribe({
        next: () => {
          this.loadEquipment();
        },
        error: (error) => {
          console.error('Error deleting equipment:', error);
          alert('Failed to delete equipment. Please try again.');
        }
      });
    }
  }

  addNewEquipment() {
    this.router.navigate(['/equipment/new']);
  }

  getAvailability(eq: Equipment): boolean {
    return eq.isAvailable ?? eq.available ?? false;
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
}

