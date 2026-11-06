import { Injectable } from '@angular/core';
import { UserLocation } from '../models/announcement-filters.model';

@Injectable({
  providedIn: 'root',
})
export class LocationService {
  /** Asks the browser for the user's position. Resolves to null if it is refused or unavailable. */
  getCurrentLocation(): Promise<UserLocation | null> {
    if (!navigator.geolocation) {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) =>
          resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
        () => resolve(null),
        { timeout: 10000 },
      );
    });
  }
}
