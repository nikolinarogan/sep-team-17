export enum InsuranceType {
  BASIC = 'BASIC',
  FULL = 'FULL',
  PREMIUM = 'PREMIUM'
}

export interface Insurance {
  id?: number;
  price: number;
  type: InsuranceType;
  isAvailable?: boolean;  // Insurance model uses isAvailable
  available?: boolean;     // Also support available for DTO compatibility
}

