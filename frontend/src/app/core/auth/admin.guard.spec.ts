import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { UserApiService } from '../api/user-api.service';
import { User } from '../models/user';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  const user = (roles: string[]): User => ({
    id: '1',
    username: 'x',
    email: null,
    displayName: null,
    roles,
  });

  async function run(me: Observable<User>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: UserApiService, useValue: { me: () => me } }],
    });
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
    return firstValueFrom(result);
  }

  it('laat een beheerder door', async () => {
    expect(await run(of(user(['user', 'admin'])))).toBe(true);
  });

  it('stuurt een gewone gebruiker naar de startpagina', async () => {
    const result = await run(of(user(['user'])));
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/');
  });
});
