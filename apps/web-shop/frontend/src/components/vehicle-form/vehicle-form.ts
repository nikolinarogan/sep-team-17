import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { VehicleService } from '../../app/services/vehicle.service';
import { Vehicle } from '../../app/models/vehicle.models';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: 'vehicle-form.html',
  styleUrl: 'vehicle-form.css'
})
export class VehicleFormComponent implements OnInit {
  vehicleForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  isEditMode: boolean = false;
  vehicleId: number | null = null;
  selectedFile: File | null = null;
  imagePreview: string | null = null;
  existingImageUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private vehicleService: VehicleService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.vehicleForm = this.fb.group({
      model: ['', [Validators.required]],
      pricePerDay: ['', [Validators.required, Validators.min(0)]],
      isAvailable: [true, [Validators.required]]
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.vehicleId = +id;
      this.loadVehicle(+id);
    }
  }

  loadVehicle(id: number) {
    this.isLoading = true;
    this.vehicleService.getVehicleById(id).subscribe({
      next: (vehicle) => {
        this.vehicleForm.patchValue({
          model: vehicle.model,
          pricePerDay: vehicle.pricePerDay,
          isAvailable: vehicle.isAvailable ?? vehicle.available ?? true
        });
        if (vehicle.imageUrl) {
          this.existingImageUrl = vehicle.imageUrl;
          this.imagePreview = vehicle.imageUrl;
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading vehicle:', error);
        this.errorMessage = 'Failed to load vehicle. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit() {
    if (this.vehicleForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const formData = new FormData();
      formData.append('model', this.vehicleForm.get('model')?.value);
      // Backend expects isAvailable as Boolean
      const isAvailable = this.vehicleForm.get('isAvailable')?.value === true || this.vehicleForm.get('isAvailable')?.value === 'true';
      formData.append('isAvailable', isAvailable.toString());
      formData.append('pricePerDay', this.vehicleForm.get('pricePerDay')?.value.toString());

      if (this.isEditMode) {
        // For update, we need to send vehicle object and optional image
        // Backend setter is setAvailable(), so Jackson expects 'available' in JSON
        const isAvailableValue = this.vehicleForm.get('isAvailable')?.value;
        const available = isAvailableValue === true || isAvailableValue === 'true';
        
        const vehicleData = {
          model: this.vehicleForm.get('model')?.value,
          available: available,  // Use 'available' to match setAvailable() setter
          pricePerDay: this.vehicleForm.get('pricePerDay')?.value,
          imageUrl: this.existingImageUrl
        };
        
        console.log('Sending vehicle update data:', vehicleData);
        console.log('available value:', available, 'type:', typeof available);
        
        const updateFormData = new FormData();
        updateFormData.append('vehicle', new Blob([JSON.stringify(vehicleData)], { type: 'application/json' }));
        
        if (this.selectedFile) {
          updateFormData.append('imageFile', this.selectedFile);
        }

        this.vehicleService.updateVehicle(this.vehicleId!, updateFormData).subscribe({
          next: () => {
            this.successMessage = 'Vehicle updated successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/vehicles']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error updating vehicle:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to update vehicle. Please try again.';
            this.isLoading = false;
          }
        });
      } else {
        // For create, image is required
        if (!this.selectedFile) {
          this.errorMessage = 'Please select an image for the vehicle.';
          this.isLoading = false;
          return;
        }

        formData.append('imageFile', this.selectedFile);

        this.vehicleService.createVehicle(formData).subscribe({
          next: () => {
            this.successMessage = 'Vehicle created successfully!';
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/vehicles']);
            }, 1500);
          },
          error: (error) => {
            console.error('Error creating vehicle:', error);
            this.errorMessage = error.error?.message || error.error || 'Failed to create vehicle. Please try again.';
            this.isLoading = false;
          }
        });
      }
    }
  }

  cancel() {
    this.router.navigate(['/vehicles']);
  }
}

