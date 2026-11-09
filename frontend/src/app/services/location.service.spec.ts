import { TestBed } from '@angular/core/testing';
import { LocationService } from './location.service';

describe('LocationService', () => {
  let service: LocationService;

  function mockGeolocation(getCurrentPosition: Geolocation['getCurrentPosition']): void {
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: { getCurrentPosition },
    });
  }

  beforeEach(() => {
    service = TestBed.inject(LocationService);
  });

  afterEach(() => {
    Reflect.deleteProperty(navigator, 'geolocation');
  });

  it('should return the position when the user allows it', async () => {
    mockGeolocation((success) =>
      success({ coords: { latitude: 46.77, longitude: 23.6 } } as GeolocationPosition),
    );

    expect(await service.getCurrentLocation()).toEqual({ latitude: 46.77, longitude: 23.6 });
  });

  it('should return null when the user refuses', async () => {
    mockGeolocation((_success, error) => error?.({ code: 1 } as GeolocationPositionError));

    expect(await service.getCurrentLocation()).toBeNull();
  });

  it('should return null when the browser has no geolocation', async () => {
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: undefined });

    expect(await service.getCurrentLocation()).toBeNull();
  });
});
