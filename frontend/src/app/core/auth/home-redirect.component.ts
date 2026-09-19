import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { homeRouteForRole } from './role-routes';

// Ruta "/" para un usuario ya autenticado: lo manda a su panel según su rol.
// authGuard ya garantiza que solo llega hasta acá si hay sesión activa.
@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: ''
})
export class HomeRedirectComponent {
  constructor() {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.role();
    if (role) {
      router.navigateByUrl(homeRouteForRole(role));
    }
  }
}
