import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { CheckoutResponse, PaymentMethod } from '../../models/psp-models';
import { Payment } from '../../services/payment';
import { QRCodeComponent } from 'angularx-qrcode';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, QRCodeComponent, ZXingScannerModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css',
})
export class Checkout implements OnInit{
  @Input() uuid!: string; 

  checkoutData: CheckoutResponse | null = null;
  isLoading = true;
  errorMessage = '';
  qrCodeString: string = ''; 
  showScanner = false;

   // NOVO: Čuvamo Subscription da možemo da ga otkažemo
  private initiateSubscription: Subscription | null = null;

  constructor(private paymentService: Payment) {}

  ngOnInit(): void {
    if (this.uuid) {
      this.loadData(this.uuid);
    } else {
      this.errorMessage = 'Nedostaje ID transakcije (UUID).';
      this.isLoading = false;
    }
  }
  cancelInitiate(): void {
    if (this.initiateSubscription) {
      this.initiateSubscription.unsubscribe();
      this.initiateSubscription = null;
    }
    this.isLoading = false;
    this.errorMessage = '';
    // Korisnik ostaje na stranici i može odmah da izabere drugu metodu
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
  this.isLoading = true;
  this.errorMessage = '';
  // NOVO: Otkaži prethodni zahtev ako postoji (npr. dupli klik)
  if (this.initiateSubscription) {
    this.initiateSubscription.unsubscribe();
  }
  // this.paymentService.initiatePayment(this.uuid, method.name).subscribe({
  //   next: (response: any) => {
  //     if (response?.paymentUrl) {
  //       window.location.href = response.paymentUrl;
  //     } else if (response?.qrData) {
  //       this.qrCodeString = response.qrData;
  //       this.isLoading = false;
  //     } else {
  //       this.errorMessage = 'Greška: Nije dobijen validan odgovor od servera.';
  //       this.isLoading = false;
  //     }
  //   },
  //   error: (err) => {
  //     console.error('Greška pri inicijalizaciji plaćanja:', err);
  //     this.errorMessage = err?.error?.error || err?.error?.message || 'Došlo je do greške pri povezivanju.'; 
  //     this.isLoading = false;
  //   }
  // });
  this.initiateSubscription = this.paymentService.initiatePayment(this.uuid, method.name).subscribe({
    next: (response: any) => {
      this.initiateSubscription = null;
      if (response?.paymentUrl) {
        window.location.href = response.paymentUrl;
      } else if (response?.qrData) {
        this.qrCodeString = response.qrData;
        this.isLoading = false;
      } else {
        this.errorMessage = 'Greška: Nije dobijen validan odgovor od servera.';
        this.isLoading = false;
      }
    },
    error: (err) => {
      this.initiateSubscription = null;
      console.error('Greška pri inicijalizaciji plaćanja:', err);
      // this.errorMessage = err?.error?.error || err?.error?.message || 'Došlo je do greške pri povezivanju.';
      // this.isLoading = false;
      // NOVO: Posebna poruka za timeout
   if (err?.name === 'TimeoutError' || err?.message?.includes('timeout')) {
    this.errorMessage = 'Zahtev je istekao. Pokušajte ponovo ili izaberite drugu metodu plaćanja.';
  } else if (err?.status === 503 && err?.error?.retryable) {
    // 503 + retryable = metoda privremeno nedostupna, ali može da proba ponovo
    const baseMsg = err?.error?.error || 'Metoda plaćanja trenutno nije dostupna.';
    this.errorMessage = baseMsg + ' Možete pokušati ponovo istom metodom ili izabrati drugu metodu plaćanja.';
  } else {
    this.errorMessage = err?.error?.error || err?.error?.message || 'Došlo je do greške pri povezivanju.';
  }
  this.isLoading = false;
    }
  });
}
  
  onScanSuccess(scannedText: string) {
    console.log("Kamera je pročitala:", scannedText);
    
    if (scannedText === this.qrCodeString) {

      this.isLoading = true;
      this.showScanner = false; 

      this.paymentService.verifyQrScan(this.uuid, scannedText).subscribe({
        next: (res) => {
          alert("Plaćanje uspešno!");
          window.location.href = 'http://localhost:4200/my-services';
        },
        error: (err) => {
          console.error("Greška pri verifikaciji:", err);
          this.errorMessage = "Verifikacija neuspešna.";
          this.isLoading = false;
        }
      });
    } else {
      alert("Skenirani kod ne odgovara ovoj transakciji!");
    }
  }

  openMbankingSimulator() {
      const bankUrl = 'https://localhost:8082/mbanking.html'; 
      window.open(bankUrl, '_blank');
  }
}
