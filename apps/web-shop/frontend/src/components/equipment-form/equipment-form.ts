import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { EquipmentService } from '../../app/services/equipment.service';
import { Equipment, EquipmentType } from '../../app/models/equipment.models';

@Component({
  selector: 'app-equipment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'equipment-form.html',
  styleUrl: 'equipment-form.css'
})
export class EquipmentFormComponent implements OnInit {
  equipmentForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  isEditMode: boolean = false;
  equipmentId: number | null = null;
  equipmentTypes = Object.values(EquipmentType);

  constructor(
    private fb: FormBuilder,
    private equipmentService: EquipmentService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.equipmentForm = this.fb.group({
      pricePerDay: ['', [Validators.required, Validators.min(0)]],
      equipmentType: ['', [Validators.required]],
      isAvailable: [true, [Validators.required]]
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.equipmentId = +id;
      this.loadEquipment(+id);
    }
  }

  loadEquipment(id: number) {
    this.isLoading = true;
    this.equipmentService.getEquipmentById(id).subscribe({
      next: (equipment) => {
        this.equipmentForm.patchValue({
          pricePerDay: equipment.pricePerDay,
          equipmentType: equipment.equipmentType,
          isAvailable: equipment.isAvailable ?? equipment.available ?? true
        });
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading equipment:', error);
        this.errorMessage = 'Failed to load equipment. Please try again.';
        this.isLoading = false;
      }
    });
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

  onSubmit() {
    if (this.equipmentForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const isAvailableValue = this.equipmentForm.get('isAvailable')?.value;
      const available = isAvailableValue === true || isAvailableValue === 'true';

      const equipmentData: Equipment = {
        pricePerDay: this.equipmentForm.get('pricePerDay')?.value,
        equipmentType: this.equipmentForm.get('equipmentType')?.value as EquipmentType,
        available: available
      };

      if (this.isEditMode) {
        this.equipmentService.updateEquipment(this.equipmentId!, equipmentData).subscribe({
          next: () => {
            this.successMessage = 'Equipment updated successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/equipment']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error updating equipment:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to update equipment. Please try again.';
            this.isLoading = false;
          }
        });
      } else {
        this.equipmentService.createEquipment(equipmentData).subscribe({
          next: () => {
            this.successMessage = 'Equipment created successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/equipment']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error creating equipment:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to create equipment. Please try again.';
            this.isLoading = false;
          }
        });
      }
    }
  }

  cancel() {
    this.router.navigate(['/equipment']);
  }
}

