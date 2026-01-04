import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Merchant } from '../../services/merchant';
import { MerchantCreateRequest, MerchantCredentials } from '../../models/psp-models';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard implements OnInit {
  merchants: any[] = [];
  
  newMerchant: MerchantCreateRequest = {
    name: '',
    webShopUrl: ''
  };

  createdCredentials: MerchantCredentials | null = null;
  
  isLoading = false;

  constructor(private merchantService: Merchant, private router: Router) {}

  ngOnInit(): void {
    this.loadMerchants();
  }

  loadMerchants() {
    this.merchantService.getAllMerchants().subscribe({
      next: (data) => {
        this.merchants = data;
      },
      error: (err) => console.error('Greška pri učitavanju prodavaca:', err)
    });
  }

  createMerchant() {
    this.isLoading = true;
    this.createdCredentials = null; 

    this.merchantService.createMerchant(this.newMerchant).subscribe({
      next: (response) => {
        this.createdCredentials = response;
                
        const noviProdavac = {
            merchantId: response.merchantId,
            name: this.newMerchant.name,
            webShopUrl: this.newMerchant.webShopUrl
        };

        this.merchants.push(noviProdavac);

        this.newMerchant = { name: '', webShopUrl: '' }; 
        this.isLoading = false;
      },
      error: (err) => {
        alert('Došlo je do greške pri kreiranju prodavca.');
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  logout() {
    this.router.navigate(['/admin/login']);
  }

}
