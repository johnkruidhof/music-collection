import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';

/** Toevoegen: scannen (F-14), zoeken (F-16) of handmatig (F-17), elk als eigen route. */
@Component({
  selector: 'app-add-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule, MatIconModule],
  template: `
    <h1>Toevoegen</h1>
    <nav mat-tab-nav-bar [tabPanel]="panel" aria-label="Manier van toevoegen">
      @for (tab of tabs; track tab.path) {
        <a
          mat-tab-link
          [routerLink]="tab.path"
          queryParamsHandling="preserve"
          routerLinkActive
          #rla="routerLinkActive"
          [active]="rla.isActive"
        >
          <mat-icon>{{ tab.icon }}</mat-icon>
          <span class="label">{{ tab.label }}</span>
        </a>
      }
    </nav>
    <mat-tab-nav-panel #panel>
      <div class="panel"><router-outlet /></div>
    </mat-tab-nav-panel>
  `,
  styles: `
    .label {
      margin-left: 8px;
    }
    .panel {
      padding-top: 24px;
    }
  `,
})
export class AddPage {
  protected readonly tabs = [
    { path: 'scannen', label: 'Scannen', icon: 'qr_code_scanner' },
    { path: 'zoeken', label: 'Zoeken', icon: 'search' },
    { path: 'luisteren', label: 'Luisteren', icon: 'hearing' },
    { path: 'handmatig', label: 'Handmatig', icon: 'edit' },
  ];
}
