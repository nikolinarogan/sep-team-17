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
  orderStatus: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
  completedAt?: string;
}

