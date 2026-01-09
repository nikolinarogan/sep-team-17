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

    if (method.name === 'CARD') { 
        this.isLoading = true;
        
        this.paymentService.initiateCardPayment(this.uuid).subscribe({
  next: (response: any) => {
    // 1. OVO JE KLJUČNO: Pogledaj u konzolu šta je tačno stiglo
    console.log("Šta je stiglo od beka?", response);

    // 2. Provera strukture
    // Ako je Java vratila mapu, URL je verovatno u 'response.paymentUrl'
    // Ali ako je Java vratila čist string, onda je URL sam 'response'
    
    let urlZaBanku = '';

    if (response && response.paymentUrl) {
        // Slučaj A: Bekend vratio JSON { "paymentUrl": "http..." }
        urlZaBanku = response.paymentUrl;
    } else if (typeof response === 'string') {
        // Slučaj B: Bekend vratio običan tekst "http..."
        urlZaBanku = response;
    } else {
        console.error("Nepoznat format odgovora!", response);
        return; // Prekini ako nema URL-a
    }

    console.log("Preusmeravam na:", urlZaBanku);

    // 3. Izvrši preusmeravanje SAMO ako je URL validan string
    if (urlZaBanku && urlZaBanku.startsWith('http')) {
        window.location.href = urlZaBanku;
    } else {
        alert("Greška: Nije stigao validan URL od banke!");
    }
  },
  error: (err) => {
    console.error("Greška:", err);
    this.errorMessage = "Greška pri komunikaciji sa bankom.";
    this.isLoading = false;
  }
});
    } else {
        // Za ostale metode (PayPal, Crypto, QR) koje još nisu gotove
        alert(`Metoda ${method.name} još nije implementirana.`);
    }
  }

}
