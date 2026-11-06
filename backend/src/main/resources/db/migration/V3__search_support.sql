-- Lets search ignore diacritics, so "Manastur" also finds "Mănăștur"
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Columns used by the list filters
CREATE INDEX idx_announcement_type ON announcement (type);
CREATE INDEX idx_announcement_species ON announcement (species);
CREATE INDEX idx_announcement_status ON announcement (status);

-- Test announcements from V2 that had no map position, matched by their placeholder phone
UPDATE announcement SET latitude = 46.7690, longitude = 23.5900 WHERE contact_phone = '0700000006'; -- Centru
UPDATE announcement SET latitude = 46.7620, longitude = 23.6080 WHERE contact_phone = '0700000009'; -- Andrei Mureșanu
UPDATE announcement SET latitude = 46.7880, longitude = 23.6510 WHERE contact_phone = '0700000013'; -- Someșeni

-- Every area follows the "Neighborhood, City" format
UPDATE announcement SET area = 'Centru, Florești', latitude = 46.7445, longitude = 23.4850 WHERE contact_phone = '0700000012';
UPDATE announcement SET area = 'Centru, Baciu', latitude = 46.7900, longitude = 23.5275 WHERE contact_phone = '0700000014';
