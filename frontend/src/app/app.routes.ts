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
      import('./features/customer/customer-shell.component').then((m) => m.CustomerShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'reservar' },
      {
        path: 'reservar',
        loadComponent: () => import('./features/customer/booking/booking.component').then((m) => m.BookingComponent)
      },
      {
        path: 'mis-citas',
        loadComponent: () =>
          import('./features/customer/my-appointments/my-appointments.component').then(
            (m) => m.MyAppointmentsComponent
          )
      }
    ]
  },
  {
    path: 'barbero',
    canActivate: [roleGuard(['BARBER'])],
    loadComponent: () => import('./features/barber/barber-shell.component').then((m) => m.BarberShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'agenda' },
      {
        path: 'agenda',
        loadComponent: () =>
          import('./features/barber/agenda/barber-agenda.component').then((m) => m.BarberAgendaComponent)
      },
      {
        path: 'horarios',
        loadComponent: () =>
          import('./features/barber/working-hours/barber-working-hours.component').then(
            (m) => m.BarberWorkingHoursComponent
          )
      },
      {
        path: 'bloqueos',
        loadComponent: () =>
          import('./features/barber/exceptions/barber-exceptions.component').then(
            (m) => m.BarberExceptionsComponent
          )
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'servicios' },
      {
        path: 'servicios',
        loadComponent: () =>
          import('./features/admin/services/admin-services.component').then((m) => m.AdminServicesComponent)
      },
      {
        path: 'barberos',
        loadComponent: () =>
          import('./features/admin/barbers/admin-barbers.component').then((m) => m.AdminBarbersComponent)
      },
      {
        path: 'barberos/:barberId/horarios',
        loadComponent: () =>
          import('./features/barber/working-hours/barber-working-hours.component').then(
            (m) => m.BarberWorkingHoursComponent
          )
      },
      {
        path: 'barberos/:barberId/bloqueos',
        loadComponent: () =>
          import('./features/barber/exceptions/barber-exceptions.component').then(
            (m) => m.BarberExceptionsComponent
          )
      },
      {
        path: 'citas',
        loadComponent: () =>
          import('./features/admin/appointments/admin-appointments.component').then(
            (m) => m.AdminAppointmentsComponent
          )
      },
      {
        path: 'estadisticas',
        loadComponent: () =>
          import('./features/admin/stats/admin-stats.component').then((m) => m.AdminStatsComponent)
      }
    ]
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    component: HomeRedirectComponent
  },
  { path: '**', redirectTo: 'login' }
];
