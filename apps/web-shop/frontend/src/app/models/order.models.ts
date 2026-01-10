export interface OrderRequest {
  vehicleId?: number;
  insuranceId?: number;
  equipmentId?: number;
  startDate?: string; // ISO string format
  endDate?: string; // ISO string format
  currency?: string;
}

export interface Order {
  id: number;
  type: 'VEHICLE' | 'INSURANCE' | 'EQUIPMENT';
  vehicleId?: number;
  insuranceId?: number;
  equipmentId?: number;
  startDate?: string;
  endDate?: string;
  pricePerDay?: number;
  price?: number;
  totalAmount: number;
  currency: string;
  orderStatus: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'ERROR';
  createdAt: string;
  completedAt?: string;
  
  // Detalji usluge
  vehicleModel?: string;
  vehicleImageUrl?: string;
  equipmentType?: string;
  insuranceType?: string;
  
  // PaymentTransaction detalji
  merchantOrderId?: string;
  pspPaymentId?: string;
  paymentMethod?: string;
  paymentStatus?: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'ERROR';
  paymentCreatedAt?: string;
  
  // Flag za aktivnu uslugu
  isActive?: boolean;
}

export interface PaymentResponse {
  paymentUrl: string;
  paymentId: string;
}

