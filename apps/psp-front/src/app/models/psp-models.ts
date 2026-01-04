export interface PaymentMethod {
  name: string;        
  serviceUrl: string;  
}

export interface CheckoutResponse {
  amount: number;      
  currency: string;
  merchantId: string;
  availableMethods: PaymentMethod[];
}

export interface MerchantCreateRequest {
  name: string;
  webShopUrl: string;
}

export interface MerchantCredentials {
  merchantId: string;
  merchantPassword: string;
}

export interface MerchantConfigRequest {
  methodName: string;
  credentials: { [key: string]: string }; 
}

export interface LoginRequestDTO {
  username: string;
  password: string;
}