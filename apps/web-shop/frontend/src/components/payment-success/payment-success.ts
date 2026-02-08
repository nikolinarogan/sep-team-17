import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { OrderService } from '../../app/services/order.service';
import { Order } from '../../app/models/order.models';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'payment-success.html',
  styleUrl: 'payment-success.css'
})
export class PaymentSuccessComponent implements OnInit {
  orderId: string | null = null;
  order: Order | null = null;
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  ngOnInit() {
    // Uzmi orderId iz query parametra
    console.log("!!! USLA SAM U SUCCESS KOMPONENTU !!!");
    this.route.queryParams.subscribe(params => {
      this.orderId = params['orderId'];
      
      if (this.orderId) {
        // Proveri status narudžbine u bazi
        this.verifyOrderStatus();
      } else {
        this.errorMessage = 'Order ID is missing';
        this.isLoading = false;
      }
    });
  }

  verifyOrderStatus() {
    // Pretpostavljamo da imaš endpoint za dobijanje narudžbine po merchantOrderId
    // Za sada samo prikazujemo success poruku
    // Možeš dodati poziv API-ja ako imaš endpoint
    // this.isLoading = false;
    if (!this.orderId) {
    this.isLoading = false;
    return;
  }

  this.orderService.getOrderStatus(this.orderId).subscribe({
    next: (data) => {
      if (data.orderStatus !== 'CONFIRMED') {
        this.errorMessage = `Status: ${data.orderStatus}. Proverite istoriju narudžbina.`;
      }
      this.isLoading = false;
    },
    error: () => {
      this.errorMessage = 'Greška pri proveri statusa.';
      this.isLoading = false;
    }
  });
  }

  goToServices() {
    this.router.navigate(['/services']);
  }

  goToHome() {
    this.router.navigate(['/home']);
  }
}

