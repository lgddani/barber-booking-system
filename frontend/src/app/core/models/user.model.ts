export type Role = 'CUSTOMER' | 'BARBER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
}
