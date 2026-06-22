import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { authInterceptor } from './core/auth.interceptor';
import { routes } from './app.routes';
import { AuthService } from './services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // aspetta il primo GET /auth/me (mai un errore: AuthService.load() lo cattura internamente)
    // prima che il router attivi qualunque rotta, cosi' authGuard vede sempre uno stato gia' risolto
    provideAppInitializer(() => firstValueFrom(inject(AuthService).load()))
  ]
};
