import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { OrderService } from '../../app/services/order.service';
import { Order } from '../../app/models/order.models';

@Component({
  selector: 'app-payment-error',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'payment-error.html',
  styleUrl: 'payment-error.css'
})
export class PaymentErrorComponent implements OnInit {
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
    this.route.queryParams.subscribe(params => {
      this.orderId = params['orderId'];
      
      if (this.orderId) {
        this.verifyOrderStatus();
      } else {
        this.errorMessage = 'Order ID is missing';
        this.isLoading = false;
      }
    });
  }

  verifyOrderStatus() {
    // this.isLoading = false;
    if (!this.orderId) {
    this.isLoading = false;
    return;
  }

  this.orderService.getOrderStatus(this.orderId).subscribe({
    next: (data) => {
      // Ako je status zapravo CONFIRMED, preusmeravamo na success
      if (data.orderStatus === 'CONFIRMED') {
        this.errorMessage = 'Plaćanje je zapravo uspelo! Preusmeravam...';
        setTimeout(() => {
          this.router.navigate(['/payment-success'], { 
            queryParams: { orderId: this.orderId } 
          });
        }, 2000);
      } else if (data.orderStatus !== 'CANCELLED' && data.orderStatus !== 'ERROR') {
        // Ako je PENDING ili nešto drugo
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

  tryAgain() {
    this.router.navigate(['/services']);
  }

  goToHome() {
    this.router.navigate(['/home']);
  }

  contactSupport() {
    // Možeš dodati link ka support stranici ili email
    window.location.href = 'mailto:support@webshop.com';
  }
}

