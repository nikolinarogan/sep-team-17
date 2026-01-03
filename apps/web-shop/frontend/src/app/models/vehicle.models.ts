export interface Vehicle {
  id?: number;
  model: string;
  available?: boolean;  // Backend returns 'available' not 'isAvailable'
  isAvailable?: boolean; // Also support isAvailable for compatibility
  pricePerDay: number;
  imageUrl?: string;
}

