import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/auth/jwt.interceptor';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    // Antes de que la app levante rutas/guards, valida si había una sesión
    // guardada de una visita anterior (ver AuthService.restoreSession).
    provideAppInitializer(() => firstValueFrom(inject(AuthService).restoreSession()))
  ]
};
