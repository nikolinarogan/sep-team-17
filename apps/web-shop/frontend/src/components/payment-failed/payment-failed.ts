import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { OrderService } from '../../app/services/order.service';
import { Order } from '../../app/models/order.models';

@Component({
  selector: 'app-payment-failed',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'payment-failed.html',
  styleUrl: 'payment-failed.css'
})
export class PaymentFailedComponent implements OnInit {
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
    this.isLoading = false;
  }

  tryAgain() {
    this.router.navigate(['/services']);
  }

  goToHome() {
    this.router.navigate(['/home']);
  }
}

