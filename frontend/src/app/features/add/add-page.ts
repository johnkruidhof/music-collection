import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';

/** Toevoegen: scannen (F-14), zoeken (F-16) of handmatig (F-17), elk als eigen route. */
@Component({
  selector: 'app-add-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule, MatIconModule],
  templateUrl: './add-page.html',
  styleUrl: './add-page.scss',
})
export class AddPage {
  protected readonly tabs = [
    { path: 'scannen', label: 'Scannen', icon: 'qr_code_scanner' },
    { path: 'zoeken', label: 'Zoeken', icon: 'search' },
    { path: 'luisteren', label: 'Luisteren', icon: 'hearing' },
    { path: 'handmatig', label: 'Handmatig', icon: 'edit' },
  ];
}
