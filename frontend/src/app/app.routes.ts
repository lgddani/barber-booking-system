import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { HomeRedirectComponent } from './core/auth/home-redirect.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'cliente',
    canActivate: [roleGuard(['CUSTOMER'])],
    loadComponent: () =>
      import('./features/customer/customer-home.component').then((m) => m.CustomerHomeComponent)
  },
  {
    path: 'barbero',
    canActivate: [roleGuard(['BARBER'])],
    loadComponent: () => import('./features/barber/barber-home.component').then((m) => m.BarberHomeComponent)
  },
  {
    path: 'admin',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/admin-home.component').then((m) => m.AdminHomeComponent)
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    component: HomeRedirectComponent
  },
  { path: '**', redirectTo: 'login' }
];
