import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  EmbeddedViewRef,
  inject,
  input,
  output,
  signal,
  TemplateRef,
  viewChild,
  ViewContainerRef,
  ViewEncapsulation,
} from '@angular/core';
import * as L from 'leaflet';
import { Coordinates, MapPin, PinPopupContext } from '../../models/map.model';
import { createPinIcon, fixDefaultMarkerIcons } from '../../shared/leaflet-icons';

const TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const TILE_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

const ROMANIA_BOUNDS = L.latLngBounds([43.6, 20.2], [48.3, 29.7]);
const COORDINATE_DECIMALS = 6;
const FIT_PADDING_PX = 40;
const FIT_MAX_ZOOM = 15;

const POPUP_OPTIONS: L.PopupOptions = {
  minWidth: 200,
  maxWidth: 260,
};

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

  /** Without a center, zooms so that all the pins fit on the screen. */
  readonly fitToPins = input(false);

  /** A line drawn through these points, in order. Nothing is drawn for fewer than two points. */
  readonly trail = input<Coordinates[]>([]);

  /** The content of the popup that opens when a pin is clicked. */
  readonly popup = input<TemplateRef<PinPopupContext> | null>(null);

  /** Reports the location clicked by the user. */
  readonly locationSelected = output<Coordinates>();

  private readonly viewContainer = inject(ViewContainerRef);
  private readonly container = viewChild.required<ElementRef<HTMLElement>>('container');

  private readonly ready = signal(false);

  /**
   * The pins the map zooms to. Its value is only new when a location changes, so a pin that does not
   * count (a draft) does not move the map while the user explores it to choose a place.
   */
  private readonly fitTargets = computed(
    () => (this.fitToPins() ? this.pins().filter((pin) => pin.type !== 'DRAFT') : []),
    { equal: sameLocations },
  );

  private map?: L.Map;
  private pinLayer?: L.LayerGroup;
  private trailLayer?: L.LayerGroup;
  private resizeObserver?: ResizeObserver;

  constructor() {
    afterNextRender(() => {
      this.createMap();
      this.ready.set(true);
    });

    inject(DestroyRef).onDestroy(() => {
      this.resizeObserver?.disconnect();
      this.map?.remove();
    });

    effect(() => {
      if (!this.ready()) {
        return;
      }

      this.showView();
    });

    effect(() => {
      if (!this.ready()) {
        return;
      }

      this.showPins(this.pins());
    });

    effect(() => {
      if (!this.ready()) {
        return;
      }

      this.showTrail(this.trail());
    });
  }

  private createMap(): void {
    fixDefaultMarkerIcons();

    const container = this.container().nativeElement;

    const map = L.map(container, {
      zoomControl: true,
      scrollWheelZoom: true,
    });

    L.tileLayer(TILE_URL, {
      maxZoom: 19,
      attribution: TILE_ATTRIBUTION,
    }).addTo(map);

    map.on('click', (event: L.LeafletMouseEvent) => {
      this.reportClick(event.latlng);
    });

    this.map = map;
    this.trailLayer = L.layerGroup().addTo(map);
    this.pinLayer = L.layerGroup().addTo(map);

    this.observeContainerSize(container);
  }

  /**
   * Leaflet does not automatically detect every container size change.
   * ResizeObserver makes the map recalculate its dimensions when its
   * parent changes size.
   */
  private observeContainerSize(container: HTMLElement): void {
    if (typeof ResizeObserver === 'undefined') {
      return;
    }

    this.resizeObserver = new ResizeObserver(() => {
      this.map?.invalidateSize({
        pan: false,
        debounceMoveend: true,
      });
    });

    this.resizeObserver.observe(container);
  }

  private showView(): void {
    const center = this.center();

    if (center) {
      this.map?.setView([center.latitude, center.longitude], this.zoom());
      return;
    }

    const pins = this.fitTargets();

    if (pins.length > 0) {
      const bounds = L.latLngBounds(pins.map((pin) => [pin.latitude, pin.longitude]));

      this.map?.fitBounds(bounds, {
        padding: [FIT_PADDING_PX, FIT_PADDING_PX],
        maxZoom: FIT_MAX_ZOOM,
      });
    } else {
      this.map?.fitBounds(ROMANIA_BOUNDS);
    }
  }

  private showTrail(points: Coordinates[]): void {
    const layer = this.trailLayer;

    if (!layer) {
      return;
    }

    layer.clearLayers();

    if (points.length >= 2) {
      L.polyline(
        points.map((point) => [point.latitude, point.longitude] as L.LatLngTuple),
        { className: 'map-trail', weight: 3, interactive: false },
      ).addTo(layer);
    }
  }

  private showPins(pins: MapPin[]): void {
    const layer = this.pinLayer;

    if (!layer) {
      return;
    }

    layer.clearLayers();

    const popup = this.popup();

    pins.forEach((pin) => {
      const marker = L.marker([pin.latitude, pin.longitude], {
        icon: createPinIcon(pin.type),
        title: pin.label,
        alt: pin.label ?? `${pin.type} pin`,
      }).addTo(layer);

      if (popup) {
        this.bindPopup(marker, pin, popup);
      }
    });
  }

  /** Renders popup content when opened and destroys it when closed. */
  private bindPopup(marker: L.Marker, pin: MapPin, template: TemplateRef<PinPopupContext>): void {
    let view: EmbeddedViewRef<PinPopupContext> | undefined;

    marker.bindPopup(() => {
      view = this.viewContainer.createEmbeddedView(template, { $implicit: pin });

      view.detectChanges();

      const content = document.createElement('div');

      view.rootNodes.forEach((node) => content.appendChild(node));

      return content;
    }, POPUP_OPTIONS);

    marker.on('popupclose', () => {
      view?.destroy();
      view = undefined;
    });
  }

  /** Sends the clicked map coordinates to the parent component. */
  private reportClick(position: L.LatLng): void {
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

function sameLocations(a: MapPin[], b: MapPin[]): boolean {
  return (
    a.length === b.length &&
    a.every((pin, i) => pin.latitude === b[i].latitude && pin.longitude === b[i].longitude)
  );
}
