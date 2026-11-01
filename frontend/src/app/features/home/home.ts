import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { UserApiService } from '../../core/api/user-api.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly auth = inject(AuthService);
  private readonly userApi = inject(UserApiService);

  /** undefined = laden, null = fout */
  protected readonly me = this.auth.isLoggedIn()
    ? toSignal(this.userApi.me().pipe(catchError(() => of(null))))
    : () => undefined;
}
