import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { CheckoutResponse, PaymentMethod } from '../../models/psp-models';
import { Payment } from '../../services/payment';
import { QRCodeComponent } from 'angularx-qrcode';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ActivatedRoute } from '@angular/router'; // Uvezeno za čitanje URL parametara

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, QRCodeComponent, ZXingScannerModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css',
})
export class Checkout implements OnInit {
  @Input() uuid!: string;

  checkoutData: CheckoutResponse | null = null;
  isLoading = true;
  errorMessage = '';
  qrCodeString: string = '';
  showScanner = false;
  private initiateSubscription: Subscription | null = null;

  constructor(private paymentService: Payment, private router: Router, private route: ActivatedRoute ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['error'] === 'retry_method') {
        this.errorMessage = 'Došlo je do problema sa servisom za plaćanje. Vaša transakcija je i dalje aktivna, molimo pokušajte ponovo ili izaberite drugu metodu.';
      }
    });

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

    if (this.initiateSubscription) {
      this.initiateSubscription.unsubscribe();
    }

    if (method.name === 'CRYPTO') {
      this.router.navigate(['/checkout', this.uuid, 'crypto']);
      return; 
    }
    this.initiateSubscription = this.paymentService.initiatePayment(this.uuid, method.name).subscribe({
      next: (response: any) => {
        this.initiateSubscription = null;

        if (response?.error) {
          this.errorMessage = response.error;
          this.isLoading = false;
          return;
        }

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
      error: (err: { name: string; message: string | string[]; status: number; error: { retryable: any; error: string; message: any; }; }) => {
        this.initiateSubscription = null;
        console.error('Greška pri inicijalizaciji plaćanja:', err);

        if (err?.name === 'TimeoutError' || err?.message?.includes('timeout')) {
          this.errorMessage = 'Zahtev je istekao. Pokušajte ponovo ili izaberite drugu metodu plaćanja.';
        } else if (err?.status === 503 && err?.error?.retryable) {
          const baseMsg = err?.error?.error || 'Metoda plaćanja trenutno nije dostupna.';
          this.errorMessage = baseMsg + ' Možete pokušati ponovo istom metodom ili izabrati drugu metodu plaćanja.';
        } else {
          this.errorMessage = err?.error?.error || err?.error?.message || 'Došlo je do greške pri povezivanju.';
        }
        this.isLoading = false;
      }
    });
  }
  3

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

  selectCryptoPayment() {
  this.router.navigate(['/checkout', this.uuid, 'crypto']);
  }
}

