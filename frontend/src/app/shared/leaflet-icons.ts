import * as L from 'leaflet';
import { AnnouncementType } from '../models/announcement.model';

const MARKER_IMAGES_PATH = 'leaflet/';

/**
 * Leaflet guesses the marker image URLs from its stylesheet, which breaks once the build
 * renames the files. Pointing it at the copied images keeps the default pin visible.
 */
export function fixDefaultMarkerIcons(): void {
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: `${MARKER_IMAGES_PATH}marker-icon-2x.png`,
    iconUrl: `${MARKER_IMAGES_PATH}marker-icon.png`,
    shadowUrl: `${MARKER_IMAGES_PATH}marker-shadow.png`,
  });
}

/** A pin colored with --color-lost (red) or --color-found (green), see map.css. */
export function createPinIcon(type: AnnouncementType): L.DivIcon {
  return L.divIcon({
    className: 'map-pin',
    html: `<span class="map-pin-shape map-pin-${type.toLowerCase()}"></span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 34],
    popupAnchor: [0, -34],
  });
}
