import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Merchant } from '../../services/merchant';
import { PaymentMethod } from '../../services/payment-method';
import { MerchantConfigDTO } from '../../models/psp-models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-merchant-details',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './merchant-details.html',
  styleUrl: './merchant-details.css',
})
export class MerchantDetails implements OnInit{
  @Input() id!: string; 

  merchant: any = null;
  globalMethods: any[] = [];
  
  selections: any = {};

  constructor(
    private merchantService: Merchant,
    private pmService: PaymentMethod,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData() {
  forkJoin({
    methods: this.pmService.getAllMethods(),
    merchant: this.merchantService.getMerchantById(this.id),
    subs: this.merchantService.getMerchantSubscriptions(this.id)
  }).subscribe({
    next: (result) => {
      this.globalMethods = result.methods;
      result.methods.forEach(m => {
        this.selections[m.name] = { enabled: false, credentials: {} };
      });

      this.merchant = result.merchant;

      if (result.subs) {
        result.subs.forEach((s: any) => {
          const name = s.paymentMethod.name;
          if (this.selections[name]) {
            this.selections[name].enabled = true;
            if (s.credentialsJson) {
              this.selections[name].credentials = JSON.parse(s.credentialsJson);
            }
          }
        });
      }
      console.log("Sve je spremno i učitano!");
    },
    error: (err) => {
      console.error("Greška pri učitavanju podataka:", err);
      alert("Došlo je do greške pri komunikaciji sa serverom.");
    }
  });
}

  onSave() {
  const configsToSave = [];
  
  for (const name in this.selections) {
    if (this.selections[name].enabled) {
      configsToSave.push({
        methodName: name,
        credentials: this.selections[name].credentials
      });
    } else {
      
    }
  }

  this.merchantService.saveMerchantServices(this.id, configsToSave).subscribe({
    next: () => {
      alert('Promene su uspešno sačuvane!');
      this.router.navigate(['/admin/dashboard']);
    }
  });
}
  
}
