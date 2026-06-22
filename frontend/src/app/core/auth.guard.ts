import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

// Il signal user e' gia' risolto a questo punto: provideAppInitializer in app.config.ts
// aspetta il primo GET /auth/me prima che il router attivi qualunque rotta.
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated() || router.createUrlTree(['/login']);
};
