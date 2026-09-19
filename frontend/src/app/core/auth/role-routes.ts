import { Role } from '../models/user.model';

const HOME_BY_ROLE: Record<Role, string> = {
  CUSTOMER: '/cliente',
  BARBER: '/barbero',
  ADMIN: '/admin'
};

export function homeRouteForRole(role: Role): string {
  return HOME_BY_ROLE[role];
}
