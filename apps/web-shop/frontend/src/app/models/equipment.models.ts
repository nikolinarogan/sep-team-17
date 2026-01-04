export enum EquipmentType {
  CHILD_SEAT = 'CHILD_SEAT',
  GPS = 'GPS',
  TOLL_CARD = 'TOLL_CARD',
  SNOW_CHAINS = 'SNOW_CHAINS'
}

export interface Equipment {
  id?: number;
  pricePerDay: number;
  equipmentType: EquipmentType;
  available?: boolean;  // Backend returns 'available' not 'isAvailable'
  isAvailable?: boolean; // Also support isAvailable for compatibility
}

