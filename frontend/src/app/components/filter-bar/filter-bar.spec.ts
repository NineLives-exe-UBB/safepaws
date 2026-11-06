import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  AnnouncementFilters,
  DEFAULT_FILTERS,
  LocationStatus,
} from '../../models/announcement-filters.model';
import { FilterBar } from './filter-bar';

describe('FilterBar', () => {
  let fixture: ComponentFixture<FilterBar>;
  let element: HTMLElement;
  let emitted: AnnouncementFilters[];

  async function render(
    inputs: { filters?: AnnouncementFilters; locationStatus?: LocationStatus } = {},
  ): Promise<void> {
    fixture = TestBed.createComponent(FilterBar);
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
    emitted = [];
    fixture.componentInstance.filtersChange.subscribe((filters) => emitted.push(filters));
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  const select = (id: string) => element.querySelector<HTMLSelectElement>(`#filter-${id}`)!;
  const input = (id: string) => element.querySelector<HTMLInputElement>(`#filter-${id}`)!;
  const optionTexts = (id: string) =>
    Array.from(select(id).options).map((option) => option.textContent?.trim());

  function choose(id: string, value: string): void {
    select(id).value = value;
    select(id).dispatchEvent(new Event('change'));
  }

  function type(id: string, value: string): void {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
  }

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should show readable labels in the dropdowns', async () => {
    await render();

    expect(optionTexts('type')).toEqual(['Any', 'Lost', 'Found']);
    expect(optionTexts('species')).toEqual(['Any', 'Cat', 'Dog', 'Other']);
    expect(optionTexts('status')).toEqual(['Any', 'Active', 'Resolved']);
    expect(optionTexts('sort')).toEqual(['Newest first', 'Oldest first', 'Nearest first']);
    expect(optionTexts('radius')).toEqual([
      'Any distance',
      'Within 1 km',
      'Within 5 km',
      'Within 10 km',
      'Within 25 km',
      'Within 50 km',
    ]);
  });

  it('should start with the default values', async () => {
    await render();

    expect(select('type').value).toBe('');
    expect(select('species').value).toBe('');
    expect(select('status').value).toBe('');
    expect(input('color').value).toBe('');
    expect(input('breed').value).toBe('');
    expect(input('area').value).toBe('');
    expect(select('sort').value).toBe('NEWEST');
  });

  it('should show the filters it is given', async () => {
    await render({
      filters: { ...DEFAULT_FILTERS, species: 'DOG', color: 'grey', sort: 'OLDEST' },
    });

    expect(select('species').value).toBe('DOG');
    expect(input('color').value).toBe('grey');
    expect(select('sort').value).toBe('OLDEST');
  });

  it('should update when the filters it is given change', async () => {
    await render();
    fixture.componentRef.setInput('filters', { ...DEFAULT_FILTERS, type: 'FOUND' });
    await fixture.whenStable();

    expect(select('type').value).toBe('FOUND');
    expect(emitted).toEqual([]);
  });

  it('should report a dropdown change right away', async () => {
    await render();
    choose('species', 'CAT');

    expect(emitted).toEqual([{ ...DEFAULT_FILTERS, species: 'CAT' }]);
  });

  it('should wait 300 ms after the user stops typing', async () => {
    await render();
    vi.useFakeTimers();
    type('color', 'g');
    vi.advanceTimersByTime(200);
    type('color', 'gr');
    vi.advanceTimersByTime(200);

    expect(emitted).toEqual([]);

    vi.advanceTimersByTime(100);

    expect(emitted).toEqual([{ ...DEFAULT_FILTERS, color: 'gr' }]);
  });

  it('should report typed text without the surrounding spaces', async () => {
    await render();
    vi.useFakeTimers();
    type('area', '  Cluj ');
    vi.advanceTimersByTime(300);

    expect(emitted[0].area).toBe('Cluj');
  });

  it('should report the radius and the sort order', async () => {
    await render({ locationStatus: 'granted' });
    fixture.componentInstance['form'].controls.radiusKm.setValue(10);
    choose('sort', 'NEAREST');

    expect(emitted[emitted.length - 1]).toEqual({
      ...DEFAULT_FILTERS,
      radiusKm: 10,
      sort: 'NEAREST',
    });
  });

  it('should hide the clear button when no filter is active', async () => {
    await render();

    expect(element.querySelector('.clear-button')).toBeNull();
  });

  it('should not count the sort order as a filter', async () => {
    await render({ filters: { ...DEFAULT_FILTERS, sort: 'OLDEST' } });

    expect(element.querySelector('.clear-button')).toBeNull();
  });

  it('should show the clear button when a filter is active', async () => {
    await render({ filters: { ...DEFAULT_FILTERS, species: 'CAT' } });

    expect(element.querySelector('.clear-button')?.textContent).toContain('Clear filters');
  });

  it('should clear the filters but keep the sort order', async () => {
    await render({
      filters: { ...DEFAULT_FILTERS, species: 'CAT', color: 'grey', sort: 'OLDEST' },
    });
    fixture.nativeElement.querySelector('.clear-button').click();
    await fixture.whenStable();

    expect(emitted).toEqual([{ ...DEFAULT_FILTERS, sort: 'OLDEST' }]);
    expect(select('species').value).toBe('');
    expect(input('color').value).toBe('');
    expect(element.querySelector('.clear-button')).toBeNull();
  });

  it('should enable the radius and the distance sorting when the location is known', async () => {
    await render({ locationStatus: 'granted' });
    const nearest = Array.from(select('sort').options).find((o) => o.value === 'NEAREST');

    expect(select('radius').disabled).toBe(false);
    expect(nearest?.disabled).toBe(false);
    expect(element.querySelector('.location-message')).toBeNull();
  });

  it('should disable the radius and the distance sorting when the location is refused', async () => {
    await render({ locationStatus: 'denied' });
    const nearest = Array.from(select('sort').options).find((o) => o.value === 'NEAREST');

    expect(select('radius').disabled).toBe(true);
    expect(nearest?.disabled).toBe(true);
    expect(element.querySelector('.location-message')?.textContent).toContain(
      'Allow location access to see announcements near you',
    );
  });

  it('should disable them without a message while the location is still being asked', async () => {
    await render({ locationStatus: 'pending' });

    expect(select('radius').disabled).toBe(true);
    expect(element.querySelector('.location-message')).toBeNull();
  });

  it('should enable the radius once the location is allowed', async () => {
    await render({ locationStatus: 'pending' });
    fixture.componentRef.setInput('locationStatus', 'granted');
    await fixture.whenStable();

    expect(select('radius').disabled).toBe(false);
  });
});
