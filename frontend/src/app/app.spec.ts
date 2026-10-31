import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';

class AuthServiceStub {
  isLoggedIn = () => false;
  displayName = () => null;
  login = () => undefined;
  logout = () => undefined;
}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthService, useClass: AuthServiceStub }],
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
