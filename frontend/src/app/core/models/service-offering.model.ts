export interface ServiceOffering {
  id: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export interface ServiceOfferingRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
}
