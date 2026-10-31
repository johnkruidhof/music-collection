import { Component, effect, inject, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { CurrentUserService } from './core/auth/current-user.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly currentUser = inject(CurrentUserService);

  constructor() {
    // Rollen (voor het menu-item Beheer) ophalen zodra er een geldige sessie is.
    effect(() => {
      if (this.auth.isLoggedIn()) {
        untracked(() => this.currentUser.load().subscribe());
      }
    });

    // Nieuwe versie van de app (service worker, F-26): de gebruiker kiest zelf wanneer te verversen.
    const updates = inject(SwUpdate, { optional: true });
    const snackBar = inject(MatSnackBar);
    if (updates?.isEnabled) {
      updates.versionUpdates
        .pipe(filter((event): event is VersionReadyEvent => event.type === 'VERSION_READY'))
        .subscribe(() =>
          snackBar
            .open('Er is een nieuwe versie van de app.', 'Vernieuwen')
            .onAction()
            .subscribe(() => document.location.reload()),
        );
    }
  }
}
