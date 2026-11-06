import { ComponentFixture, TestBed } from '@angular/core/testing';
import * as L from 'leaflet';
import { Coordinates, MapPin } from '../../models/map.model';
import { MapView } from './map';

describe('MapView', () => {
  const clujNapoca: Coordinates = { latitude: 46.7712, longitude: 23.6236 };
  const lostPin: MapPin = { ...clujNapoca, type: 'LOST', label: 'Lost cat' };
  const foundPin: MapPin = { latitude: 45.7489, longitude: 21.2087, type: 'FOUND' };

  let fixture: ComponentFixture<MapView>;
  let element: HTMLElement;

  // jsdom has no layout, so Leaflet would see a map of size 0 and could not zoom or center
  beforeAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'clientWidth', { configurable: true, value: 800 });
    Object.defineProperty(HTMLElement.prototype, 'clientHeight', {
      configurable: true,
      value: 400,
    });
  });

  afterAll(() => {
    Reflect.deleteProperty(HTMLElement.prototype, 'clientWidth');
    Reflect.deleteProperty(HTMLElement.prototype, 'clientHeight');
  });

  async function render(inputs: Record<string, unknown> = {}): Promise<void> {
    fixture = TestBed.createComponent(MapView);
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  function leafletMap(): L.Map {
    return fixture.componentInstance['map'] as L.Map;
  }

  it('should create a Leaflet map with OpenStreetMap tiles and attribution', async () => {
    await render();

    expect(element.querySelector('.leaflet-container')).toBeTruthy();
    expect(element.querySelector('.leaflet-control-attribution')?.textContent).toContain(
      'OpenStreetMap',
    );
  });

  it('should show all of Romania when no center is given', async () => {
    await render();
    const bounds = leafletMap().getBounds();

    expect(bounds.contains([45.9432, 24.9668])).toBe(true);
    expect(bounds.contains([43.7, 20.3])).toBe(true);
    expect(bounds.contains([48.2, 29.6])).toBe(true);
  });

  it('should center on the given location', async () => {
    await render({ center: clujNapoca, zoom: 14 });
    const center = leafletMap().getCenter();

    expect(center.lat).toBeCloseTo(clujNapoca.latitude, 4);
    expect(center.lng).toBeCloseTo(clujNapoca.longitude, 4);
    expect(leafletMap().getZoom()).toBe(14);
  });

  it('should move to a new center when the input changes', async () => {
    await render({ center: clujNapoca });
    fixture.componentRef.setInput('center', { latitude: 44.4268, longitude: 26.1025 });
    await fixture.whenStable();

    expect(leafletMap().getCenter().lat).toBeCloseTo(44.4268, 4);
  });

  it('should show one pin per location', async () => {
    await render({ pins: [lostPin, foundPin] });

    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(2);
  });

  it('should show no pins by default', async () => {
    await render();

    expect(element.querySelector('.leaflet-marker-icon')).toBeNull();
  });

  it('should color the pin by announcement type', async () => {
    await render({ pins: [lostPin, foundPin] });

    expect(element.querySelectorAll('.map-pin-lost').length).toBe(1);
    expect(element.querySelectorAll('.map-pin-found').length).toBe(1);
  });

  it('should label the pin for assistive technology', async () => {
    await render({ pins: [lostPin] });

    expect(element.querySelector('.leaflet-marker-icon')?.getAttribute('title')).toBe('Lost cat');
  });

  it('should replace the pins when the input changes', async () => {
    await render({ pins: [lostPin, foundPin] });
    fixture.componentRef.setInput('pins', [foundPin]);
    await fixture.whenStable();

    expect(element.querySelectorAll('.leaflet-marker-icon').length).toBe(1);
    expect(element.querySelector('.map-pin-lost')).toBeNull();
  });

  it('should report where the user clicked', async () => {
    await render();
    const clicks: Coordinates[] = [];
    fixture.componentInstance.locationSelected.subscribe((location) => clicks.push(location));

    leafletMap().fire('click', { latlng: L.latLng(46.7712345678, 23.6236345678) });

    expect(clicks).toEqual([{ latitude: 46.771235, longitude: 23.623635 }]);
  });

  it('should report longitudes inside -180..180 when clicking on a repeated world copy', async () => {
    await render();
    const clicks: Coordinates[] = [];
    fixture.componentInstance.locationSelected.subscribe((location) => clicks.push(location));

    leafletMap().fire('click', { latlng: L.latLng(46, 383.6236) });

    expect(clicks[0].longitude).toBeCloseTo(23.6236, 4);
  });

  it('should point the default Leaflet marker at the copied images', async () => {
    await render();
    const options = L.Icon.Default.prototype.options;

    expect(options.iconUrl).toBe('leaflet/marker-icon.png');
    expect(options.iconRetinaUrl).toBe('leaflet/marker-icon-2x.png');
    expect(options.shadowUrl).toBe('leaflet/marker-shadow.png');
  });

  it('should remove the map when destroyed', async () => {
    await render();
    const map = leafletMap();
    const removeSpy = vi.spyOn(map, 'remove');

    fixture.destroy();

    expect(removeSpy).toHaveBeenCalled();
  });
});
