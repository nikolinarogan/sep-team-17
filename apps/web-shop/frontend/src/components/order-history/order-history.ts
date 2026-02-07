import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../app/services/order.service';
import { Order } from '../../app/models/order.models';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: 'order-history.html',
  styleUrl: 'order-history.css'
})
export class OrderHistoryComponent implements OnInit {
  allOrders: Order[] = [];
  activeOrders: Order[] = [];
  pastOrders: Order[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  activeTab: 'all' | 'active' | 'past' = 'all';

  constructor(private orderService: OrderService) {}

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    this.errorMessage = '';

    this.orderService.getUserOrders().subscribe({
      next: (orders) => {
        this.allOrders = orders;
        this.activeOrders = orders.filter(order => order.active === true);
        this.pastOrders = orders.filter(order => order.active === false || !order.active);
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load orders. Please try again.';
        this.isLoading = false;
      }
    });
  }

  getOrdersToDisplay(): Order[] {
    switch (this.activeTab) {
      case 'active':
        return this.activeOrders;
      case 'past':
        return this.pastOrders;
      default:
        return this.allOrders;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'PENDING':
        return 'status-pending';
      case 'CANCELLED':
        return 'status-cancelled';
      case 'ERROR':
        return 'status-error';
      default:
        return 'status-default';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'Confirmed';
      case 'PENDING':
        return 'Pending';
      case 'CANCELLED':
        return 'Cancelled';
      case 'ERROR':
        return 'Error';
      default:
        return status;
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('sr-RS', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getServiceName(order: Order): string {
    if (order.type === 'VEHICLE' && order.vehicleModel) {
      return order.vehicleModel;
    } else if (order.type === 'EQUIPMENT' && order.equipmentType) {
      return order.equipmentType.replace('_', ' ');
    } else if (order.type === 'INSURANCE' && order.insuranceType) {
      return `${order.insuranceType} Insurance`;
    }
    return order.type;
  }

  getServiceImage(order: Order): string {
    if (order.type === 'VEHICLE' && order.vehicleImageUrl) {
      return order.vehicleImageUrl;
    }
    return '/assets/default-service.png'; // Fallback slika
  }

  continuePayment(pspPaymentId: string) {
  
  window.location.href = `https://localhost:4201/checkout/${pspPaymentId}`;
}
}

