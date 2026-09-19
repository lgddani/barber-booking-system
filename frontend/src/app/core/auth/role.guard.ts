import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Role } from '../models/user.model';

export function roleGuard(allowed: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.parseUrl('/login');
    }
    const role = auth.role();
    return role && allowed.includes(role) ? true : router.parseUrl('/');
  };
}
