import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Payment } from '../services/payment';
import { interval, Subscription, switchMap, takeWhile } from 'rxjs';
import { QRCodeComponent } from "angularx-qrcode";

@Component({
  selector: 'app-crypto-checkout',
  standalone: true,
  imports: [CommonModule, QRCodeComponent],
  templateUrl: './crypto-checkout.html',
  styleUrls: ['./crypto-checkout.css']
})
export class CryptoCheckout implements OnInit {
  uuid: string = '';
  cryptoData: any = { btcAmount: '', walletAddress: '', qrCodeUrl: '' };
  isLoading: boolean = true;

  private statusSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private paymentService: Payment,
    private router: Router

  ) {}

  ngOnInit() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
        this.paymentService.getCryptoDetails(this.uuid, 'CRYPTO').subscribe(data => {
        this.cryptoData = data;
        console.log("🔥 ŠTA JE STIGLO SA BACKENDA:", data); 
        this.isLoading = false;
        this.startStatusPolling();
    });
}

  /*loadCryptoData() {
    this.isLoading = true;
    this.paymentService.initiateCryptoPayment(this.uuid).subscribe({
      next: (data) => {
        this.cryptoData = data;
        this.isLoading = false;

      },
      error: (err) => {
        alert("Greška pri inicijalizaciji kripto plaćanja");
        this.isLoading = false;
        this.goBack();
      }
    });
  }*/

  copyToClipboard(val: string) {
    navigator.clipboard.writeText(val);
    alert("Kopirano u clipboard!");
  }

  goBack() {
    this.router.navigate(['/checkout', this.uuid]);
  }

  startStatusPolling() {
    console.log("🚀 Pokrećem proveru statusa...");
    
    this.statusSubscription = interval(3000)
      .pipe(
        switchMap(() => this.paymentService.checkCryptoStatus(this.uuid, 'CRYPTO')),
        takeWhile((res) => res.redirectUrl === null, true) 
      )
      .subscribe({
        next: (res) => {
          console.log("Status:", res);
          
          if (res.redirectUrl) {
            console.log("✅ UPLATA LEGLA! Vraćam korisnika na Webshop...");
            this.statusSubscription?.unsubscribe();
            
            window.location.href = res.redirectUrl; 
          }
        },
        error: (err) => console.error("Greška:", err)
      });
}

  ngOnDestroy() {
    this.statusSubscription?.unsubscribe();
  }
}