import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { CheckoutResponse, PaymentMethod } from '../../models/psp-models';
import { Payment } from '../../services/payment';
import { QRCodeComponent } from 'angularx-qrcode';
import { ZXingScannerModule } from '@zxing/ngx-scanner';

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

//   selectMethod(method: PaymentMethod) {
//     console.log("Korisnik bira:", method.name);

//     if (method.name === 'CARD') { 
//         this.isLoading = true;
        
//         this.paymentService.initiateCardPayment(this.uuid).subscribe({
//   next: (response: any) => {
//     // 1. OVO JE KLJUČNO: Pogledaj u konzolu šta je tačno stiglo
//     console.log("Šta je stiglo od beka?", response);

//     // 2. Provera strukture
//     // Ako je Java vratila mapu, URL je verovatno u 'response.paymentUrl'
//     // Ali ako je Java vratila čist string, onda je URL sam 'response'
    
//     let urlZaBanku = '';

//     if (response && response.paymentUrl) {
//         // Slučaj A: Bekend vratio JSON { "paymentUrl": "http..." }
//         urlZaBanku = response.paymentUrl;
//     } else if (typeof response === 'string') {
//         // Slučaj B: Bekend vratio običan tekst "http..."
//         urlZaBanku = response;
//     } else {
//         console.error("Nepoznat format odgovora!", response);
//         return; // Prekini ako nema URL-a
//     }

//     console.log("Preusmeravam na:", urlZaBanku);

//     // 3. Izvrši preusmeravanje SAMO ako je URL validan string
//     if (urlZaBanku && urlZaBanku.startsWith('http')) {
//         window.location.href = urlZaBanku;
//     } else {
//         alert("Greška: Nije stigao validan URL od banke!");
//     }
//   },
//   error: (err) => {
//     console.error("Greška:", err);
//     this.errorMessage = "Greška pri komunikaciji sa bankom.";
//     this.isLoading = false;
//   }
// });
//     } else if (method.name === 'QR') {
//         this.isLoading = true;
//         this.paymentService.initiateQrPayment(this.uuid).subscribe({
//           next: (response: any) => {
//             this.qrCodeString = response.qrData; 
//             this.isLoading = false;
//             // Ostajemo na stranici da bi se prikazao QR kod iz HTML-a
//           },
//           error: (err) => {
//             this.errorMessage = "Greška pri dobavljanju QR koda.";
//             this.isLoading = false;
//           }
//         });
//     }
//     else if (method.name === 'PAYPAL') {
//         this.isLoading = true;
//         console.log("Inicijalizujem PayPal za UUID:", this.uuid);

//         this.paymentService.initiatePaypalPayment(this.uuid).subscribe({
//             next: (response: any) => {
//                 console.log("Odgovor od beka za PayPal:", response);
                
//                 // Očekujemo JSON format { "paymentUrl": "https://www.sandbox.paypal.com/..." }
//                 if (response && response.paymentUrl) {
//                     console.log("Preusmeravam na PayPal:", response.paymentUrl);
//                     window.location.href = response.paymentUrl;
//                 } else {
//                     console.error("Bekend nije vratio paymentUrl!", response);
//                     this.errorMessage = "Greška: PayPal link nije generisan.";
//                     this.isLoading = false;
//                 }
//             },
//             error: (err) => {
//                 console.error("Greška pri inicijalizaciji PayPal-a:", err);
//                 this.errorMessage = "Došlo je do greške pri povezivanju sa PayPal-om.";
//                 this.isLoading = false;
//             }
//         });
//     }
//   }

selectMethod(method: PaymentMethod) {
  this.isLoading = true;
  this.errorMessage = '';

  this.paymentService.initiatePayment(this.uuid, method.name).subscribe({
    next: (response: any) => {
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
      console.error('Greška pri inicijalizaciji plaćanja:', err);
      this.errorMessage = err?.error?.message || 'Došlo je do greške pri povezivanju.';
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
