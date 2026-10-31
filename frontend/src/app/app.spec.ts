import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { of } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { CurrentUserService } from './core/auth/current-user.service';

class AuthServiceStub {
  isLoggedIn = () => false;
  hasSession = () => false;
  online = () => true;
  displayName = () => null;
  login = () => undefined;
  logout = () => undefined;
}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useClass: AuthServiceStub },
        { provide: CurrentUserService, useValue: { isAdmin: () => false, load: () => of(null) } },
      ],
    }).compileComponents();
  });

  it('toont de applicatienaam en een inlogknop', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.brand')?.textContent).toContain('Muziekcollectie');
    expect(el.textContent).toContain('Inloggen');
  });
});
