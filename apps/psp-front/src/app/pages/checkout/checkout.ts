import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { CheckoutResponse, PaymentMethod } from '../../models/psp-models';
import { Payment } from '../../services/payment';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css',
})
export class Checkout implements OnInit{
  @Input() uuid!: string; 

  checkoutData: CheckoutResponse | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(private paymentService: Payment) {}

  ngOnInit(): void {
    if (this.uuid) {
      this.loadData(this.uuid);
    } else {
      this.errorMessage = 'Nedostaje ID transakcije (UUID).';
      this.isLoading = false;
    }
  }

  loadData(uuid: string) {
    this.paymentService.getCheckoutData(uuid).subscribe({
      next: (data) => {
        this.checkoutData = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Greška:', err);
        this.errorMessage = 'Transakcija nije pronađena ili je istekla.';
        this.isLoading = false;
      }
    });
  }

  selectMethod(method: PaymentMethod) {
    console.log("Korisnik bira:", method.name);
    
    // OVDJE REDIREKTUJEMO NA BANKU/PAYPAL
    // Logika: Servis URL + ID naše transakcije da bi banka znala šta naplaćuje
    // Primjer: http://localhost:8082/api/card/payment/123-uuid-456
    
    // PRIVREMENO: Samo alert
    alert(`Redirekcija na ${method.name} servis:\nURL: ${method.serviceUrl}`);
    
    // window.location.href = `${method.serviceUrl}?paymentId=${this.uuid}`;
  }

}
