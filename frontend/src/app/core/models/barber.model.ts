export interface Barber {
  id: string;
  fullName: string;
  phone: string | null;
  bio: string | null;
  active: boolean;
}

export interface BarberCreateRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  bio?: string;
}

export interface BarberUpdateRequest {
  fullName: string;
  phone?: string;
  bio?: string;
}
