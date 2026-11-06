import {
  afterNextRender,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
  ViewEncapsulation,
} from '@angular/core';
import * as L from 'leaflet';
import { Coordinates, MapPin } from '../../models/map.model';
import { createPinIcon, fixDefaultMarkerIcons } from '../../shared/leaflet-icons';

const TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const TILE_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const ROMANIA_BOUNDS = L.latLngBounds([43.6, 20.2], [48.3, 29.7]);
const COORDINATE_DECIMALS = 6;

@Component({
  selector: 'app-map',
  styleUrl: './map.css',
  templateUrl: './map.html',
  encapsulation: ViewEncapsulation.None,
})
export class MapView {
  /** Centers the map on this location. Without it, the map shows all of Romania. */
  readonly center = input<Coordinates | null>(null);
  readonly zoom = input(15);
  readonly pins = input<MapPin[]>([]);

  /** The place the user clicked, so the parent can decide what to do with it (e.g. move a pin). */
  readonly locationSelected = output<Coordinates>();

  private readonly container = viewChild.required<ElementRef<HTMLElement>>('container');
  private readonly ready = signal(false);
  private map?: L.Map;
  private pinLayer?: L.LayerGroup;

  constructor() {
    afterNextRender(() => {
      this.createMap();
      this.ready.set(true);
    });

    inject(DestroyRef).onDestroy(() => this.map?.remove());

    effect(() => {
      if (!this.ready()) {
        return;
      }
      this.showView(this.center(), this.zoom());
    });

    effect(() => {
      if (!this.ready()) {
        return;
      }
      this.showPins(this.pins());
    });
  }

  private createMap(): void {
    fixDefaultMarkerIcons();

    const map = L.map(this.container().nativeElement);
    L.tileLayer(TILE_URL, { maxZoom: 19, attribution: TILE_ATTRIBUTION }).addTo(map);
    map.on('click', (event: L.LeafletMouseEvent) => this.reportClick(event.latlng));

    this.map = map;
    this.pinLayer = L.layerGroup().addTo(map);
  }

  private showView(center: Coordinates | null, zoom: number): void {
    if (center) {
      this.map?.setView([center.latitude, center.longitude], zoom);
    } else {
      this.map?.fitBounds(ROMANIA_BOUNDS);
    }
  }

  private showPins(pins: MapPin[]): void {
    const layer = this.pinLayer;
    if (!layer) {
      return;
    }
    layer.clearLayers();
    pins.forEach((pin) =>
      L.marker([pin.latitude, pin.longitude], {
        icon: createPinIcon(pin.type),
        title: pin.label,
        alt: pin.label ?? `${pin.type} pin`,
      }).addTo(layer),
    );
  }

  private reportClick(position: L.LatLng): void {
    // wrap() brings the longitude back to -180..180 when the user clicked on a repeated world copy
    const { lat, lng } = position.wrap();
    this.locationSelected.emit({
      latitude: this.round(lat),
      longitude: this.round(lng),
    });
  }

  private round(value: number): number {
    return Number(value.toFixed(COORDINATE_DECIMALS));
  }
}
