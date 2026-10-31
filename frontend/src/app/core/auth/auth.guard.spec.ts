import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  const auth = { hasSession: vi.fn(), login: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: auth }] });
  });

  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/collectie?type=CD' } as RouterStateSnapshot),
    );

  it('laat een sessie (online of offline) door', () => {
    auth.hasSession.mockReturnValue(true);
    expect(run()).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('stuurt zonder sessie naar Keycloak met de gevraagde pagina als doel', () => {
    auth.hasSession.mockReturnValue(false);
    expect(run()).toBe(false);
    expect(auth.login).toHaveBeenCalledWith('/collectie?type=CD');
  });
});
